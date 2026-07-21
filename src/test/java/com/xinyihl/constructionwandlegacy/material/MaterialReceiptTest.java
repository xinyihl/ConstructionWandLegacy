package com.xinyihl.constructionwandlegacy.material;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MaterialReceiptTest {
    private final MaterialKey key = MaterialKey.of(new ItemStack(
            new Item().setRegistryName(new ResourceLocation("test", "receipt")), 1));

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void refundFailuresAreIsolatedAndRetryable() {
        AtomicInteger failingCalls = new AtomicInteger();
        AtomicInteger successfulRefunds = new AtomicInteger();
        MaterialReceipt failing = MaterialReceipt.of("failing", key, 1, (ignoredKey, count) -> {
            if (failingCalls.getAndIncrement() == 0) {
                throw new IllegalStateException("transient");
            }
            return 0;
        });
        MaterialReceipt successful = MaterialReceipt.of("successful", key, 2, (ignoredKey, count) -> {
            successfulRefunds.addAndGet(count);
            return 0;
        });
        MaterialReceipt combined = MaterialReceipt.combine(Arrays.asList(failing, successful));

        assertFalse(combined.refund());
        assertEquals(2, successfulRefunds.get());
        assertEquals(1, combined.getRemainingCount());
        assertTrue(combined.refund());
        assertEquals(0, combined.getRemainingCount());
        assertFalse(combined.refund());
    }

    @Test
    public void combinedAndComponentReceiptsShareOnceOnlyEntryState() {
        AtomicInteger refunded = new AtomicInteger();
        MaterialReceipt component = MaterialReceipt.of("source", key, 3, (ignoredKey, count) -> {
            refunded.addAndGet(count);
            return 0;
        });
        MaterialReceipt combined = MaterialReceipt.combine(Arrays.asList(component, component));

        assertEquals(3, combined.getCount());
        assertTrue(combined.refund());
        assertEquals(3, refunded.get());
        assertFalse(component.refund());
        assertEquals(3, refunded.get());
    }

    @Test
    public void partialFallbackRetriesOnlyTheUndeliveredRemainder() {
        AtomicInteger requested = new AtomicInteger();
        MaterialReceipt receipt = MaterialReceipt.of("volatile", key, 4, (ignoredKey, count) -> {
            requested.addAndGet(count);
            return count == 4 ? 2 : 0;
        });

        assertFalse(receipt.refund());
        assertEquals(2, receipt.getRemainingCount());
        assertTrue(receipt.refund());
        assertEquals(6, requested.get());
    }
}
