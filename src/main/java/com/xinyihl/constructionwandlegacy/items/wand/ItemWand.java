package com.xinyihl.constructionwandlegacy.items.wand;

import appeng.tile.networking.TileController;
import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.Tags;
import com.xinyihl.constructionwandlegacy.api.IWandCore;
import com.xinyihl.constructionwandlegacy.basics.option.IOption;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.compat.inventory.handlers.HandlerAE;
import com.xinyihl.constructionwandlegacy.compat.inventory.handlers.HandlerContainer;
import com.xinyihl.constructionwandlegacy.items.core.CoreDefault;
import com.xinyihl.constructionwandlegacy.items.core.ItemCoreAE;
import com.xinyihl.constructionwandlegacy.wand.WandJob;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;
import java.util.List;

public abstract class ItemWand extends Item {
    protected ItemWand() {
        setMaxStackSize(1);
        addPropertyOverride(ConstructionWandLegacy.loc("using_core"), (stack, worldIn, entityIn) -> hasCustomCore(stack) ? 1.0F : 0.0F);
    }

    private static boolean hasCustomCore(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        WandOptions options = new WandOptions(stack);
        return options.cores.get().getColor() > -1;
    }

    @Optional.Method(modid = "appliedenergistics2")
    private boolean bindAE(ItemStack stack, EntityPlayer player, World world, BlockPos pos) {
        WandOptions options = new WandOptions(stack);
        if (options.cores.get() instanceof ItemCoreAE) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileController) {
                HandlerAE.storeBinding(options, pos, world.provider.getDimension());
                player.sendStatusMessage(new TextComponentTranslation(Tags.MOD_ID + ".option.cores." + options.cores.get().getRegistryName().toString() + ".bound"), true);
                return true;
            }
        }
        return false;
    }

    private boolean bindContainer(ItemStack stack, EntityPlayer player, World world, BlockPos pos) {
        WandOptions options = new WandOptions(stack);
        // Only default core supports container binding
        if (!(options.cores.get() instanceof CoreDefault)) {
            return false;
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) return false;
        if (!tile.hasCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
            return false;
        }
        HandlerContainer.storeBinding(options, pos, world.provider.getDimension());
        player.sendStatusMessage(new TextComponentTranslation(Tags.MOD_ID + ".tooltip.container_bound"), true);
        return true;
    }

    public static void optionMessage(IOption<?> option, List<String> out) {
        out.add(TextFormatting.AQUA + I18n.translateToLocal(option.getKeyTranslation())
                + TextFormatting.WHITE + I18n.translateToLocal(option.getValueTranslation()));
    }

    public static void optionMessage(EntityPlayer player, IOption<?> option) {
        ITextComponent key = new TextComponentTranslation(option.getKeyTranslation());
        key.getStyle().setColor(TextFormatting.AQUA);

        ITextComponent value = new TextComponentTranslation(option.getValueTranslation());
        value.getStyle().setColor(TextFormatting.WHITE);

        ITextComponent desc = new TextComponentTranslation(option.getDescTranslation());
        desc.getStyle().setColor(TextFormatting.WHITE);

        key.appendSibling(value);
        key.appendSibling(new TextComponentString(" - ").setStyle(key.getStyle().createShallowCopy().setColor(TextFormatting.GRAY)));
        key.appendSibling(desc);

        player.sendStatusMessage(key, true);
    }

    public int remainingDurability(ItemStack stack) {
        return Integer.MAX_VALUE;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        ItemStack stack = player.getHeldItem(hand);

        if (Loader.isModLoaded("appliedenergistics2")) {
            if (bindAE(stack, player, world, pos)) {
                return EnumActionResult.SUCCESS;
            }
        }

        if (player.isSneaking() && bindContainer(stack, player, world, pos)) {
            return EnumActionResult.SUCCESS;
        }

        if (player.isSneaking()) {
            return ConstructionWandLegacy.instance.undoHistory.undo(player) ? EnumActionResult.SUCCESS : EnumActionResult.FAIL;
        }

        RayTraceResult hitResult = new RayTraceResult(new Vec3d(pos).add(hitX, hitY, hitZ), facing, pos);
        WandJob job = new WandJob(player, world, hitResult, stack);
        job.getSnapshots();
        return job.doIt() ? EnumActionResult.SUCCESS : EnumActionResult.FAIL;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        if (player.isSneaking()) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        RayTraceResult miss = new RayTraceResult(RayTraceResult.Type.MISS,
                player.getPositionEyes(1.0F),
                EnumFacing.getFacingFromVector((float) player.getLookVec().x, (float) player.getLookVec().y, (float) player.getLookVec().z),
                player.getPosition());
        WandJob job = new WandJob(player, world, miss, stack);
        job.getSnapshots();
        return new ActionResult<>(job.doIt() ? EnumActionResult.SUCCESS : EnumActionResult.FAIL, stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        WandOptions options = new WandOptions(stack);
        int limit = options.cores.get().getWandAction().getLimit(stack);
        if (GuiScreen.isShiftKeyDown()) {
            for (int i = 1; i < options.allOptions.length; i++) {
                IOption<?> option = options.allOptions[i];
                tooltip.add(TextFormatting.AQUA + I18n.translateToLocal(option.getKeyTranslation())
                        + TextFormatting.GRAY + I18n.translateToLocal(option.getValueTranslation()));
            }

            if (!options.cores.getUpgrades().isEmpty()) {
                tooltip.add("");
                tooltip.add(TextFormatting.GRAY + I18n.translateToLocal(Tags.MOD_ID + ".tooltip.cores"));
                for (IWandCore core : options.cores.getUpgrades()) {
                    tooltip.add(I18n.translateToLocal(options.cores.getKeyTranslation() + "." + core.getRegistryName().toString()));
                }
            }

            // Show binding status
            if (options.cores.get() instanceof ItemCoreAE && HandlerAE.hasBinding(options)) {
                tooltip.add(TextFormatting.GREEN + I18n.translateToLocal(Tags.MOD_ID + ".tooltip.ae_bound"));
            }
            if (options.cores.get() instanceof CoreDefault && HandlerContainer.hasBinding(options)) {
                tooltip.add(TextFormatting.GREEN + I18n.translateToLocal(Tags.MOD_ID + ".tooltip.container_bound"));
            }
        } else {
            tooltip.add(TextFormatting.GRAY + String.format(I18n.translateToLocal(Tags.MOD_ID + ".tooltip.blocks"), limit));
            IOption<?> coreOption = options.allOptions[0];
            tooltip.add(TextFormatting.AQUA + I18n.translateToLocal(coreOption.getKeyTranslation())
                    + TextFormatting.WHITE + I18n.translateToLocal(coreOption.getValueTranslation()));
            tooltip.add(TextFormatting.AQUA + I18n.translateToLocal(Tags.MOD_ID + ".tooltip.shift"));
        }
    }
}
