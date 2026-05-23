package com.xinyihl.constructionwandlegacy.wand.undo;

import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.compat.inventory.handlers.HandlerContainer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;

import javax.annotation.Nullable;

public class PlantSnapshot implements ISnapshot {
    private final BlockPos pos;
    private final ItemStack seedStack;
    private IBlockState cropBlock;

    private PlantSnapshot(BlockPos pos, ItemStack seedStack, IBlockState cropBlock) {
        this.pos = pos;
        this.seedStack = seedStack;
        this.cropBlock = cropBlock;
    }

    @Nullable
    public static PlantSnapshot get(World world, EntityPlayer player, BlockPos farmlandPos, ItemStack seedStack) {
        if (seedStack.isEmpty() || !(seedStack.getItem() instanceof IPlantable)) {
            return null;
        }

        BlockPos cropPos = farmlandPos.up();
        if (!world.isAirBlock(cropPos)
                || !world.isBlockModifiable(player, cropPos)
                || !player.canPlayerEdit(cropPos, EnumFacing.UP, seedStack)) {
            return null;
        }

        IPlantable plantable = (IPlantable) seedStack.getItem();
        IBlockState farmland = world.getBlockState(farmlandPos);
        if (!farmland.getBlock().canSustainPlant(farmland, world, farmlandPos, EnumFacing.UP, plantable)) {
            return null;
        }

        IBlockState plantState = plantable.getPlant(world, cropPos);
        if (plantState == null) {
            return null;
        }

        return new PlantSnapshot(cropPos, seedStack.copy(), plantState);
    }

    private static void refundToInventoryOrContainer(EntityPlayer player, ItemStack refund) {
        if (new HandlerContainer().tryInsert(player, refund)) {
            return;
        }
        if (!player.inventory.addItemStackToInventory(refund)) {
            player.dropItem(refund, false);
        }
        player.inventory.markDirty();
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public IBlockState getBlockState() {
        return cropBlock;
    }

    @Override
    public ItemStack getRequiredItems() {
        ItemStack required = seedStack.copy();
        required.setCount(1);
        return required;
    }

    @Override
    public boolean execute(World world, EntityPlayer player, RayTraceResult rayTraceResult) {
        if (!world.isAirBlock(pos)) {
            return false;
        }

        IPlantable plantable = (IPlantable) seedStack.getItem();
        BlockPos farmlandPos = pos.down();
        IBlockState farmland = world.getBlockState(farmlandPos);
        if (!farmland.getBlock().canSustainPlant(farmland, world, farmlandPos, EnumFacing.UP, plantable)
                || !player.canPlayerEdit(pos, EnumFacing.UP, seedStack)) {
            return false;
        }

        IBlockState plantState = plantable.getPlant(world, pos);
        if (plantState == null) {
            return false;
        }

        BlockSnapshot snapshot = BlockSnapshot.getBlockSnapshot(world, pos);
        if (!world.setBlockState(pos, plantState, 3)) {
            return false;
        }

        BlockEvent.EntityPlaceEvent placeEvent = new BlockEvent.EntityPlaceEvent(snapshot, farmland, player);
        MinecraftForge.EVENT_BUS.post(placeEvent);
        if (placeEvent.isCanceled()) {
            world.setBlockState(pos, snapshot.getReplacedBlock(), 3);
            return false;
        }

        cropBlock = world.getBlockState(pos);
        return true;
    }

    @Override
    public boolean canRestore(World world, EntityPlayer player) {
        return world.isBlockModifiable(player, pos);
    }

    @Override
    public boolean restore(World world, EntityPlayer player) {
        if (!WandUtil.removeBlock(world, player, cropBlock, pos)) {
            return false;
        }

        if (!player.isCreative()) {
            ItemStack refund = getRequiredItems();
            refundToInventoryOrContainer(player, refund);
        }
        return true;
    }

    @Override
    public void forceRestore(World world) {
        IBlockState state = world.getBlockState(pos);
        if (isSameCrop(state)) {
            world.setBlockToAir(pos);
        }
    }

    private boolean isSameCrop(IBlockState state) {
        if (cropBlock == null) {
            return false;
        }

        Block block = state.getBlock();
        Block expected = cropBlock.getBlock();
        return block == expected
                && block.getMetaFromState(state) == expected.getMetaFromState(cropBlock);
    }
}
