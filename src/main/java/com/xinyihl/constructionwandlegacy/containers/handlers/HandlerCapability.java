package com.xinyihl.constructionwandlegacy.containers.handlers;

import com.xinyihl.constructionwandlegacy.api.IContainerHandler;
import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class HandlerCapability implements IContainerHandler {
    @Override
    public boolean matches(EntityPlayer player, ItemStack itemStack, ItemStack inventoryStack) {
        return !inventoryStack.isEmpty() && inventoryStack.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
    }

    @Override
    public int countItems(EntityPlayer player, ItemStack itemStack, ItemStack inventoryStack) {
        IItemHandler itemHandler = inventoryStack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (itemHandler == null) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack containerStack = itemHandler.getStackInSlot(i);
            if (WandUtil.stackEquals(itemStack, containerStack)) {
                total += Math.max(0, containerStack.getCount());
            }
        }
        return total;
    }

    @Override
    public int useItems(EntityPlayer player, ItemStack itemStack, ItemStack inventoryStack, int count) {
        IItemHandler itemHandler = inventoryStack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        if (itemHandler == null) {
            return count;
        }

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack containerStack = itemHandler.getStackInSlot(i);
            if (WandUtil.stackEquals(itemStack, containerStack)) {
                ItemStack extracted = itemHandler.extractItem(i, count, false);
                count -= extracted.getCount();
                if (count <= 0) {
                    return 0;
                }
            }
        }
        return count;
    }
}
