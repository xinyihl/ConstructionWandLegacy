package com.xinyihl.constructionwandlegacy.wand.action;

import com.xinyihl.constructionwandlegacy.api.IWandAction;
import com.xinyihl.constructionwandlegacy.api.IWandSupplier;
import com.xinyihl.constructionwandlegacy.basics.config.ConfigServer;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.wand.undo.ISnapshot;
import com.xinyihl.constructionwandlegacy.wand.undo.PlaceSnapshot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

public class ActionAngel implements IWandAction {
    @Override
    public int getLimit(ItemStack wand) {
        return ConfigServer.getWandProperties(wand.getItem()).getAngel();
    }

    @Nonnull
    @Override
    public List<ISnapshot> getSnapshots(World world, EntityPlayer player, RayTraceResult rayTraceResult, ItemStack wand,
                                        WandOptions options, IWandSupplier supplier, int limit) {
        LinkedList<ISnapshot> placeSnapshots = new LinkedList<>();

        EnumFacing placeDirection = rayTraceResult.sideHit;
        BlockPos currentPos = rayTraceResult.getBlockPos();

        for (int i = 0; i < limit; i++) {
            currentPos = currentPos.offset(placeDirection.getOpposite());

            PlaceSnapshot snapshot = supplier.getPlaceSnapshot(world, currentPos, rayTraceResult,
                    world.getBlockState(rayTraceResult.getBlockPos()));
            if (snapshot != null) {
                placeSnapshots.add(snapshot);
                break;
            }
        }
        return placeSnapshots;
    }

    @Nonnull
    @Override
    public List<ISnapshot> getSnapshotsFromAir(World world, EntityPlayer player, RayTraceResult rayTraceResult, ItemStack wand,
                                               WandOptions options, IWandSupplier supplier, int limit) {
        LinkedList<ISnapshot> placeSnapshots = new LinkedList<>();
        Vec3d placeVec = player.getPositionVector().add(player.getLookVec().scale(2));
        BlockPos currentPos = new BlockPos(placeVec);

        PlaceSnapshot snapshot = supplier.getPlaceSnapshot(world, currentPos, rayTraceResult, null);
        if (snapshot != null) {
            placeSnapshots.add(snapshot);
        }
        return placeSnapshots;
    }
}
