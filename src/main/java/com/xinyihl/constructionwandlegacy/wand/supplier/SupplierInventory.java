package com.xinyihl.constructionwandlegacy.wand.supplier;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.api.IWandSupplier;
import com.xinyihl.constructionwandlegacy.basics.ReplacementRegistry;
import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.basics.pool.IPool;
import com.xinyihl.constructionwandlegacy.basics.pool.OrderedPool;
import com.xinyihl.constructionwandlegacy.compat.inventory.InventoryManager;
import com.xinyihl.constructionwandlegacy.compat.inventory.handlers.HandlerContainer;
import com.xinyihl.constructionwandlegacy.wand.undo.PlaceSnapshot;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

public class SupplierInventory implements IWandSupplier {
    protected final EntityPlayer player;
    protected final WandOptions options;
    protected final HandlerContainer containerHandler;

    protected Map<ItemStack, Integer> itemCounts;
    protected IPool<ItemStack> itemPool;

    public SupplierInventory(EntityPlayer player, WandOptions options) {
        this.player = player;
        this.options = options;
        this.containerHandler = new HandlerContainer();
    }

    @Override
    public void getSupply(@Nullable ItemStack target) {
        itemCounts = new LinkedHashMap<>();
        itemPool = new OrderedPool<>();

        ItemStack offhandStack = player.getHeldItemOffhand();
        if (!offhandStack.isEmpty() && offhandStack.getItem() instanceof ItemBlock) {
            addBlockStack(offhandStack);
            return;
        }

        if (target != null && !target.isEmpty() && target.getItem() instanceof ItemBlock) {
            addBlockStack(target);
            if (options.match.get() != WandOptions.MATCH.EXACT) {
                ItemBlock targetItem = (ItemBlock) target.getItem();
                for (Item item : ReplacementRegistry.getMatchingSet(targetItem)) {
                    if (item instanceof ItemBlock) {
                        InventoryManager inventoryManager = ConstructionWandLegacy.instance.inventoryManager;
                        inventoryManager.addMatchingStacks(player, item, this::addBlockStack);
                    }
                }
            }
        }
    }

    protected void addBlockStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
            return;
        }

        ItemStack normalized = stack.copy();
        normalized.setCount(1);

        InventoryManager inventoryManager = ConstructionWandLegacy.instance.inventoryManager;
        int count = player.isCreative() ? Integer.MAX_VALUE : inventoryManager.countItems(player, normalized);

        // Include bound container items
        if (!player.isCreative()) {
            count += containerHandler.countItems(player, normalized, null);
        }

        if (count > 0) {
            ItemStack key = findTrackedStack(normalized);
            if (key == null) {
                itemCounts.put(normalized, count);
                itemPool.add(normalized);
            } else {
                itemCounts.put(key, itemCounts.get(key) + count);
            }
        }
    }

    @Nullable
    @Override
    public PlaceSnapshot getPlaceSnapshot(World world, BlockPos pos, RayTraceResult rayTraceResult, @Nullable IBlockState supportingBlock) {
        if (!WandUtil.isPositionPlaceable(world, player, pos, options.replace.get())) {
            return null;
        }
        itemPool.reset();

        while (true) {
            ItemStack stack = itemPool.draw();
            if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
                return null;
            }

            Integer count = itemCounts.get(stack);
            if (count == null || count == 0) {
                continue;
            }

            PlaceSnapshot snapshot = PlaceSnapshot.get(world, player, rayTraceResult, pos, stack, supportingBlock, options, getSourceType());
            if (snapshot != null) {
                int remaining = count - 1;
                itemCounts.put(stack, remaining);
                if (remaining <= 0) {
                    itemPool.remove(stack);
                }
                return snapshot;
            }
        }
    }

    @Override
    public int takeItemStack(ItemStack stack) {
        int count = stack.getCount();
        if (count <= 0 || player.isCreative()) {
            return 0;
        }

        // Try container first, then player inventory
        int remaining = containerHandler.useItems(player, stack, count, null);
        if (remaining > 0) {
            InventoryManager inventoryManager = ConstructionWandLegacy.instance.inventoryManager;
            remaining = inventoryManager.useItems(player, stack, remaining);
        }
        return remaining;
    }

    @Nullable
    private ItemStack findTrackedStack(ItemStack query) {
        for (ItemStack stack : itemCounts.keySet()) {
            if (WandUtil.stackEquals(stack, query)) {
                return stack;
            }
        }
        return null;
    }
}
