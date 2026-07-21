package com.xinyihl.constructionwandlegacy.client;

import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class PreviewCacheTest {
    private static PreviewKey key(long worldTick, double playerX, double lookX, String state) {
        return PreviewKey.forTest(PreviewKey.Mode.AIR, worldTick, playerX, lookX, BlockPos.ORIGIN.toLong(), state, 1L);
    }

    private static PreviewSnapshot snapshot(PreviewKey key, AtomicInteger calls) {
        calls.incrementAndGet();
        return PreviewSnapshot.create(key, Collections.singleton(BlockPos.ORIGIN), PreviewSnapshot.PreviewColor.BLACK);
    }

    @Test
    public void plansAtMostOncePerClientTick() {
        PreviewCache cache = new PreviewCache();
        AtomicInteger calls = new AtomicInteger();
        PreviewKey first = key(1L, 1.0D, 1.0D, "first");
        PreviewKey changed = key(1L, 2.0D, 1.0D, "second");

        PreviewSnapshot initial = cache.update(10L, first, () -> snapshot(first, calls));
        PreviewSnapshot sameTick = cache.update(10L, changed, () -> snapshot(changed, calls));

        assertSame(initial, sameTick);
        assertEquals(1, calls.get());
    }

    @Test
    public void singleBlockAndEmptyPlansAreValidCacheEntries() {
        PreviewCache cache = new PreviewCache();
        AtomicInteger calls = new AtomicInteger();
        PreviewKey singleKey = key(1L, 1.0D, 1.0D, "single");
        PreviewSnapshot single = cache.update(1L, singleKey, () -> snapshot(singleKey, calls));

        assertSame(single, cache.update(2L, singleKey, () -> snapshot(singleKey, calls)));
        assertEquals(1, calls.get());

        PreviewKey emptyKey = key(2L, 1.0D, 1.0D, "empty");
        PreviewSnapshot empty = cache.update(3L, emptyKey, () -> {
            calls.incrementAndGet();
            return PreviewSnapshot.create(emptyKey, Collections.emptySet(), PreviewSnapshot.PreviewColor.BLACK);
        });
        assertSame(empty, cache.update(4L, emptyKey, () -> snapshot(emptyKey, calls)));
        assertEquals(2, calls.get());
    }

    @Test
    public void airInputsAndWorldTickParticipateInTheKey() {
        PreviewKey base = key(5L, 1.0D, 1.0D, "state");
        org.junit.Assert.assertNotEquals(base, key(6L, 1.0D, 1.0D, "state"));
        org.junit.Assert.assertNotEquals(base, key(5L, 2.0D, 1.0D, "state"));
        org.junit.Assert.assertNotEquals(base, key(5L, 1.0D, 2.0D, "state"));
        org.junit.Assert.assertNotEquals(base, key(5L, 1.0D, 1.0D, "other"));
    }
}
