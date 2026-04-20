package com.xinyihl.constructionwandlegacy.api;

import com.xinyihl.constructionwandlegacy.compat.containers.ContainerManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;

public interface IInventoryHandler {
    int countItems(EntityPlayer player, ItemStack requiredStack, ContainerManager containerManager);

    int useItems(EntityPlayer player, ItemStack requiredStack, int count, ContainerManager containerManager);

    void addMatchingStacks(EntityPlayer player, Item item, Consumer<ItemStack> consumer);
}
