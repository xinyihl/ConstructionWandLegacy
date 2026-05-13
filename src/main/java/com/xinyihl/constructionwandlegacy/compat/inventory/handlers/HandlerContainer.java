package com.xinyihl.constructionwandlegacy.compat.inventory.handlers;

import com.xinyihl.constructionwandlegacy.api.IInventoryHandler;
import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.compat.containers.ContainerManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class HandlerContainer implements IInventoryHandler {
    public static final String TAG_BOUND_CONT_POS = "bound_container_pos";
    public static final String TAG_BOUND_CONT_DIM = "bound_container_dim";

    @Nullable
    private static IItemHandler getItemHandler(EntityPlayer player) {
        ItemStack wand = WandUtil.holdingWand(player);
        if (wand.isEmpty()) return null;

        com.xinyihl.constructionwandlegacy.basics.option.WandOptions opts =
                new com.xinyihl.constructionwandlegacy.basics.option.WandOptions(wand);

        if (!opts.tag.hasKey(TAG_BOUND_CONT_POS) || !opts.tag.hasKey(TAG_BOUND_CONT_DIM)) {
            return null;
        }

        int[] posArr = opts.tag.getIntArray(TAG_BOUND_CONT_POS);
        int dimId = opts.tag.getInteger(TAG_BOUND_CONT_DIM);
        if (posArr.length < 3) return null;

        BlockPos boundPos = new BlockPos(posArr[0], posArr[1], posArr[2]);
        World world = DimensionManager.getWorld(dimId);
        if (world == null || !world.isBlockLoaded(boundPos)) return null;

        TileEntity tile = world.getTileEntity(boundPos);
        if (tile == null) return null;

        if (!tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            return null;
        }

        return tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
    }

    public static void storeBinding(com.xinyihl.constructionwandlegacy.basics.option.WandOptions opts,
                                    BlockPos pos, int dimId) {
        opts.tag.setIntArray(TAG_BOUND_CONT_POS, new int[]{pos.getX(), pos.getY(), pos.getZ()});
        opts.tag.setInteger(TAG_BOUND_CONT_DIM, dimId);
    }

    public static boolean hasBinding(com.xinyihl.constructionwandlegacy.basics.option.WandOptions opts) {
        return opts.tag.hasKey(TAG_BOUND_CONT_POS) && opts.tag.hasKey(TAG_BOUND_CONT_DIM);
    }

    public static void clearBinding(com.xinyihl.constructionwandlegacy.basics.option.WandOptions opts) {
        opts.tag.removeTag(TAG_BOUND_CONT_POS);
        opts.tag.removeTag(TAG_BOUND_CONT_DIM);
    }

    @Override
    public int countItems(EntityPlayer player, ItemStack requiredStack, ContainerManager containerManager) {
        IItemHandler handler = getItemHandler(player);
        if (handler == null) return 0;

        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (WandUtil.stackEquals(stack, requiredStack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @Override
    public int useItems(EntityPlayer player, ItemStack requiredStack, int count, ContainerManager containerManager) {
        IItemHandler handler = getItemHandler(player);
        if (handler == null) return count;

        int remaining = count;
        // First pass: find matching slots and extract
        for (int i = 0; i < handler.getSlots() && remaining > 0; i++) {
            ItemStack stackInSlot = handler.getStackInSlot(i);
            if (WandUtil.stackEquals(stackInSlot, requiredStack)) {
                ItemStack extracted = handler.extractItem(i, remaining, false);
                remaining -= extracted.getCount();
            }
        }
        return remaining;
    }

    @Override
    public void addMatchingStacks(EntityPlayer player, Item item, Consumer<ItemStack> consumer) {
        IItemHandler handler = getItemHandler(player);
        if (handler == null) return;

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                consumer.accept(stack);
            }
        }
    }

    public boolean tryInsert(EntityPlayer player, ItemStack stack) {
        IItemHandler handler = getItemHandler(player);
        if (handler == null) return false;

        ItemStack remaining = stack.copy();
        for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
            remaining = handler.insertItem(i, remaining, false);
        }
        return remaining.isEmpty();
    }

    public boolean hasBinding(EntityPlayer player) {
        return getItemHandler(player) != null;
    }
}
