package com.xinyihl.constructionwandlegacy.wand.action;

import com.xinyihl.constructionwandlegacy.api.IWandAction;
import com.xinyihl.constructionwandlegacy.api.IWandSupplier;
import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.basics.config.ConfigServer;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.wand.undo.DestroySnapshot;
import com.xinyihl.constructionwandlegacy.wand.undo.ISnapshot;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class ActionDestruction implements IWandAction {
    private static void addPlaneCandidates(LinkedList<BlockPos> candidates, BlockPos origin,
                                           EnumFacing firstA, EnumFacing firstB,
                                           EnumFacing secondA, EnumFacing secondB,
                                           boolean useFirstAxis, boolean useSecondAxis) {
        if (useFirstAxis) {
            candidates.add(origin.offset(firstA));
            candidates.add(origin.offset(firstB));
        }
        if (useSecondAxis) {
            candidates.add(origin.offset(secondA));
            candidates.add(origin.offset(secondB));
        }
        if (useFirstAxis && useSecondAxis) {
            candidates.add(origin.offset(firstA).offset(secondA));
            candidates.add(origin.offset(firstA).offset(secondB));
            candidates.add(origin.offset(firstB).offset(secondA));
            candidates.add(origin.offset(firstB).offset(secondB));
        }
    }

    @Override
    public int getLimit(ItemStack wand) {
        return ConfigServer.getWandProperties(wand.getItem()).getDestruction();
    }

    @Nonnull
    @Override
    public List<ISnapshot> getSnapshots(World world, EntityPlayer player, RayTraceResult rayTraceResult, ItemStack wand,
                                        WandOptions options, IWandSupplier supplier, int limit) {
        LinkedList<ISnapshot> destroySnapshots = new LinkedList<>();
        LinkedList<BlockPos> candidates = new LinkedList<>();
        HashSet<BlockPos> allCandidates = new HashSet<>();

        EnumFacing breakFace = rayTraceResult.sideHit;
        BlockPos startingPoint = rayTraceResult.getBlockPos();
        IBlockState targetBlock = world.getBlockState(startingPoint);

        if (breakFace == EnumFacing.UP || breakFace == EnumFacing.DOWN) {
            if (options.testLock(WandOptions.LOCK.NORTHSOUTH) || options.testLock(WandOptions.LOCK.EASTWEST)) {
                candidates.add(startingPoint);
            }
        } else if (options.testLock(WandOptions.LOCK.HORIZONTAL) || options.testLock(WandOptions.LOCK.VERTICAL)) {
            candidates.add(startingPoint);
        }

        while (!candidates.isEmpty() && destroySnapshots.size() < limit) {
            BlockPos currentCandidate = candidates.removeFirst();

            if (!WandUtil.isBlockPermeable(world, currentCandidate.offset(breakFace))) {
                continue;
            }

            try {
                IBlockState candidateBlock = world.getBlockState(currentCandidate);
                if (options.matchBlocks(targetBlock, candidateBlock)
                        && allCandidates.add(currentCandidate)) {
                    DestroySnapshot snapshot = DestroySnapshot.get(world, player, currentCandidate);
                    if (snapshot == null) {
                        continue;
                    }
                    destroySnapshots.add(snapshot);

                    switch (breakFace) {
                        case DOWN:
                        case UP:
                            addPlaneCandidates(candidates, currentCandidate,
                                    EnumFacing.NORTH, EnumFacing.SOUTH,
                                    EnumFacing.EAST, EnumFacing.WEST,
                                    options.testLock(WandOptions.LOCK.NORTHSOUTH),
                                    options.testLock(WandOptions.LOCK.EASTWEST));
                            break;
                        case NORTH:
                        case SOUTH:
                            addPlaneCandidates(candidates, currentCandidate,
                                    EnumFacing.EAST, EnumFacing.WEST,
                                    EnumFacing.UP, EnumFacing.DOWN,
                                    options.testLock(WandOptions.LOCK.HORIZONTAL),
                                    options.testLock(WandOptions.LOCK.VERTICAL));
                            break;
                        case EAST:
                        case WEST:
                            addPlaneCandidates(candidates, currentCandidate,
                                    EnumFacing.NORTH, EnumFacing.SOUTH,
                                    EnumFacing.UP, EnumFacing.DOWN,
                                    options.testLock(WandOptions.LOCK.HORIZONTAL),
                                    options.testLock(WandOptions.LOCK.VERTICAL));
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return destroySnapshots;
    }

    @Nonnull
    @Override
    public List<ISnapshot> getSnapshotsFromAir(World world, EntityPlayer player, RayTraceResult rayTraceResult, ItemStack wand,
                                               WandOptions options, IWandSupplier supplier, int limit) {
        return new ArrayList<>();
    }
}
