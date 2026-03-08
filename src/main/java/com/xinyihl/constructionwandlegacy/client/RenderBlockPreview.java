package com.xinyihl.constructionwandlegacy.client;

import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.items.wand.ItemWand;
import com.xinyihl.constructionwandlegacy.network.PacketUndoBlocks;
import com.xinyihl.constructionwandlegacy.wand.WandJob;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RenderBlockPreview {
    private static final float DEFAULT_R = 0.0F;
    private static final float DEFAULT_G = 0.0F;
    private static final float DEFAULT_B = 0.0F;

    private WandJob wandJob;
    private RayTraceResult lastHit;
    private ItemStack lastWand = ItemStack.EMPTY;

    private static boolean sameTarget(RayTraceResult first, RayTraceResult second) {
        if (first == null || second == null || first.typeOfHit != RayTraceResult.Type.BLOCK || second.typeOfHit != RayTraceResult.Type.BLOCK) {
            return false;
        }
        return first.getBlockPos().equals(second.getBlockPos()) && first.sideHit == second.sideHit;
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onDrawBlockHighlight(DrawBlockHighlightEvent event) {
        RayTraceResult target = event.getTarget();
        EntityPlayer player = event.getPlayer();
        if (target == null || player == null || target.typeOfHit != RayTraceResult.Type.BLOCK) {
            reset();
            return;
        }

        ItemStack wand = WandUtil.holdingWand(player);
        if (wand.isEmpty() || !(wand.getItem() instanceof ItemWand)) {
            reset();
            return;
        }

        Set<BlockPos> blocks;
        float colorR = DEFAULT_R;
        float colorG = DEFAULT_G;
        float colorB = DEFAULT_B;

        if (player.isSneaking() && ClientEvents.isOptKeyDown()) {
            blocks = new HashSet<>(PacketUndoBlocks.CLIENT_UNDO_BLOCKS);
            colorG = 1.0F;
        } else {
            if (wandJob == null || !sameTarget(lastHit, target) || !ItemStack.areItemStacksEqual(lastWand, wand)
                    || wandJob.blockCount() < 2) {
                wandJob = new WandJob(player, player.world, target, wand);
                wandJob.getSnapshots();
                lastHit = target;
                lastWand = wand.copy();
            }
            blocks = wandJob != null ? wandJob.getBlockPositions() : Collections.emptySet();
            float[] coreColor = getCorePreviewColor(wand);
            colorR = coreColor[0];
            colorG = coreColor[1];
            colorB = coreColor[2];
        }

        if (blocks.isEmpty()) {
            return;
        }

        renderBoxes(player, blocks, colorR, colorG, colorB, event.getPartialTicks());

        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null || player.isSneaking() && ClientEvents.isOptKeyDown()) {
            return;
        }

        ItemStack wand = WandUtil.holdingWand(player);
        if (wand.isEmpty() || !(wand.getItem() instanceof ItemWand)) {
            return;
        }

        RayTraceResult target = mc.objectMouseOver;
        if (target == null || target.typeOfHit != RayTraceResult.Type.MISS) {
            return;
        }

        RayTraceResult miss = new RayTraceResult(RayTraceResult.Type.MISS,
                player.getPositionEyes(1.0F),
                EnumFacing.getFacingFromVector((float) player.getLookVec().x, (float) player.getLookVec().y, (float) player.getLookVec().z),
                player.getPosition());

        WandJob airJob = new WandJob(player, player.world, miss, wand);
        airJob.getSnapshots();
        Set<BlockPos> blocks = airJob.getBlockPositions();
        if (blocks.isEmpty()) {
            return;
        }

        float[] coreColor = getCorePreviewColor(wand);
        renderBoxes(player, blocks, coreColor[0], coreColor[1], coreColor[2], event.getPartialTicks());
    }

    private void renderBoxes(EntityPlayer player, Set<BlockPos> blocks, float colorR, float colorG, float colorB, float partialTicks) {
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        for (BlockPos blockPos : blocks) {
            AxisAlignedBB box = new AxisAlignedBB(blockPos).grow(0.002D).offset(-px, -py, -pz);
            RenderGlobal.drawSelectionBoundingBox(box, colorR, colorG, colorB, 0.4F);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private float[] getCorePreviewColor(ItemStack wand) {
        int color = new WandOptions(wand).cores.get().getColor();
        if (color < 0) {
            return new float[]{DEFAULT_R, DEFAULT_G, DEFAULT_B};
        }
        return new float[]{
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F
        };
    }

    public void reset() {
        wandJob = null;
        lastHit = null;
        lastWand = ItemStack.EMPTY;
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            reset();
            PacketUndoBlocks.CLIENT_UNDO_BLOCKS.clear();
        }
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        reset();
        PacketUndoBlocks.CLIENT_UNDO_BLOCKS.clear();
    }
}