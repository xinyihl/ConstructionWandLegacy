package com.xinyihl.constructionwandlegacy.testfixture;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.function.Predicate;

/**
 * Reference implementation of the legacy construction/destruction plane walk.
 * It intentionally preserves the legacy queue order for use by later refactors.
 */
public final class PlaneTraversalFixture {
    private PlaneTraversalFixture() {
    }

    public static List<BlockPos> traverse(BlockPos origin, EnumFacing face, Lock lock, int limit, Predicate<BlockPos> isMatching) {
        LinkedList<BlockPos> candidates = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> result = new ArrayList<>();

        Axes axes = axesFor(face, lock);
        if (axes.useFirst || axes.useSecond) {
            candidates.add(origin);
        }

        while (!candidates.isEmpty() && result.size() < limit) {
            BlockPos current = candidates.removeFirst();
            if (!isMatching.test(current) || !visited.add(current)) {
                continue;
            }

            result.add(current);
            addPlaneCandidates(candidates, current, axes);
        }
        return result;
    }

    private static Axes axesFor(EnumFacing face, Lock lock) {
        switch (face) {
            case DOWN:
            case UP:
                return new Axes(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST, permits(lock, Lock.NORTHSOUTH), permits(lock, Lock.EASTWEST));
            case NORTH:
            case SOUTH:
                return new Axes(EnumFacing.EAST, EnumFacing.WEST, EnumFacing.UP, EnumFacing.DOWN, permits(lock, Lock.HORIZONTAL), permits(lock, Lock.VERTICAL));
            case EAST:
            case WEST:
                return new Axes(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP, EnumFacing.DOWN, permits(lock, Lock.HORIZONTAL), permits(lock, Lock.VERTICAL));
            default:
                throw new IllegalArgumentException("Unsupported face: " + face);
        }
    }

    private static boolean permits(Lock actual, Lock requested) {
        return actual == Lock.NOLOCK || actual == requested;
    }

    private static void addPlaneCandidates(LinkedList<BlockPos> candidates, BlockPos origin, Axes axes) {
        if (axes.useFirst) {
            candidates.add(origin.offset(axes.firstA));
            candidates.add(origin.offset(axes.firstB));
        }
        if (axes.useSecond) {
            candidates.add(origin.offset(axes.secondA));
            candidates.add(origin.offset(axes.secondB));
        }
        if (axes.useFirst && axes.useSecond) {
            candidates.add(origin.offset(axes.firstA).offset(axes.secondA));
            candidates.add(origin.offset(axes.firstA).offset(axes.secondB));
            candidates.add(origin.offset(axes.firstB).offset(axes.secondA));
            candidates.add(origin.offset(axes.firstB).offset(axes.secondB));
        }
    }

    public enum Lock {
        HORIZONTAL, VERTICAL, NORTHSOUTH, EASTWEST, NOLOCK
    }

    private static final class Axes {
        private final EnumFacing firstA;
        private final EnumFacing firstB;
        private final EnumFacing secondA;
        private final EnumFacing secondB;
        private final boolean useFirst;
        private final boolean useSecond;

        private Axes(EnumFacing firstA, EnumFacing firstB, EnumFacing secondA, EnumFacing secondB, boolean useFirst, boolean useSecond) {
            this.firstA = firstA;
            this.firstB = firstB;
            this.secondA = secondA;
            this.secondB = secondB;
            this.useFirst = useFirst;
            this.useSecond = useSecond;
        }
    }
}
