package com.xinyihl.constructionwandlegacy.wand.undo;

import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.basics.config.ConfigServer;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collection;

public class PlaceSnapshot implements ISnapshot {
    private final BlockPos pos;
    private final ItemStack itemStack;
    private final ItemBlock item;
    private final IBlockState supportingBlock;
    private final boolean targetMode;
    private IBlockState block;

    public PlaceSnapshot(IBlockState block, BlockPos pos, ItemStack itemStack, ItemBlock item, @Nullable IBlockState supportingBlock, boolean targetMode) {
        this.block = block;
        this.pos = pos;
        this.itemStack = itemStack;
        this.item = item;
        this.supportingBlock = supportingBlock;
        this.targetMode = targetMode;
    }

    @Nullable
    public static PlaceSnapshot get(World world, EntityPlayer player, RayTraceResult rayTraceResult,
                                    BlockPos pos, ItemStack itemStack,
                                    @Nullable IBlockState supportingBlock,
                                    @Nullable WandOptions options) {
        if (!(itemStack.getItem() instanceof ItemBlock)) {
            return null;
        }
        ItemBlock item = (ItemBlock) itemStack.getItem();
        boolean targetMode = options != null && supportingBlock != null && options.direction.get() == WandOptions.DIRECTION.TARGET;
        IBlockState state = getPlaceBlockState(world, player, rayTraceResult, pos, itemStack, item, supportingBlock, targetMode);
        if (state == null || !ConfigServer.isPlacementAllowed(itemStack, state)) {
            return null;
        }
        return new PlaceSnapshot(state, pos, itemStack.copy(), item, supportingBlock, targetMode);
    }

    @Nullable
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static IBlockState getPlaceBlockState(World world, EntityPlayer player, RayTraceResult rayTraceResult,
                                                  BlockPos pos, ItemStack itemStack, ItemBlock item,
                                                  @Nullable IBlockState supportingBlock, boolean targetMode) {
        Block block = item.getBlock();
        EnumFacing facing = rayTraceResult == null || rayTraceResult.sideHit == null ? EnumFacing.UP : rayTraceResult.sideHit;

        float hitX = 0.5f;
        float hitY = 0.5f;
        float hitZ = 0.5f;
        if (rayTraceResult != null && rayTraceResult.hitVec != null) {
            Vec3d hit = rayTraceResult.hitVec.subtract(pos.getX(), pos.getY(), pos.getZ());
            hitX = (float) hit.x;
            hitY = (float) hit.y;
            hitZ = (float) hit.z;
        }

        if (!world.mayPlace(block, pos, false, facing, player)) {
            return null;
        }

        IBlockState state = block.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, itemStack.getMetadata(), player, EnumHand.MAIN_HAND);

        if (targetMode && supportingBlock != null) {
            Collection<IProperty<?>> sourceProps = supportingBlock.getPropertyKeys();
            for (IProperty property : state.getPropertyKeys()) {
                if (!ConfigServer.isPropertyCopyAllowed(property)) {
                    continue;
                }

                IProperty<?> source = sourceProps.stream()
                        .filter(p -> p.getName().equals(property.getName()))
                        .findFirst()
                        .orElse(null);
                if (source == null) {
                    continue;
                }

                Comparable value = supportingBlock.getValue((IProperty) source);
                if (property.getAllowedValues().contains(value)) {
                    state = state.withProperty(property, value);
                }
            }
        }

        AxisAlignedBB aabb = state.getCollisionBoundingBox(world, pos);
        if (aabb != null && !world.checkNoEntityCollision(aabb.offset(pos))) {
            return null;
        }
        return state;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public IBlockState getBlockState() {
        return block;
    }

    @Override
    public ItemStack getRequiredItems() {
        ItemStack required = itemStack.copy();
        required.setCount(1);
        return required;
    }

    @Override
    public boolean canRestore(World world, EntityPlayer player) {
        return world.isBlockModifiable(player, pos);
    }

    @Override
    public boolean restore(World world, EntityPlayer player) {
        if (!WandUtil.removeBlock(world, player, block, pos)) {
            return false;
        }

        if (!player.isCreative()) {
            ItemStack refund = getRequiredItems();
            if (!player.inventory.addItemStackToInventory(refund)) {
                player.dropItem(refund, false);
            }
            player.inventory.markDirty();
        }

        return true;
    }

    @Override
    public void forceRestore(World world) {
        world.setBlockToAir(pos);
    }

    @Override
    public boolean execute(World world, EntityPlayer player, RayTraceResult rayTraceResult) {
        IBlockState recalculated = getPlaceBlockState(world, player, rayTraceResult, pos, itemStack, item, supportingBlock, targetMode);
        if (recalculated == null) {
            return false;
        }
        block = recalculated;
        return WandUtil.placeBlockAt(world, player, pos, itemStack, block, rayTraceResult);
    }
}
