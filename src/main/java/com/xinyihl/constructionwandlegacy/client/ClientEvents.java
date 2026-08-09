package com.xinyihl.constructionwandlegacy.client;

import com.xinyihl.constructionwandlegacy.basics.WandTarget;
import com.xinyihl.constructionwandlegacy.basics.option.WandDataCodec;
import com.xinyihl.constructionwandlegacy.basics.option.WandOption;
import com.xinyihl.constructionwandlegacy.basics.option.WandState;
import com.xinyihl.constructionwandlegacy.network.ModMessages;
import com.xinyihl.constructionwandlegacy.network.PacketQueryUndo;
import com.xinyihl.constructionwandlegacy.network.PacketWandOption;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class ClientEvents {
    private final ClientPreviewController previewController;
    private boolean lastUndoPressed;
    private boolean forceUndoRefresh;

    public ClientEvents(ClientPreviewController previewController) {
        this.previewController = previewController;
    }

    public static boolean isOptKeyDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    public static boolean modeKeyCombDown(EntityPlayer player) {
        return player.isSneaking() && isOptKeyDown();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            lastUndoPressed = false;
            previewController.resetPreview();
            return;
        }

        boolean undoPressed = WandTarget.locate(player) != null && isOptKeyDown();
        if (forceUndoRefresh || undoPressed != lastUndoPressed) {
            ModMessages.sendToServer(new PacketQueryUndo(undoPressed));
            lastUndoPressed = undoPressed;
            forceUndoRefresh = false;
        }
        previewController.tick(Minecraft.getMinecraft(), modeKeyCombDown(player));
    }

    @SubscribeEvent
    public void onMouseScroll(MouseEvent event) {
        int wheel = event.getDwheel();
        if (wheel == 0) {
            return;
        }

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null || !modeKeyCombDown(player)) {
            return;
        }

        WandTarget target = WandTarget.locate(player);
        if (target == null) {
            return;
        }
        ItemStack preview = target.resolve(player).copy();
        if (!WandDataCodec.cycle(preview, WandOption.LOCK, wheel < 0)) {
            return;
        }
        WandState state = WandDataCodec.read(preview);
        ModMessages.sendToServer(new PacketWandOption(WandOption.LOCK, target, WandDataCodec.getNetworkValue(state, WandOption.LOCK), true));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || !modeKeyCombDown(player)) {
            return;
        }

        WandTarget target = WandTarget.forHand(player, event.getHand());
        if (target == null) {
            return;
        }
        ItemStack preview = target.resolve(player).copy();
        if (!WandDataCodec.cycle(preview, WandOption.CORES, true)) {
            return;
        }
        WandState state = WandDataCodec.read(preview);
        ModMessages.sendToServer(new PacketWandOption(WandOption.CORES, target, WandDataCodec.getNetworkValue(state, WandOption.CORES), true));
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || !modeKeyCombDown(player)) {
            return;
        }

        WandTarget target = WandTarget.forHand(player, event.getHand());
        if (target == null) {
            return;
        }

        if (player.world.isRemote) {
            Minecraft mc = Minecraft.getMinecraft();
            RayTraceResult mouseOver = mc.objectMouseOver;
            if (mouseOver != null && mouseOver.typeOfHit != RayTraceResult.Type.MISS) {
                return;
            }

            mc.displayGuiScreen(new GuiWand(target, target.resolve(player)));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            forceUndoRefresh = true;
        }
    }
}
