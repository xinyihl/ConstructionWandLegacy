package com.xinyihl.constructionwandlegacy.client;

import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.items.wand.ItemWand;
import com.xinyihl.constructionwandlegacy.network.ModMessages;
import com.xinyihl.constructionwandlegacy.network.PacketQueryUndo;
import com.xinyihl.constructionwandlegacy.network.PacketWandOption;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

public class ClientEvents {
    private boolean lastUndoPressed;

    public static boolean isOptKeyDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }

    private static boolean modeKeyCombDown(EntityPlayer player) {
        return player.isSneaking() && isOptKeyDown();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }

        boolean undoPressed = !WandUtil.holdingWand(player).isEmpty() && isOptKeyDown();
        if (undoPressed != lastUndoPressed) {
            ModMessages.sendToServer(new PacketQueryUndo(undoPressed));
            lastUndoPressed = undoPressed;
        }
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

        ItemStack wand = WandUtil.holdingWand(player);
        if (wand.isEmpty() || !(wand.getItem() instanceof ItemWand)) {
            return;
        }

        WandOptions options = new WandOptions(wand);
        options.lock.next(wheel < 0);
        ModMessages.sendToServer(new PacketWandOption(options.lock, true));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || !modeKeyCombDown(player)) {
            return;
        }

        ItemStack wand = WandUtil.holdingWand(player);
        if (wand.isEmpty() || !(wand.getItem() instanceof ItemWand)) {
            return;
        }

        WandOptions options = new WandOptions(wand);
        options.cores.next(true);
        ModMessages.sendToServer(new PacketWandOption(options.cores, true));
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || !modeKeyCombDown(player)) {
            return;
        }

        ItemStack wand = WandUtil.holdingWand(player);
        if (wand.isEmpty() || !(wand.getItem() instanceof ItemWand)) {
            return;
        }

        if (player.world.isRemote) {
            Minecraft mc = Minecraft.getMinecraft();
            RayTraceResult mouseOver = mc.objectMouseOver;
            if (mouseOver != null && mouseOver.typeOfHit != RayTraceResult.Type.MISS) {
                return;
            }

            mc.displayGuiScreen(new GuiWand(wand));
            event.setCanceled(true);
        }
    }
}
