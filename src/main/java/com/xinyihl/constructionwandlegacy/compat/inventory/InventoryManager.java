package com.xinyihl.constructionwandlegacy.compat.inventory;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.api.IInventoryHandler;
import com.xinyihl.constructionwandlegacy.compat.containers.ContainerManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.function.Consumer;

public class InventoryManager {
    private final ArrayList<IInventoryHandler> handlers;

    public InventoryManager() {
        handlers = new ArrayList<>();
    }

    public boolean register(IInventoryHandler handler) {
        return handlers.add(handler);
    }

    public int countItems(EntityPlayer player, ItemStack requiredStack) {
        ContainerManager containerManager = ConstructionWandLegacy.instance.containerManager;
        int total = 0;
        for (IInventoryHandler handler : handlers) {
            total += handler.countItems(player, requiredStack, containerManager);
        }
        return total;
    }

    public int useItems(EntityPlayer player, ItemStack requiredStack, int count) {
        ContainerManager containerManager = ConstructionWandLegacy.instance.containerManager;
        for (IInventoryHandler handler : handlers) {
            if (count <= 0) break;
            count = handler.useItems(player, requiredStack, count, containerManager);
        }
        return count;
    }

    public void addMatchingStacks(EntityPlayer player, Item item, Consumer<ItemStack> consumer) {
        for (IInventoryHandler handler : handlers) {
            handler.addMatchingStacks(player, item, consumer);
        }
    }
}
