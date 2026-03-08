package com.xinyihl.constructionwandlegacy.api;

import com.xinyihl.constructionwandlegacy.wand.undo.PlaceSnapshot;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public interface IWandSupplier {
    void getSupply(@Nullable ItemStack target);

    @Nullable
    PlaceSnapshot getPlaceSnapshot(World world, BlockPos pos, RayTraceResult rayTraceResult,
                                   @Nullable IBlockState supportingBlock);

    int takeItemStack(ItemStack stack);
}
