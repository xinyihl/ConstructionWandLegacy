package com.xinyihl.constructionwandlegacy.wand.supplier;

import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.basics.pool.RandomPool;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Random;

public class SupplierRandom extends SupplierInventory {
    public SupplierRandom(EntityPlayer player, WandOptions options) {
        super(player, options);
    }

    @Override
    public void getSupply(@Nullable ItemStack target) {
        itemCounts = new LinkedHashMap<>();
        itemPool = new RandomPool<>(new Random());

        for (ItemStack stack : WandUtil.getHotbarWithOffhand(player)) {
            if (stack.getItem() instanceof ItemBlock) {
                addBlockStack(stack);
            }
        }
    }
}
