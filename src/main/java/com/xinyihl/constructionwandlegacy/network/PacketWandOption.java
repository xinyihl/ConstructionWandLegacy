package com.xinyihl.constructionwandlegacy.network;

import com.xinyihl.constructionwandlegacy.basics.WandUtil;
import com.xinyihl.constructionwandlegacy.basics.option.IOption;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.items.wand.ItemWand;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketWandOption implements IMessage {
    public String key;
    public String value;
    public boolean notify;

    public PacketWandOption() {
    }

    public PacketWandOption(IOption<?> option, boolean notify) {
        this(option.getKey(), option.getValueString(), notify);
    }

    public PacketWandOption(String key, String value, boolean notify) {
        this.key = key;
        this.value = value;
        this.notify = notify;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        key = ByteBufUtils.readUTF8String(buf);
        value = ByteBufUtils.readUTF8String(buf);
        notify = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, key);
        ByteBufUtils.writeUTF8String(buf, value);
        buf.writeBoolean(notify);
    }

    public static class Handler implements IMessageHandler<PacketWandOption, IMessage> {
        @Override
        public IMessage onMessage(PacketWandOption message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack wand = WandUtil.holdingWand(player);
                if (wand.isEmpty()) {
                    return;
                }

                WandOptions options = new WandOptions(wand);
                IOption<?> option = options.get(message.key);
                if (option == null) {
                    return;
                }

                option.setValueString(message.value);
                if (message.notify) {
                    ItemWand.optionMessage(player, option);
                }
                player.inventory.markDirty();
            });
            return null;
        }
    }
}
