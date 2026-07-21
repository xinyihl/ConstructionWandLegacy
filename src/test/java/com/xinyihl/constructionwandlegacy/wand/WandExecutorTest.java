package com.xinyihl.constructionwandlegacy.wand;

import com.xinyihl.constructionwandlegacy.material.MaterialCollector;
import com.xinyihl.constructionwandlegacy.material.MaterialKey;
import com.xinyihl.constructionwandlegacy.material.MaterialReceipt;
import com.xinyihl.constructionwandlegacy.material.MaterialReservation;
import com.xinyihl.constructionwandlegacy.material.MaterialSession;
import com.xinyihl.constructionwandlegacy.material.MaterialSource;
import com.xinyihl.constructionwandlegacy.wand.undo.UndoService;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class WandExecutorTest {
    private static final Item ITEM = new Item().setRegistryName(new ResourceLocation("test", "executor"));

    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void bestEffortExecutionRecordsOnlySuccessfulOperations() {
        FakeChange first = new FakeChange(BlockPos.ORIGIN);
        FakeChange third = new FakeChange(BlockPos.ORIGIN.east());
        WandPlan plan = plan(
                FakeOperation.applied(first, null),
                FakeOperation.rejected(BlockPos.ORIGIN.up()),
                FakeOperation.applied(third, null));
        TestAccess access = new TestAccess(true);

        WandExecutor.ExecutionResult result = executor().execute(null, plan, access);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getSucceededOperations());
        assertNotNull(access.recorded);
        assertEquals(2, access.recorded.getPositions().size());
        assertTrue(access.recorded.getPositions().contains(BlockPos.ORIGIN));
        assertTrue(access.recorded.getPositions().contains(BlockPos.ORIGIN.east()));
        assertEquals(0, first.rollbacks);
        assertEquals(0, third.rollbacks);
    }

    @Test
    public void materialCommitFailureRestoresReplaceableOriginalAndContinues() {
        MaterialKey key = MaterialKey.of(new ItemStack(ITEM));
        MaterialSession session = new MaterialSession(Collections.singletonList(
                new TestSource(key, false, new AtomicInteger())));
        MaterialReservation reservation = session.reserve(key, 1);
        AtomicReference<String> worldState = new AtomicReference<>("placed_block");
        FakeChange failedChange = new FakeChange(BlockPos.ORIGIN, () -> {
            worldState.set("replaceable_before");
            return true;
        });
        FakeChange successfulChange = new FakeChange(BlockPos.ORIGIN.east());
        WandPlan plan = plan(
                FakeOperation.applied(failedChange, reservation),
                FakeOperation.applied(successfulChange, null));
        TestAccess access = new TestAccess(true);

        WandExecutor.ExecutionResult result = executor().execute(null, plan, access);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getSucceededOperations());
        assertEquals("replaceable_before", worldState.get());
        assertEquals(1, failedChange.rollbacks);
        assertEquals(Collections.singleton(BlockPos.ORIGIN.east()), access.recorded.getPositions());
    }

    @Test
    public void damageFailureRefundsCommittedMaterialAndRollsBackWorld() {
        MaterialKey key = MaterialKey.of(new ItemStack(ITEM));
        AtomicInteger refunds = new AtomicInteger();
        MaterialSession session = new MaterialSession(Collections.singletonList(
                new TestSource(key, true, refunds)));
        FakeChange change = new FakeChange(BlockPos.ORIGIN);
        WandPlan plan = plan(FakeOperation.applied(change, session.reserve(key, 1)));
        TestAccess access = new TestAccess(false);
        access.failDamage = true;

        WandExecutor.ExecutionResult result = executor().execute(null, plan, access);

        assertFalse(result.isSuccess());
        assertEquals(1, refunds.get());
        assertEquals(1, change.rollbacks);
        assertEquals(null, access.recorded);
    }

    @Test
    public void rollbackFailureDoesNotRefundAndCreatesPendingRecovery() {
        MaterialKey key = MaterialKey.of(new ItemStack(ITEM));
        AtomicInteger refunds = new AtomicInteger();
        MaterialSession session = new MaterialSession(Collections.singletonList(
                new TestSource(key, true, refunds)));
        FakeChange change = new FakeChange(BlockPos.ORIGIN, () -> false);
        WandPlan plan = plan(FakeOperation.applied(change, session.reserve(key, 1)));
        TestAccess access = new TestAccess(false);
        access.failDamage = true;

        WandExecutor.ExecutionResult result = executor().execute(null, plan, access);

        assertFalse(result.isSuccess());
        assertEquals(0, refunds.get());
        assertEquals(1, access.pendingCount);
    }

    @Test
    public void rollbackExceptionDoesNotRefundAndCreatesPendingRecovery() {
        MaterialKey key = MaterialKey.of(new ItemStack(ITEM));
        AtomicInteger refunds = new AtomicInteger();
        MaterialSession session = new MaterialSession(Collections.singletonList(
                new TestSource(key, true, refunds)));
        FakeChange change = new FakeChange(BlockPos.ORIGIN, () -> {
            throw new IllegalStateException("rollback exploded");
        });
        WandPlan plan = plan(FakeOperation.applied(change, session.reserve(key, 1)));
        TestAccess access = new TestAccess(false);
        access.failDamage = true;

        WandExecutor.ExecutionResult result = executor().execute(null, plan, access);

        assertFalse(result.isSuccess());
        assertEquals(0, refunds.get());
        assertEquals(1, access.pendingCount);
    }

    private static WandExecutor executor() {
        return new WandExecutor(new UndoService(LogManager.getLogger("executor-test")),
                LogManager.getLogger("executor-test"));
    }

    private static WandPlan plan(FakeOperation... operations) {
        Map<WandOperation, MaterialReservation> reservations = new IdentityHashMap<>();
        for (FakeOperation operation : operations) {
            if (operation.reservation != null) {
                reservations.put(operation, operation.reservation);
            }
        }
        return new WandPlan(Arrays.asList(operations), reservations);
    }

    private static final class TestAccess implements WandExecutor.ExecutionAccess {
        private final boolean creative;
        private boolean failDamage;
        private int pendingCount;
        private WandTransaction recorded;

        private TestAccess(boolean creative) {
            this.creative = creative;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public boolean canContinue() {
            return true;
        }

        @Override
        public boolean isCreative() {
            return creative;
        }

        @Override
        public void damageWand() {
            if (failDamage) {
                throw new IllegalStateException("damage failed");
            }
        }

        @Override
        public int getDimension() {
            return 7;
        }

        @Override
        public void record(WandTransaction transaction) {
            recorded = transaction;
        }

        @Override
        public WandOperation.RollbackResult rollback(WandOperation.AppliedChange change) {
            return change.rollback(null);
        }

        @Override
        public void recordPending(WandOperation.AppliedChange change, @Nullable MaterialReceipt receipt,
                                  boolean worldRestored) {
            pendingCount++;
        }
    }

    private static final class FakeOperation implements WandOperation {
        private final BlockPos pos;
        private final ApplyResult result;
        @Nullable
        private final MaterialReservation reservation;

        private FakeOperation(BlockPos pos, ApplyResult result, @Nullable MaterialReservation reservation) {
            this.pos = pos;
            this.result = result;
            this.reservation = reservation;
        }

        private static FakeOperation applied(FakeChange change, @Nullable MaterialReservation reservation) {
            return new FakeOperation(change.getPos(), ApplyResult.applied(change), reservation);
        }

        private static FakeOperation rejected(BlockPos pos) {
            return new FakeOperation(pos, ApplyResult.rejected("rejected"), null);
        }

        @Override
        public BlockPos getPos() {
            return pos;
        }

        @Override
        public IBlockState getPreviewState() {
            return Blocks.AIR.getDefaultState();
        }

        @Override
        public ApplyResult apply(WandContext context, WandPlan.ExecutionToken token) {
            return result;
        }

        private MaterialReservation reservation() {
            return reservation;
        }
    }

    private static final class FakeChange implements WandOperation.AppliedChange {
        private final BlockPos pos;
        private final Rollback rollback;
        private int rollbacks;

        private FakeChange(BlockPos pos) {
            this(pos, () -> true);
        }

        private FakeChange(BlockPos pos, Rollback rollback) {
            this.pos = pos;
            this.rollback = rollback;
        }

        @Override
        public BlockPos getPos() {
            return pos;
        }

        @Override
        public WandOperation.RollbackResult rollback(World world) {
            rollbacks++;
            return rollback.run()
                    ? WandOperation.RollbackResult.restored()
                    : WandOperation.RollbackResult.notRestored("fake rollback rejected");
        }

        @Override
        public WandOperation.RollbackResult restore(World world, EntityPlayer player) {
            return WandOperation.RollbackResult.restored();
        }
    }

    private interface Rollback {
        boolean run();
    }

    private static final class TestSource implements MaterialSource {
        private final MaterialKey key;
        private final boolean extractionSucceeds;
        private final AtomicInteger refunds;

        private TestSource(MaterialKey key, boolean extractionSucceeds, AtomicInteger refunds) {
            this.key = key;
            this.extractionSucceeds = extractionSucceeds;
            this.refunds = refunds;
        }

        @Override
        public String getId() {
            return "executor_test";
        }

        @Override
        public void enumerate(MaterialCollector collector) {
            collector.accept(key, 1);
        }

        @Override
        public MaterialReceipt extract(MaterialKey requested, int count) {
            if (!extractionSucceeds) {
                return MaterialReceipt.empty();
            }
            return MaterialReceipt.of(getId(), key, count, (refundKey, refundCount) -> {
                refunds.addAndGet(refundCount);
                return 0;
            });
        }
    }
}
