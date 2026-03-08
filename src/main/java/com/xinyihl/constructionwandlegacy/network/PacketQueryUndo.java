package com.xinyihl.constructionwandlegacy.network;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketQueryUndo implements IMessage {
    public boolean undoPressed;

    public PacketQueryUndo() {
    }

    public PacketQueryUndo(boolean undoPressed) {
        this.undoPressed = undoPressed;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        undoPressed = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(undoPressed);
    }

    public static class Handler implements IMessageHandler<PacketQueryUndo, IMessage> {
        @Override
        public IMessage onMessage(PacketQueryUndo message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> ConstructionWandLegacy.instance.undoHistory.updateClient(player, message.undoPressed));
            return null;
        }
    }
}
