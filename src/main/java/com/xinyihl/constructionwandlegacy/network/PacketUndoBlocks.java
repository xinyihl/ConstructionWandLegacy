package com.xinyihl.constructionwandlegacy.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.HashSet;
import java.util.Set;

public class PacketUndoBlocks implements IMessage {
    public static final Set<BlockPos> CLIENT_UNDO_BLOCKS = new HashSet<>();

    public HashSet<BlockPos> undoBlocks;

    public PacketUndoBlocks() {
        this.undoBlocks = new HashSet<>();
    }

    public PacketUndoBlocks(Set<BlockPos> undoBlocks) {
        this.undoBlocks = new HashSet<>(undoBlocks);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        undoBlocks = new HashSet<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            undoBlocks.add(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(undoBlocks.size());
        for (BlockPos pos : undoBlocks) {
            buf.writeInt(pos.getX());
            buf.writeInt(pos.getY());
            buf.writeInt(pos.getZ());
        }
    }

    public static class Handler implements IMessageHandler<PacketUndoBlocks, IMessage> {
        @Override
        public IMessage onMessage(PacketUndoBlocks message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                CLIENT_UNDO_BLOCKS.clear();
                CLIENT_UNDO_BLOCKS.addAll(message.undoBlocks);
            });
            return null;
        }
    }
}
