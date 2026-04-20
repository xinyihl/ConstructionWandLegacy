package com.xinyihl.constructionwandlegacy.compat.inventory.handlers;

import com.xinyihl.constructionwandlegacy.api.IInventoryHandler;
import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.compat.containers.ContainerManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;

public class HandlerPlayer implements IInventoryHandler {

    @Override
    public int countItems(EntityPlayer player, ItemStack requiredStack, ContainerManager containerManager) {
        int total = 0;

        for (ItemStack inventoryStack : WandUtil.getMainInv(player)) {
            if (WandUtil.stackEquals(inventoryStack, requiredStack)) {
                total += inventoryStack.getCount();
            } else {
                total += containerManager.countItems(player, requiredStack, inventoryStack);
            }
        }

        for (ItemStack inventoryStack : WandUtil.getHotbarWithOffhand(player)) {
            if (WandUtil.stackEquals(inventoryStack, requiredStack)) {
                total += inventoryStack.getCount();
            } else {
                total += containerManager.countItems(player, requiredStack, inventoryStack);
            }
        }

        return total;
    }

    @Override
    public int useItems(EntityPlayer player, ItemStack requiredStack, int count, ContainerManager containerManager) {

        for (ItemStack inventoryStack : WandUtil.getMainInv(player)) {
            if (count == 0) break;
            if (WandUtil.stackEquals(inventoryStack, requiredStack)) {
                int toTake = Math.min(count, inventoryStack.getCount());
                inventoryStack.shrink(toTake);
                count -= toTake;
            } else {
                count = containerManager.useItems(player, requiredStack, inventoryStack, count);
            }
        }

        for (ItemStack inventoryStack : WandUtil.getHotbarWithOffhand(player)) {
            if (count == 0) break;
            if (WandUtil.stackEquals(inventoryStack, requiredStack)) {
                int toTake = Math.min(count, inventoryStack.getCount());
                inventoryStack.shrink(toTake);
                count -= toTake;
            } else {
                count = containerManager.useItems(player, requiredStack, inventoryStack, count);
            }
        }

        player.inventory.markDirty();
        return count;
    }

    @Override
    public void addMatchingStacks(EntityPlayer player, Item item, Consumer<ItemStack> consumer) {
        for (ItemStack inventoryStack : WandUtil.getHotbarWithOffhand(player)) {
            if (!inventoryStack.isEmpty() && inventoryStack.getItem() == item) {
                consumer.accept(inventoryStack);
            }
        }

        for (ItemStack inventoryStack : WandUtil.getMainInv(player)) {
            if (!inventoryStack.isEmpty() && inventoryStack.getItem() == item) {
                consumer.accept(inventoryStack);
            }
        }
    }
}
