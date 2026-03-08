package com.xinyihl.constructionwandlegacy.wand.undo;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public interface ISnapshot {
    BlockPos getPos();

    IBlockState getBlockState();

    ItemStack getRequiredItems();

    boolean execute(World world, EntityPlayer player, RayTraceResult rayTraceResult);

    boolean canRestore(World world, EntityPlayer player);

    boolean restore(World world, EntityPlayer player);

    void forceRestore(World world);
}
