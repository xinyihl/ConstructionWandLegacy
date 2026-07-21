package com.xinyihl.constructionwandlegacy.config;

import com.xinyihl.constructionwandlegacy.wand.WandTier;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class RuleSnapshotTest {
    private static String repeat(char value, int count) {
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, value);
        return new String(chars);
    }

    @Test
    public void copiesRulesAndExposesTierLimits() {
        ArrayList<String> whitelist = new ArrayList<>(Collections.singletonList("minecraft:stone"));
        RuleSnapshot snapshot = RuleSnapshot.create(4L, 1, 2, 3, 4, false, whitelist, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        whitelist.clear();

        assertEquals(Collections.singletonList("minecraft:stone"), snapshot.getPlacementWhitelist());
        assertEquals(1, snapshot.getPlacementLimit(WandTier.STONE));
        assertEquals(4, snapshot.getPlacementLimit(WandTier.INFINITY));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOutOfRangePlacementLimit() {
        RuleSnapshot.create(1L, 0, 2, 3, 4, false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOversizedRuleValue() {
        RuleSnapshot.create(1L, 1, 2, 3, 4, false, Collections.singletonList(repeat('x', RuleSnapshot.MAX_RULE_BYTES + 1)), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }
}
