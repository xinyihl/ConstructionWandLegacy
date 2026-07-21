package com.xinyihl.constructionwandlegacy.material;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MaterialReservationFailureTest {
    private final MaterialKey key = MaterialKey.of(new ItemStack(
            new Item().setRegistryName(new ResourceLocation("test", "reservation_failure")), 1));

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void extractExceptionRollsBackOtherSourcesAndAlwaysReleasesReservation() {
        RecordingSource retrying = new RecordingSource("retrying", key, 1, true, false);
        RecordingSource normal = new RecordingSource("normal", key, 1, false, false);
        RecordingSource throwing = new RecordingSource("throwing", key, 1, false, true);
        MaterialSession session = new MaterialSession(Arrays.asList(retrying, normal, throwing));
        MaterialReservation reservation = session.reserve(key, 3);

        assertNotNull(reservation);
        assertNull(reservation.commit());
        assertEquals(3, session.available(key));
        assertEquals(1, normal.actual);
        assertEquals(0, retrying.actual);

        MaterialReceipt rollback = reservation.getRollbackReceipt();
        assertNotNull(rollback);
        assertEquals(1, rollback.getRemainingCount());
        assertTrue(reservation.retryRollback());
        assertEquals(1, retrying.actual);
        assertFalse(reservation.retryRollback());
    }

    private static final class RecordingSource implements MaterialSource {
        private final String id;
        private final MaterialKey key;
        private final int enumerated;
        private final boolean failFirstRefund;
        private final boolean throwOnExtract;
        private int actual;
        private boolean refundFailed;

        private RecordingSource(String id, MaterialKey key, int count,
                                boolean failFirstRefund, boolean throwOnExtract) {
            this.id = id;
            this.key = key;
            this.enumerated = count;
            this.actual = count;
            this.failFirstRefund = failFirstRefund;
            this.throwOnExtract = throwOnExtract;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void enumerate(MaterialCollector collector) {
            collector.accept(key, enumerated);
        }

        @Override
        public MaterialReceipt extract(MaterialKey requestedKey, int count) {
            if (throwOnExtract) {
                throw new IllegalStateException("external source failure");
            }
            int extracted = Math.min(actual, count);
            actual -= extracted;
            return MaterialReceipt.of(id, requestedKey, extracted, (refundKey, refundCount) -> {
                if (failFirstRefund && !refundFailed) {
                    refundFailed = true;
                    throw new IllegalStateException("transient refund failure");
                }
                actual += refundCount;
                return 0;
            });
        }
    }
}
