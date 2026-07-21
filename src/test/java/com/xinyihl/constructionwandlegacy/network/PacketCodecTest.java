package com.xinyihl.constructionwandlegacy.network;

import com.xinyihl.constructionwandlegacy.basics.WandTarget;
import com.xinyihl.constructionwandlegacy.basics.option.WandOption;
import com.xinyihl.constructionwandlegacy.config.RuleSnapshot;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PacketCodecTest {
    @Test
    public void channelAndDiscriminatorsAreStableAndUnique() {
        assertEquals("xcwl", ModMessages.CHANNEL_NAME);
        java.util.Set<Integer> ids = new java.util.HashSet<>(Arrays.asList(
                ModMessages.ID_UNDO_BLOCKS, ModMessages.ID_QUERY_UNDO,
                ModMessages.ID_WAND_OPTION, ModMessages.ID_SERVER_RULES));
        assertEquals(4, ids.size());
        assertEquals(2, NetworkProtocol.VERSION);
    }

    @Test
    public void wandOptionRoundTripsStableIdsHandSlotAndBoundedValue() {
        for (WandOption option : WandOption.values()) {
            PacketWandOption decoded = roundTrip(
                    new PacketWandOption(option,
                            new WandTarget(EnumHand.OFF_HAND, WandTarget.OFFHAND_SLOT), 1, true),
                    new PacketWandOption());
            assertTrue(decoded.isValid());
            assertEquals(option, decoded.getOption());
            assertEquals(EnumHand.OFF_HAND, decoded.getHand());
            assertEquals(WandTarget.OFFHAND_SLOT, decoded.getSlot());
            assertEquals(1, decoded.getValue());
            assertTrue(decoded.shouldNotify());
        }
    }

    @Test
    public void malformedProtocolAndSlotsAreRejected() {
        ByteBuf wrongVersion = Unpooled.buffer();
        wrongVersion.writeByte(NetworkProtocol.VERSION + 1);
        wrongVersion.writeZero(5);
        PacketWandOption option = new PacketWandOption();
        option.fromBytes(wrongVersion);
        assertFalse(option.isValid());

        ByteBuf wrongSlot = Unpooled.buffer();
        NetworkProtocol.writeHeader(wrongSlot);
        wrongSlot.writeByte(WandOption.LOCK.getNetworkId());
        wrongSlot.writeByte(0);
        wrongSlot.writeByte(40);
        wrongSlot.writeByte(0);
        wrongSlot.writeBoolean(false);
        option.fromBytes(wrongSlot);
        assertFalse(option.isValid());
    }

    @Test
    public void undoPacketIsImmutableAndRejectsOversizedCounts() {
        LinkedHashSet<BlockPos> source = new LinkedHashSet<>(Arrays.asList(
                BlockPos.ORIGIN, new BlockPos(1, 2, 3)));
        PacketUndoBlocks decoded = roundTrip(new PacketUndoBlocks(source), new PacketUndoBlocks());
        source.clear();

        assertTrue(decoded.isValid());
        assertEquals(2, decoded.getUndoBlocks().size());
        try {
            decoded.getUndoBlocks().clear();
            org.junit.Assert.fail("undo packet positions must be immutable");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        ByteBuf oversized = Unpooled.buffer();
        NetworkProtocol.writeHeader(oversized);
        oversized.writeShort(NetworkProtocol.MAX_UNDO_BLOCKS + 1);
        PacketUndoBlocks invalid = new PacketUndoBlocks();
        invalid.fromBytes(oversized);
        assertFalse(invalid.isValid());
    }

    @Test
    public void serverRulesRoundTripAndRejectOversizedStrings() {
        RuleSnapshot rules = RuleSnapshot.create(7L, 10, 20, 30, 40, true,
                Collections.singletonList("minecraft:stone"), Collections.emptyList(),
                Collections.singletonList("facing"),
                Collections.singletonList("minecraft:stone;minecraft:cobblestone"));
        PacketServerRules decoded = roundTrip(new PacketServerRules(rules), new PacketServerRules());

        assertTrue(decoded.isValid());
        assertEquals(7L, decoded.getRules().getRevision());
        assertEquals(Collections.singletonList("minecraft:stone"),
                decoded.getRules().getPlacementWhitelist());

        ByteBuf malformed = Unpooled.buffer();
        NetworkProtocol.writeHeader(malformed);
        malformed.writeLong(1L);
        malformed.writeInt(9).writeInt(27).writeInt(81).writeInt(256);
        malformed.writeBoolean(false);
        malformed.writeShort(1);
        malformed.writeShort(RuleSnapshot.MAX_RULE_BYTES + 1);
        PacketServerRules invalid = new PacketServerRules();
        invalid.fromBytes(malformed);
        assertFalse(invalid.isValid());
    }

    private static <T extends net.minecraftforge.fml.common.network.simpleimpl.IMessage> T roundTrip(
            T encoded, T decoded) {
        ByteBuf buffer = Unpooled.buffer();
        encoded.toBytes(buffer);
        decoded.fromBytes(buffer);
        assertFalse(buffer.isReadable());
        return decoded;
    }
}
