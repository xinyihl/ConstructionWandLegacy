package com.xinyihl.constructionwandlegacy.compat;

import com.xinyihl.constructionwandlegacy.Tags;
import com.xinyihl.constructionwandlegacy.basics.option.WandDataCodec;
import com.xinyihl.constructionwandlegacy.basics.option.WandState;
import com.xinyihl.constructionwandlegacy.compat.ae2.AE2Compat;
import com.xinyihl.constructionwandlegacy.compat.baubles.BaublesMaterialSourceFactory;
import com.xinyihl.constructionwandlegacy.compat.projecte.ProjectEMaterialSourceFactory;
import com.xinyihl.constructionwandlegacy.items.core.ItemCoreAE;
import com.xinyihl.constructionwandlegacy.material.MaterialSourceFactory;
import com.xinyihl.constructionwandlegacy.material.MaterialSourceRegistry;
import com.xinyihl.constructionwandlegacy.wand.upgrade.IWandCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;

public final class CompatRegistrar {
    private static final String BAUBLES_MOD_ID = "baubles";
    private static final String PROJECTE_MOD_ID = "projecte";
    private static final String AE2_MOD_ID = "appliedenergistics2";

    private CompatRegistrar() {
    }

    public static void register(MaterialSourceRegistry registry) {
        if (Loader.isModLoaded(BAUBLES_MOD_ID)) {
            registry.registerInventorySource(BaublesProvider.MATERIAL_SOURCE_FACTORY);
        }
    }

    @Nullable
    public static MaterialSourceFactory getAE2MaterialSourceFactory() {
        return Loader.isModLoaded(AE2_MOD_ID) ? AE2Provider.MATERIAL_SOURCE_FACTORY : null;
    }

    @Nullable
    public static MaterialSourceFactory getProjectEMaterialSourceFactory() {
        return Loader.isModLoaded(PROJECTE_MOD_ID) ? ProjectEProvider.MATERIAL_SOURCE_FACTORY : null;
    }

    public static boolean tryBindAE(ItemStack wand, EntityPlayer player, World world, BlockPos pos) {
        if (!Loader.isModLoaded(AE2_MOD_ID)) {
            return false;
        }
        WandState state = WandDataCodec.read(wand);
        if (!(state.getSelectedCore() instanceof ItemCoreAE) || !AE2Provider.tryBind(wand, player, world, pos)) {
            return false;
        }
        ResourceLocation coreId = state.getSelectedCore().getRegistryName();
        if (coreId != null) {
            player.sendStatusMessage(new TextComponentTranslation(Tags.MOD_ID + ".option.cores." + coreId + ".bound"), true);
        }
        return true;
    }

    public static boolean hasAE2Binding(ItemStack wand, IWandCore selectedCore) {
        return Loader.isModLoaded(AE2_MOD_ID) && selectedCore instanceof ItemCoreAE && AE2Provider.hasBinding(wand);
    }

    private static final class BaublesProvider {
        private static final MaterialSourceFactory MATERIAL_SOURCE_FACTORY = new BaublesMaterialSourceFactory();
    }

    private static final class ProjectEProvider {
        private static final MaterialSourceFactory MATERIAL_SOURCE_FACTORY = new ProjectEMaterialSourceFactory();
    }

    private static final class AE2Provider {
        private static final MaterialSourceFactory MATERIAL_SOURCE_FACTORY = AE2Compat.materialSourceFactory();

        private static boolean tryBind(ItemStack wand, EntityPlayer player, World world, BlockPos pos) {
            return AE2Compat.tryBind(wand, player, world, pos);
        }

        private static boolean hasBinding(ItemStack wand) {
            return AE2Compat.hasBinding(wand);
        }
    }
}
