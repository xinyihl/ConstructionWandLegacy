package com.xinyihl.constructionwandlegacy.material.source;

import com.xinyihl.constructionwandlegacy.basics.option.WandDataCodec;
import com.xinyihl.constructionwandlegacy.material.MaterialCollector;
import com.xinyihl.constructionwandlegacy.material.MaterialKey;
import com.xinyihl.constructionwandlegacy.material.MaterialReceipt;
import com.xinyihl.constructionwandlegacy.material.MaterialSource;
import com.xinyihl.constructionwandlegacy.material.MaterialSourceFactory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BoundContainerSourceFactory implements MaterialSourceFactory {
    private static final String TAG_BOUND_CONT_POS = "bound_container_pos";
    private static final String TAG_BOUND_CONT_DIM = "bound_container_dim";

    public static void storeBinding(ItemStack wand, BlockPos pos, int dimension) {
        WandDataCodec.update(wand, data -> {
            data.setIntArray(TAG_BOUND_CONT_POS, new int[]{pos.getX(), pos.getY(), pos.getZ()});
            data.setInteger(TAG_BOUND_CONT_DIM, dimension);
        });
    }

    public static boolean hasBinding(ItemStack wand) {
        NBTTagCompound data = WandDataCodec.readData(wand);
        return data.hasKey(TAG_BOUND_CONT_POS) && data.hasKey(TAG_BOUND_CONT_DIM);
    }

    @Nullable
    @Override
    public MaterialSource create(EntityPlayer player, ItemStack wand) {
        NBTTagCompound data = WandDataCodec.readData(wand);
        if (!data.hasKey(TAG_BOUND_CONT_POS) || !data.hasKey(TAG_BOUND_CONT_DIM)) {
            return null;
        }
        int[] position = data.getIntArray(TAG_BOUND_CONT_POS);
        if (position.length < 3) {
            return null;
        }
        BlockPos boundPos = new BlockPos(position[0], position[1], position[2]);
        World boundWorld = DimensionManager.getWorld(data.getInteger(TAG_BOUND_CONT_DIM));
        if (boundWorld == null || !boundWorld.isBlockLoaded(boundPos)) {
            return null;
        }
        TileEntity tile = boundWorld.getTileEntity(boundPos);
        if (tile == null || !tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            return null;
        }
        IItemHandler handler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        return handler == null ? null : new BoundContainerSource(player, boundWorld, boundPos, tile, handler);
    }

    private static final class BoundContainerSource implements MaterialSource {
        private static final String ID = "bound_container";

        private final EntityPlayer player;
        private final World world;
        private final BlockPos pos;
        private final TileEntity tile;
        private final IItemHandler handler;
        private final Map<MaterialKey, List<Integer>> slotsByKey = new LinkedHashMap<>();

        private BoundContainerSource(EntityPlayer player, World world, BlockPos pos,
                                     TileEntity tile, IItemHandler handler) {
            this.player = player;
            this.world = world;
            this.pos = pos;
            this.tile = tile;
            this.handler = handler;
        }

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public void enumerate(MaterialCollector collector) {
            slotsByKey.clear();
            if (!isValidEndpoint()) {
                return;
            }
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.isEmpty() || stack.getCount() <= 0) {
                    continue;
                }
                MaterialKey key = MaterialKey.of(stack);
                slotsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(slot);
                collector.accept(key, stack.getCount());
            }
        }

        @Override
        public MaterialReceipt extract(MaterialKey key, int count) {
            List<Integer> slots = slotsByKey.get(key);
            if (count <= 0 || slots == null || !isValidEndpoint()) {
                return MaterialReceipt.empty();
            }
            int remaining = count;
            List<MaterialReceipt> receipts = new ArrayList<>();
            for (Integer slot : slots) {
                ItemStack extracted;
                try {
                    if (!isValidEndpoint() || !key.matches(handler.getStackInSlot(slot))) {
                        continue;
                    }
                    extracted = handler.extractItem(slot, remaining, false);
                } catch (RuntimeException exception) {
                    break;
                }
                if (extracted != null && !extracted.isEmpty()) {
                    int extractedCount = Math.min(remaining, extracted.getCount());
                    remaining -= extractedCount;
                    receipts.add(MaterialReceipt.of(ID, key, extractedCount,
                            (refundKey, refundCount) -> refund(refundKey, refundCount)));
                }
                if (remaining == 0) {
                    break;
                }
            }
            return MaterialReceipt.combine(receipts);
        }

        private int refund(MaterialKey key, int count) {
            if (!isValidEndpoint()) {
                return InventoryRefunds.refund(player, key, count);
            }
            ItemStack remaining = key.createStack(count);
            for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
                if (!isValidEndpoint()) {
                    break;
                }
                try {
                    ItemStack next = handler.insertItem(slot, remaining, false);
                    remaining = next == null ? remaining : next;
                } catch (RuntimeException exception) {
                    break;
                }
            }
            return remaining.isEmpty()
                    ? 0
                    : InventoryRefunds.refund(player, key, remaining.getCount());
        }

        private boolean isValidEndpoint() {
            try {
                if (world == null || world.isRemote || !world.isBlockLoaded(pos)
                        || world.getTileEntity(pos) != tile || tile.isInvalid()
                        || !tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
                    return false;
                }
                return tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null) == handler;
            } catch (RuntimeException exception) {
                return false;
            }
        }
    }
}
