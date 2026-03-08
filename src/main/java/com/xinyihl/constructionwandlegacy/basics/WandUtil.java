package com.xinyihl.constructionwandlegacy.basics;

import com.xinyihl.constructionwandlegacy.items.wand.ItemWand;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class WandUtil {
    private WandUtil() {
    }

    public static boolean stackEquals(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return false;
        }
        return first.getItem() == second.getItem()
                && first.getMetadata() == second.getMetadata()
                && ItemStack.areItemStackTagsEqual(first, second);
    }

    public static List<ItemStack> getHotbarWithOffhand(EntityPlayer player) {
        ArrayList<ItemStack> hotbar = new ArrayList<>(10);
        for (int i = 0; i < 9; i++) {
            hotbar.add(player.inventory.getStackInSlot(i));
        }
        hotbar.add(player.getHeldItemOffhand());
        return hotbar;
    }

    public static List<ItemStack> getMainInv(EntityPlayer player) {
        ArrayList<ItemStack> mainInventory = new ArrayList<>();
        for (int i = 9; i < player.inventory.mainInventory.size(); i++) {
            mainInventory.add(player.inventory.getStackInSlot(i));
        }
        return mainInventory;
    }

    public static ItemStack holdingWand(EntityPlayer player) {
        ItemStack mainhand = player.getHeldItem(EnumHand.MAIN_HAND);
        if (!mainhand.isEmpty() && mainhand.getItem() instanceof ItemWand) {
            return mainhand;
        }

        ItemStack offhand = player.getHeldItem(EnumHand.OFF_HAND);
        if (!offhand.isEmpty() && offhand.getItem() instanceof ItemWand) {
            return offhand;
        }
        return ItemStack.EMPTY;
    }

    public static boolean isPositionPlaceable(World world, EntityPlayer player, BlockPos pos, boolean replace) {
        IBlockState state = world.getBlockState(pos);
        if (replace) {
            return state.getMaterial().isReplaceable();
        }
        return world.isAirBlock(pos);
    }

    public static boolean placeBlock(World world, EntityPlayer player, IBlockState state, BlockPos pos) {
        Block block = state.getBlock();
        if (!world.mayPlace(block, pos, false, null, player)) {
            return false;
        }
        return world.setBlockState(pos, state, 3);
    }

    public static boolean placeBlockAt(World world, EntityPlayer player, BlockPos pos, ItemStack placeStack,
                                       IBlockState state, @Nullable RayTraceResult rayTraceResult) {
        if (placeStack.isEmpty() || !(placeStack.getItem() instanceof ItemBlock)) {
            return false;
        }
        ItemBlock item = (ItemBlock) placeStack.getItem();

        EnumFacing facing = rayTraceResult != null && rayTraceResult.sideHit != null ? rayTraceResult.sideHit : EnumFacing.UP;
        float hitX = 0.5F;
        float hitY = 0.5F;
        float hitZ = 0.5F;

        if (rayTraceResult != null && rayTraceResult.hitVec != null) {
            Vec3d hit = rayTraceResult.hitVec.subtract(pos.getX(), pos.getY(), pos.getZ());
            hitX = (float) hit.x;
            hitY = (float) hit.y;
            hitZ = (float) hit.z;
        }

        ItemStack stack = placeStack.copy();
        stack.setCount(1);
        IBlockState placedAgainst = world.getBlockState(pos.offset(facing.getOpposite()));
        BlockSnapshot snapshot = BlockSnapshot.getBlockSnapshot(world, pos);

        if (!item.placeBlockAt(stack, player, world, pos, facing, hitX, hitY, hitZ, state)) {
            return false;
        }

        BlockEvent.EntityPlaceEvent placeEvent = new BlockEvent.EntityPlaceEvent(snapshot, placedAgainst, player);
        MinecraftForge.EVENT_BUS.post(placeEvent);
        if (placeEvent.isCanceled()) {
            world.setBlockState(pos, snapshot.getReplacedBlock(), 3);
            return false;
        }

        IBlockState placed = world.getBlockState(pos);
        placed.getBlock().onBlockPlacedBy(world, pos, placed, player, stack);
        return true;
    }

    public static boolean removeBlock(World world, @Nullable EntityPlayer player, @Nullable IBlockState expectedBlock, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock().isAir(state, world, pos)) {
            return false;
        }

        if (expectedBlock != null) {
            if (!ReplacementRegistry.matchBlocks(state.getBlock(), expectedBlock.getBlock())) {
                return false;
            }
            int currentMeta = state.getBlock().getMetaFromState(state);
            int expectedMeta = expectedBlock.getBlock().getMetaFromState(expectedBlock);
            if (currentMeta != expectedMeta) {
                return false;
            }
        }

        if (player == null) {
            return world.setBlockToAir(pos);
        }

        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(world, pos, state, player);
        MinecraftForge.EVENT_BUS.post(breakEvent);
        if (breakEvent.isCanceled()) {
            return false;
        }

        Block block = state.getBlock();
        if (!block.removedByPlayer(state, world, pos, player, false)) {
            return false;
        }
        block.onPlayerDestroy(world, pos, state);
        return true;
    }

    public static boolean isBlockRemovable(World world, EntityPlayer player, BlockPos pos) {
        if (world.isAirBlock(pos)) {
            return false;
        }

        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity != null) {
            return false;
        }

        return world.isBlockModifiable(player, pos) && world.getWorldBorder().contains(pos);
    }

    public static boolean isBlockPermeable(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getMaterial().isReplaceable() || state.getCollisionBoundingBox(world, pos) == Block.NULL_AABB;
    }
}
