package com.xinyihl.constructionwandlegacy.wand.action;

import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.items.wand.ItemWand;
import com.xinyihl.constructionwandlegacy.wand.WandContext;
import com.xinyihl.constructionwandlegacy.wand.WandOperation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

import java.util.Collections;
import java.util.List;

public final class ActionDestruction implements WandAction {
    public static final ActionDestruction INSTANCE = new ActionDestruction();

    private ActionDestruction() {
    }

    @Override
    public int getLimit(ItemStack wand) {
        return ((ItemWand) wand.getItem()).getSpec().getDestructionLimit();
    }

    @Override
    public List<WandOperation> plan(WandContext context, OperationResolver resolver, int limit) {
        RayTraceResult hit = context.getRayTraceResult();
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.sideHit == null) {
            return Collections.emptyList();
        }

        EnumFacing breakFace = hit.sideHit;
        BlockPos startingPoint = hit.getBlockPos();
        IBlockState targetBlock = context.getWorld().getBlockState(startingPoint);
        return PlaneTraversal.traverse(startingPoint, breakFace, context.getState().getLock(), limit,
                candidate -> {
                    if (!WandUtil.isBlockPermeable(context.getWorld(), candidate.offset(breakFace))) {
                        return null;
                    }
                    IBlockState candidateBlock = context.getWorld().getBlockState(candidate);
                    return context.matchesBlocks(targetBlock, candidateBlock)
                            ? resolver.createDestruction(candidate)
                            : null;
                });
    }
}
