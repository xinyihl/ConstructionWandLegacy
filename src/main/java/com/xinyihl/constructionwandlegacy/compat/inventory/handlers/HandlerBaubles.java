package com.xinyihl.constructionwandlegacy.compat.inventory.handlers;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.xinyihl.constructionwandlegacy.api.IInventoryHandler;
import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.compat.containers.ContainerManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;

import java.util.function.Consumer;

public final class HandlerBaubles implements IInventoryHandler {

    @Override
    @Optional.Method(modid = "baubles")
    public int countItems(EntityPlayer player, ItemStack requiredStack, ContainerManager containerManager) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack inventoryStack = handler.getStackInSlot(i);
            if (WandUtil.stackEquals(inventoryStack, requiredStack)) {
                total += Math.max(0, inventoryStack.getCount());
            } else {
                total += containerManager.countItems(player, requiredStack, inventoryStack);
            }
        }
        return total;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public int useItems(EntityPlayer player, ItemStack requiredStack, int count, ContainerManager containerManager) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < handler.getSlots() && count > 0; i++) {
            ItemStack inventoryStack = handler.getStackInSlot(i);
            if (WandUtil.stackEquals(inventoryStack, requiredStack)) {
                ItemStack extracted = handler.extractItem(i, count, false);
                count -= extracted.getCount();
                continue;
            }

            int remaining = containerManager.useItems(player, requiredStack, inventoryStack, count);
            if (remaining != count) {
                count = remaining;
                handler.setChanged(i, true);
            }
        }

        return count;
    }

    @Override
    @Optional.Method(modid = "baubles")
    public void addMatchingStacks(EntityPlayer player, Item item, Consumer<ItemStack> consumer) {
        IBaublesItemHandler handler = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack inventoryStack = handler.getStackInSlot(i);
            if (!inventoryStack.isEmpty() && inventoryStack.getItem() == item) {
                consumer.accept(inventoryStack);
            }
        }
    }
}