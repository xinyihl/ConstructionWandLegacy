package com.xinyihl.constructionwandlegacy.wand.supplier;

import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.compat.inventory.handlers.HandlerProjectE;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class SupplierProjectE extends SupplierInventory {
    private final HandlerProjectE projectEHandler = new HandlerProjectE();

    public SupplierProjectE(EntityPlayer player, WandOptions options) {
        super(player, options);
    }

    @Override
    protected void addBlockStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
            return;
        }

        ItemStack normalized = stack.copy();
        normalized.setCount(1);

        int count = projectEHandler.countItems(player, normalized, null);
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

    @Override
    public int takeItemStack(ItemStack stack) {
        int count = stack.getCount();
        if (count <= 0) {
            return 0;
        }
        return projectEHandler.useItems(player, stack, count, null);
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
