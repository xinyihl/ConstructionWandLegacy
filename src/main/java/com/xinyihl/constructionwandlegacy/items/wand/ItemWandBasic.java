package com.xinyihl.constructionwandlegacy.items.wand;

import com.xinyihl.constructionwandlegacy.basics.config.ConfigServer;
import net.minecraft.item.ItemStack;

public class ItemWandBasic extends ItemWand {
    private final ItemStack repairItem;

    public ItemWandBasic(ItemStack repairItem) {
        super();
        this.repairItem = repairItem;
        setMaxDamage(131);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return ConfigServer.getWandProperties(this).getDurability();
    }

    @Override
    public int remainingDurability(ItemStack stack) {
        return stack.getMaxDamage() - stack.getItemDamage();
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return ItemStack.areItemsEqual(repair, repairItem);
    }
}
