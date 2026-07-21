package com.xinyihl.constructionwandlegacy.wand.undo;

import com.xinyihl.constructionwandlegacy.material.MaterialKey;
import com.xinyihl.constructionwandlegacy.material.MaterialReceipt;
import com.xinyihl.constructionwandlegacy.wand.WandOperation;
import com.xinyihl.constructionwandlegacy.wand.WandTransaction;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class UndoServiceTest {
    private static final Item ITEM = new Item().setRegistryName(new ResourceLocation("test", "undo"));

    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    private static UndoService service() {
        return new UndoService(LogManager.getLogger("undo-test"));
    }

    private static WandTransaction transaction(int dimension, List<String> events, FakeChange... changes) {
        WandTransaction.Builder builder = WandTransaction.builder(dimension);
        for (FakeChange change : changes) {
            int id = change.pos.getX();
            MaterialKey key = MaterialKey.of(new ItemStack(ITEM));
            MaterialReceipt receipt = MaterialReceipt.of("undo_test", key, 1, (refundKey, count) -> {
                events.add("refund-" + id);
                return 0;
            });
            builder.add(change, receipt);
        }
        return builder.build();
    }

    private static FakeChange change(int id, boolean restores, List<String> events) {
        return new FakeChange(new BlockPos(id, 0, 0), restores, events);
    }

    private static WandTransaction transactionWithRefundTarget(int dimension, List<String> events, AtomicCounter attempts) {
        WandTransaction.Builder builder = WandTransaction.builder(dimension);
        MaterialKey key = MaterialKey.of(new ItemStack(ITEM));
        MaterialReceipt receipt = MaterialReceipt.of("retry", key, 1, (refundKey, count) -> {
            attempts.value++;
            return attempts.value == 1 ? 1 : 0;
        });
        builder.add(change(9, true, events), receipt);
        return builder.build();
    }

    @Test
    public void crossDimensionUndoIsRejectedWithoutPoppingTransaction() {
        UndoService service = service();
        UUID playerId = UUID.randomUUID();
        List<String> events = new ArrayList<>();
        service.record(playerId, transaction(7, events, change(1, true, events)));

        assertFalse(service.undo(playerId, 0, null, null));
        assertEquals(Collections.emptySet(), service.peekLastPositions(playerId, 0));
        assertEquals(Collections.singleton(new BlockPos(1, 0, 0)), service.peekLastPositions(playerId, 7));

        assertTrue(service.undo(playerId, 7, null, null));
        assertEquals(Arrays.asList("restore-1", "refund-1"), events);
    }

    @Test
    public void restoresAndRefundsInReverseExecutionOrder() {
        UndoService service = service();
        UUID playerId = UUID.randomUUID();
        List<String> events = new ArrayList<>();
        service.record(playerId, transaction(3, events, change(1, true, events), change(2, true, events), change(3, true, events)));

        assertTrue(service.undo(playerId, 3, null, null));
        assertEquals(Arrays.asList("restore-3", "refund-3", "restore-2", "refund-2", "restore-1", "refund-1"), events);
    }

    @Test
    public void failedRestoreDoesNotRefundThatOperation() {
        UndoService service = service();
        UUID playerId = UUID.randomUUID();
        List<String> events = new ArrayList<>();
        service.record(playerId, transaction(2, events, change(1, true, events), change(2, false, events), change(3, true, events)));

        assertTrue(service.undo(playerId, 2, null, null));
        assertEquals(Arrays.asList("restore-3", "refund-3", "restore-2", "restore-1", "refund-1"), events);
        assertEquals(Arrays.asList(new BlockPos(1, 0, 0), new BlockPos(2, 0, 0), new BlockPos(3, 0, 0)), new ArrayList<>(service.peekLastPositions(playerId, 2)));
    }

    @Test
    public void refundRemainderKeepsTransactionAndRetriesWithoutRestoringTwice() {
        UndoService service = service();
        UUID playerId = UUID.randomUUID();
        List<String> events = new ArrayList<>();
        AtomicCounter attempts = new AtomicCounter();
        service.record(playerId, transactionWithRefundTarget(4, events, attempts));

        assertTrue(service.undo(playerId, 4, null, null));
        assertEquals(Collections.singleton(new BlockPos(9, 0, 0)), service.peekLastPositions(playerId, 4));
        assertEquals(1, attempts.value);

        assertFalse(service.undo(playerId, 4, null, null));
        assertEquals(2, attempts.value);
        assertEquals(Collections.emptySet(), service.peekLastPositions(playerId, 4));
        assertEquals(Collections.singletonList("restore-9"), events);
    }

    @Test
    public void historyRetainsOnlyTenMostRecentTransactions() {
        UndoService service = service();
        UUID playerId = UUID.randomUUID();
        List<String> events = new ArrayList<>();
        for (int id = 0; id < 11; id++) {
            service.record(playerId, transaction(1, events, change(id, true, events)));
        }

        for (int count = 0; count < 10; count++) {
            assertTrue(service.undo(playerId, 1, null, null));
        }
        assertFalse(service.undo(playerId, 1, null, null));
        assertFalse(events.contains("restore-0"));
    }

    private static final class FakeChange implements WandOperation.AppliedChange {
        private final BlockPos pos;
        private final boolean restores;
        private final List<String> events;

        private FakeChange(BlockPos pos, boolean restores, List<String> events) {
            this.pos = pos;
            this.restores = restores;
            this.events = events;
        }

        @Override
        public BlockPos getPos() {
            return pos;
        }

        @Override
        public WandOperation.RollbackResult rollback(World world) {
            return WandOperation.RollbackResult.restored();
        }

        @Override
        public WandOperation.RollbackResult restore(World world, EntityPlayer player) {
            events.add("restore-" + pos.getX());
            return restores ? WandOperation.RollbackResult.restored() : WandOperation.RollbackResult.notRestored("fake restore rejected");
        }
    }

    private static final class AtomicCounter {
        private int value;
    }
}
