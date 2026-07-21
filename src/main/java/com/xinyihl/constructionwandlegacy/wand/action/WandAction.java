package com.xinyihl.constructionwandlegacy.wand.action;

import com.xinyihl.constructionwandlegacy.wand.WandContext;
import com.xinyihl.constructionwandlegacy.wand.WandOperation;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public interface WandAction {
    int getLimit(ItemStack wand);

    default int getLimit(WandContext context) {
        return getLimit(context.getWand());
    }

    List<WandOperation> plan(WandContext context, OperationResolver resolver, int limit);

    default List<WandOperation> planFromAir(WandContext context, OperationResolver resolver, int limit) {
        return Collections.emptyList();
    }

    interface OperationResolver {
        @Nullable
        WandOperation createPlacement(BlockPos pos, @Nullable IBlockState supportingBlock);

        @Nullable
        WandOperation createDestruction(BlockPos pos);
    }
}
