package com.xinyihl.constructionwandlegacy.network;

import com.xinyihl.constructionwandlegacy.Tags;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class ModMessages {
    private static SimpleNetworkWrapper INSTANCE;

    private ModMessages() {
    }

    public static void register() {
        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);
        int packetIndex = 0;

        INSTANCE.registerMessage(PacketUndoBlocks.Handler.class, PacketUndoBlocks.class, packetIndex++, Side.CLIENT);
        INSTANCE.registerMessage(PacketQueryUndo.Handler.class, PacketQueryUndo.class, packetIndex++, Side.SERVER);
        INSTANCE.registerMessage(PacketWandOption.Handler.class, PacketWandOption.class, packetIndex, Side.SERVER);
    }

    public static <MSG extends IMessage> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG extends IMessage> void sendToPlayer(MSG message, net.minecraft.entity.player.EntityPlayerMP player) {
        INSTANCE.sendTo(message, player);
    }
}
