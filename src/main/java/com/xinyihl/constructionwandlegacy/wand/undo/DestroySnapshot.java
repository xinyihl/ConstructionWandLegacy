package com.xinyihl.constructionwandlegacy.wand.undo;

import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class DestroySnapshot implements ISnapshot {
    private final IBlockState block;
    private final BlockPos pos;

    public DestroySnapshot(IBlockState block, BlockPos pos) {
        this.block = block;
        this.pos = pos;
    }

    @Nullable
    public static DestroySnapshot get(World world, EntityPlayer player, BlockPos pos) {
        if (!WandUtil.isBlockRemovable(world, player, pos)) {
            return null;
        }
        return new DestroySnapshot(world.getBlockState(pos), pos);
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public IBlockState getBlockState() {
        return block;
    }

    @Override
    public ItemStack getRequiredItems() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean execute(World world, EntityPlayer player, RayTraceResult rayTraceResult) {
        return WandUtil.removeBlock(world, player, block, pos);
    }

    @Override
    public boolean canRestore(World world, EntityPlayer player) {
        return world.isAirBlock(pos);
    }

    @Override
    public boolean restore(World world, EntityPlayer player) {
        return WandUtil.placeBlock(world, player, block, pos);
    }

    @Override
    public void forceRestore(World world) {
        world.setBlockState(pos, block, 3);
    }
}
