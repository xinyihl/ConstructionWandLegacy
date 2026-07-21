package com.xinyihl.constructionwandlegacy.wand.action;

import com.xinyihl.constructionwandlegacy.basics.option.WandState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlaneTraversalTest {
    private static final BlockPos ORIGIN = BlockPos.ORIGIN;

    @Test
    public void noLockPreservesLegacyBreadthFirstOrderIncludingDiagonals() {
        List<BlockPos> result = PlaneTraversal.traverse(
                ORIGIN, EnumFacing.UP, WandState.Lock.NOLOCK, 9, pos -> pos);

        assertEquals(Arrays.asList(
                pos(0, 0, 0),
                pos(0, 0, -1),
                pos(0, 0, 1),
                pos(1, 0, 0),
                pos(-1, 0, 0),
                pos(1, 0, -1),
                pos(-1, 0, -1),
                pos(1, 0, 1),
                pos(-1, 0, 1)
        ), result);
    }

    @Test
    public void countLimitTruncatesStableOrderWithoutExtraEvaluation() {
        Set<BlockPos> evaluated = new HashSet<>();

        List<BlockPos> result = PlaneTraversal.traverse(
                ORIGIN, EnumFacing.EAST, WandState.Lock.NOLOCK, 3, pos -> {
                    evaluated.add(pos);
                    return pos;
                });

        assertEquals(Arrays.asList(
                pos(0, 0, 0),
                pos(0, 0, -1),
                pos(0, 0, 1)
        ), result);
        assertEquals(new HashSet<>(result), evaluated);
    }

    @Test
    public void locksSelectTheExpectedAxisForFloorAndWallFaces() {
        assertEquals(Arrays.asList(
                pos(0, 0, 0),
                pos(0, 0, -1),
                pos(0, 0, 1),
                pos(0, 0, -2),
                pos(0, 0, 2)
        ), PlaneTraversal.traverse(
                ORIGIN, EnumFacing.UP, WandState.Lock.NORTHSOUTH, 5, pos -> pos));

        assertEquals(Arrays.asList(
                pos(0, 0, 0),
                pos(0, 1, 0),
                pos(0, -1, 0),
                pos(0, 2, 0),
                pos(0, -2, 0)
        ), PlaneTraversal.traverse(
                ORIGIN, EnumFacing.NORTH, WandState.Lock.VERTICAL, 5, pos -> pos));

        assertEquals(Collections.emptyList(), PlaneTraversal.traverse(
                ORIGIN, EnumFacing.UP, WandState.Lock.VERTICAL, 5, pos -> pos));
    }

    @Test
    public void everyReachablePositionIsEvaluatedAtMostOnce() {
        Map<BlockPos, Integer> evaluationCounts = new HashMap<>();

        List<BlockPos> result = PlaneTraversal.traverse(
                ORIGIN, EnumFacing.UP, WandState.Lock.NOLOCK, 100, pos -> {
                    evaluationCounts.put(pos, evaluationCounts.getOrDefault(pos, 0) + 1);
                    return Math.abs(pos.getX()) <= 2 && Math.abs(pos.getZ()) <= 2 ? pos : null;
                });

        assertEquals(25, result.size());
        assertTrue(evaluationCounts.size() > result.size());
        for (Integer count : evaluationCounts.values()) {
            assertEquals(Integer.valueOf(1), count);
        }
    }

    @Test
    public void rejectedPositionDoesNotExpandTraversal() {
        Set<BlockPos> evaluated = new HashSet<>();

        List<BlockPos> result = PlaneTraversal.traverse(
                ORIGIN, EnumFacing.UP, WandState.Lock.NORTHSOUTH, 10, pos -> {
                    evaluated.add(pos);
                    return ORIGIN.equals(pos) ? pos : null;
                });

        assertEquals(Collections.singletonList(ORIGIN), result);
        assertEquals(new HashSet<>(Arrays.asList(
                ORIGIN,
                pos(0, 0, -1),
                pos(0, 0, 1)
        )), evaluated);
        assertFalse(evaluated.contains(pos(0, 0, -2)));
        assertFalse(evaluated.contains(pos(0, 0, 2)));
    }

    @Test
    public void nonPositiveLimitDoesNotEvaluateOrigin() {
        Set<BlockPos> evaluated = new HashSet<>();

        assertEquals(Collections.emptyList(), PlaneTraversal.traverse(
                ORIGIN, EnumFacing.UP, WandState.Lock.NOLOCK, 0, pos -> {
                    evaluated.add(pos);
                    return pos;
                }));
        assertTrue(evaluated.isEmpty());
    }

    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }
}
