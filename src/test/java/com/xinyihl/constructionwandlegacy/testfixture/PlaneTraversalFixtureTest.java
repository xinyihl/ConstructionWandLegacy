package com.xinyihl.constructionwandlegacy.testfixture;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PlaneTraversalFixtureTest {
    private static final BlockPos ORIGIN = BlockPos.ORIGIN;

    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    @Test
    public void noLockPreservesLegacyBreadthFirstOrderIncludingDiagonals() {
        List<BlockPos> result = PlaneTraversalFixture.traverse(ORIGIN, EnumFacing.UP, PlaneTraversalFixture.Lock.NOLOCK, 9, ignored -> true);

        assertEquals(Arrays.asList(pos(0, 0, 0), pos(0, 0, -1), pos(0, 0, 1), pos(1, 0, 0), pos(-1, 0, 0), pos(1, 0, -1), pos(-1, 0, -1), pos(1, 0, 1), pos(-1, 0, 1)), result);
    }

    @Test
    public void northSouthLockUsesOnlyItsAxis() {
        List<BlockPos> result = PlaneTraversalFixture.traverse(ORIGIN, EnumFacing.UP, PlaneTraversalFixture.Lock.NORTHSOUTH, 7, ignored -> true);

        assertEquals(Arrays.asList(pos(0, 0, 0), pos(0, 0, -1), pos(0, 0, 1), pos(0, 0, -2), pos(0, 0, 2), pos(0, 0, -3), pos(0, 0, 3)), result);
    }

    @Test
    public void verticalLockUsesUpAndDownOnAWall() {
        List<BlockPos> result = PlaneTraversalFixture.traverse(ORIGIN, EnumFacing.NORTH, PlaneTraversalFixture.Lock.VERTICAL, 5, ignored -> true);

        assertEquals(Arrays.asList(pos(0, 0, 0), pos(0, 1, 0), pos(0, -1, 0), pos(0, 2, 0), pos(0, -2, 0)), result);
    }

    @Test
    public void irrelevantDirectionLockDoesNotStartTraversal() {
        assertEquals(Collections.emptyList(), PlaneTraversalFixture.traverse(ORIGIN, EnumFacing.UP, PlaneTraversalFixture.Lock.VERTICAL, 10, ignored -> true));
    }

    @Test
    public void countLimitTruncatesTheStableOrder() {
        List<BlockPos> result = PlaneTraversalFixture.traverse(ORIGIN, EnumFacing.EAST, PlaneTraversalFixture.Lock.NOLOCK, 3, ignored -> true);

        assertEquals(Arrays.asList(pos(0, 0, 0), pos(0, 0, -1), pos(0, 0, 1)), result);
    }

    @Test
    public void traversalDoesNotCrossANonMatchingOrigin() {
        assertEquals(Collections.emptyList(), PlaneTraversalFixture.traverse(ORIGIN, EnumFacing.UP, PlaneTraversalFixture.Lock.NOLOCK, 10, pos -> !ORIGIN.equals(pos)));
    }
}
