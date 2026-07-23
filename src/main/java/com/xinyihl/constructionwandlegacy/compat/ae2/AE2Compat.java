package com.xinyihl.constructionwandlegacy.compat.ae2;

import ae2.tile.networking.TileController;
import com.xinyihl.constructionwandlegacy.basics.option.WandDataCodec;
import com.xinyihl.constructionwandlegacy.material.MaterialSourceFactory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;

public final class AE2Compat {
    private static final String TAG_BOUND_POS = "ae_bound_pos";
    private static final String TAG_BOUND_DIM = "ae_bound_dim";

    private AE2Compat() {
    }

    public static MaterialSourceFactory materialSourceFactory() {
        return FactoryHolder.MATERIAL_SOURCE_FACTORY;
    }

    @Optional.Method(modid = "ae2")
    public static boolean tryBind(ItemStack wand, EntityPlayer player, World world, BlockPos pos) {
        if (wand == null || wand.isEmpty() || player == null || world == null || pos == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileController)) {
            return false;
        }
        WandDataCodec.update(wand, data -> {
            data.setIntArray(TAG_BOUND_POS, new int[]{pos.getX(), pos.getY(), pos.getZ()});
            data.setInteger(TAG_BOUND_DIM, world.provider.getDimension());
        });
        return true;
    }

    public static boolean hasBinding(ItemStack wand) {
        NBTTagCompound data = WandDataCodec.readData(wand);
        return data.hasKey(TAG_BOUND_POS) && data.hasKey(TAG_BOUND_DIM);
    }

    @Nullable
    static Binding readBinding(ItemStack wand) {
        NBTTagCompound data = WandDataCodec.readData(wand);
        int[] position = data.getIntArray(TAG_BOUND_POS);
        if (position.length < 3 || !data.hasKey(TAG_BOUND_DIM)) {
            return null;
        }
        return new Binding(new BlockPos(position[0], position[1], position[2]), data.getInteger(TAG_BOUND_DIM));
    }

    static final class Binding {
        private final BlockPos position;
        private final int dimension;

        private Binding(BlockPos position, int dimension) {
            this.position = position;
            this.dimension = dimension;
        }

        BlockPos getPosition() {
            return position;
        }

        int getDimension() {
            return dimension;
        }
    }

    private static final class FactoryHolder {
        private static final MaterialSourceFactory MATERIAL_SOURCE_FACTORY = new AE2MaterialSourceFactory();
    }
}
