package com.xinyihl.constructionwandlegacy.compat.containers;

import com.xinyihl.constructionwandlegacy.api.IContainerHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;

public class ContainerManager {
    private final ArrayList<IContainerHandler> handlers;

    public ContainerManager() {
        handlers = new ArrayList<>();
    }

    public boolean register(IContainerHandler handler) {
        return handlers.add(handler);
    }

    public int countItems(EntityPlayer player, ItemStack itemStack, ItemStack inventoryStack) {
        for (IContainerHandler handler : handlers) {
            if (handler.matches(player, itemStack, inventoryStack)) {
                return handler.countItems(player, itemStack, inventoryStack);
            }
        }
        return 0;
    }

    public int useItems(EntityPlayer player, ItemStack itemStack, ItemStack inventoryStack, int count) {
        for (IContainerHandler handler : handlers) {
            if (handler.matches(player, itemStack, inventoryStack)) {
                return handler.useItems(player, itemStack, inventoryStack, count);
            }
        }
        return count;
    }
}
