package com.xinyihl.constructionwandlegacy.api;

import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.wand.undo.ISnapshot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.List;

public interface IWandAction {
    int getLimit(ItemStack wand);

    @Nonnull
    List<ISnapshot> getSnapshots(World world, EntityPlayer player, RayTraceResult rayTraceResult,
                                 ItemStack wand, WandOptions options, IWandSupplier supplier, int limit);

    @Nonnull
    List<ISnapshot> getSnapshotsFromAir(World world, EntityPlayer player, RayTraceResult rayTraceResult,
                                        ItemStack wand, WandOptions options, IWandSupplier supplier, int limit);
}
