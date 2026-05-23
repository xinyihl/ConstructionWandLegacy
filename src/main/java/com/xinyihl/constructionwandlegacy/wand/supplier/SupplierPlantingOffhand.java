package com.xinyihl.constructionwandlegacy.wand.supplier;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.api.IWandSupplier;
import com.xinyihl.constructionwandlegacy.compat.inventory.InventoryManager;
import com.xinyihl.constructionwandlegacy.compat.inventory.handlers.HandlerContainer;
import com.xinyihl.constructionwandlegacy.wand.undo.ISnapshot;
import com.xinyihl.constructionwandlegacy.wand.undo.PlantSnapshot;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class SupplierPlantingOffhand implements IWandSupplier {
    private final EntityPlayer player;
    private final ItemStack seedStack;
    private final HandlerContainer containerHandler;
    private int seedCount;

    public SupplierPlantingOffhand(EntityPlayer player, ItemStack seedStack) {
        this.player = player;
        this.seedStack = seedStack.copy();
        this.seedStack.setCount(1);
        this.containerHandler = new HandlerContainer();
    }

    public static boolean isPlantable(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraftforge.common.IPlantable;
    }

    @Override
    public void getSupply(@Nullable ItemStack target) {
        if (player.isCreative()) {
            seedCount = Integer.MAX_VALUE;
            return;
        }

        InventoryManager inventoryManager = ConstructionWandLegacy.instance.inventoryManager;
        seedCount = inventoryManager.countItems(player, seedStack);
        seedCount += containerHandler.countItems(player, seedStack, null);
    }

    @Nullable
    @Override
    public ISnapshot getPlaceSnapshot(World world, BlockPos pos, RayTraceResult rayTraceResult,
                                      @Nullable IBlockState supportingBlock) {
        if (seedCount <= 0) {
            return null;
        }

        PlantSnapshot snapshot = PlantSnapshot.get(world, player, pos.down(), seedStack);
        if (snapshot == null) {
            return null;
        }

        seedCount--;
        return snapshot;
    }

    @Override
    public int takeItemStack(ItemStack stack) {
        int count = stack.getCount();
        if (count <= 0 || player.isCreative()) {
            return 0;
        }

        int remaining = containerHandler.useItems(player, stack, count, null);
        if (remaining > 0) {
            InventoryManager inventoryManager = ConstructionWandLegacy.instance.inventoryManager;
            remaining = inventoryManager.useItems(player, stack, remaining);
        }
        return remaining;
    }
}
