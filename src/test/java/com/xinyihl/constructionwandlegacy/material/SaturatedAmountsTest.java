package com.xinyihl.constructionwandlegacy.material;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SaturatedAmountsTest {
    @Test
    public void longAmountsRejectZeroAndSaturateOverflow() {
        assertEquals(0, SaturatedAmounts.fromLong(0L));
        assertEquals(0, SaturatedAmounts.fromLong(-1L));
        assertEquals(7, SaturatedAmounts.fromLong(7L));
        assertEquals(Integer.MAX_VALUE, SaturatedAmounts.fromLong(Long.MAX_VALUE));
    }

    @Test
    public void additionsSaturate() {
        assertEquals(Integer.MAX_VALUE, SaturatedAmounts.add(Integer.MAX_VALUE - 1, 20));
        assertEquals(Long.MAX_VALUE, SaturatedAmounts.add(Long.MAX_VALUE - 1, 20L));
    }
}
