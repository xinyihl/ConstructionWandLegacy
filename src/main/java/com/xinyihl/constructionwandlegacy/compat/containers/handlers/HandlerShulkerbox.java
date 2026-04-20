package com.xinyihl.constructionwandlegacy.compat.containers.handlers;

import com.xinyihl.constructionwandlegacy.api.IContainerHandler;
import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants;

public class HandlerShulkerbox implements IContainerHandler {
    private static final int SLOTS = 27;

    @Override
    public boolean matches(EntityPlayer player, ItemStack itemStack, ItemStack inventoryStack) {
        if (inventoryStack.isEmpty() || inventoryStack.getCount() != 1) {
            return false;
        }

        Block block = Block.getBlockFromItem(inventoryStack.getItem());
        return block instanceof BlockShulkerBox;
    }

    @Override
    public int countItems(EntityPlayer player, ItemStack itemStack, ItemStack inventoryStack) {
        int count = 0;
        for (ItemStack stack : getItemList(inventoryStack)) {
            if (WandUtil.stackEquals(stack, itemStack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public int useItems(EntityPlayer player, ItemStack itemStack, ItemStack inventoryStack, int count) {
        NonNullList<ItemStack> itemList = getItemList(inventoryStack);
        boolean changed = false;

        for (ItemStack stack : itemList) {
            if (WandUtil.stackEquals(stack, itemStack)) {
                int toTake = Math.min(count, stack.getCount());
                stack.shrink(toTake);
                count -= toTake;
                changed = true;

                if (count == 0) {
                    break;
                }
            }
        }

        if (changed) {
            setItemList(inventoryStack, itemList);
            player.inventory.markDirty();
        }

        return count;
    }

    private NonNullList<ItemStack> getItemList(ItemStack itemStack) {
        NonNullList<ItemStack> itemStacks = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        NBTTagCompound rootTag = itemStack.getTagCompound();
        if (rootTag != null && rootTag.hasKey("BlockEntityTag", Constants.NBT.TAG_COMPOUND)) {
            NBTTagCompound entityTag = rootTag.getCompoundTag("BlockEntityTag");
            if (entityTag.hasKey("Items", Constants.NBT.TAG_LIST)) {
                ItemStackHelper.loadAllItems(entityTag, itemStacks);
            }
        }
        return itemStacks;
    }

    private void setItemList(ItemStack itemStack, NonNullList<ItemStack> itemStacks) {
        NBTTagCompound rootTag = itemStack.getOrCreateSubCompound("BlockEntityTag");
        NBTTagList listTag = new NBTTagList();
        ItemStackHelper.saveAllItems(rootTag, itemStacks);
        if (!rootTag.hasKey("Items", Constants.NBT.TAG_LIST)) {
            rootTag.setTag("Items", listTag);
        }
    }
}
