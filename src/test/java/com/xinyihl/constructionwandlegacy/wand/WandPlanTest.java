package com.xinyihl.constructionwandlegacy.wand;

import com.xinyihl.constructionwandlegacy.material.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class WandPlanTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void defensivelyCopiesOperationsAndExposesImmutableViews() {
        List<WandOperation> source = new ArrayList<>();
        source.add(new FakeOperation(BlockPos.ORIGIN));

        WandPlan plan = new WandPlan(source);
        source.clear();

        assertEquals(1, plan.size());
        assertEquals(Collections.singleton(BlockPos.ORIGIN), plan.getBlockPositions());
        assertThrows(UnsupportedOperationException.class, () -> plan.getBlockPositions().add(BlockPos.ORIGIN.up()));
        assertEquals(1, plan.getPreviews().size());
        plan.discard();
        plan.discard();
        assertNull(plan.claimExecution());
    }

    @Test
    public void executionCanOnlyBeClaimedOnce() {
        WandPlan plan = new WandPlan(Collections.singletonList(new FakeOperation(BlockPos.ORIGIN)));

        assertNotNull(plan.claimExecution());
        assertNull(plan.claimExecution());
    }

    @Test(expected = NoSuchMethodException.class)
    public void publicPlanApiDoesNotExposeExecutableOperations() throws Exception {
        WandPlan.class.getMethod("getOperations");
    }

    @Test
    public void discardReleasesPlanningReservationAndIsIdempotent() {
        Item item = new Item().setRegistryName(new ResourceLocation("test", "plan_discard"));
        MaterialKey key = MaterialKey.of(new ItemStack(item));
        MaterialSession session = new MaterialSession(Collections.singletonList(new OneItemSource(key)));
        MaterialReservation reservation = session.reserve(key, 1);
        FakeOperation operation = new FakeOperation(BlockPos.ORIGIN);
        Map<WandOperation, MaterialReservation> reservations = new IdentityHashMap<>();
        reservations.put(operation, reservation);
        WandPlan plan = new WandPlan(Collections.singletonList(operation), reservations);

        assertEquals(0, session.available(key));
        plan.discard();
        plan.discard();
        assertEquals(1, session.available(key));
        assertNull(plan.claimExecution());
    }

    private static final class FakeOperation implements WandOperation {
        private final BlockPos pos;

        private FakeOperation(BlockPos pos) {
            this.pos = pos;
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
            return ApplyResult.rejected("not executed in this test");
        }
    }

    private static final class OneItemSource implements MaterialSource {
        private final MaterialKey key;

        private OneItemSource(MaterialKey key) {
            this.key = key;
        }

        @Override
        public String getId() {
            return "plan_discard";
        }

        @Override
        public void enumerate(MaterialCollector collector) {
            collector.accept(key, 1);
        }

        @Override
        public com.xinyihl.constructionwandlegacy.material.MaterialReceipt extract(MaterialKey key, int count) {
            return com.xinyihl.constructionwandlegacy.material.MaterialReceipt.empty();
        }
    }
}
