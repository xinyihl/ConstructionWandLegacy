package com.xinyihl.constructionwandlegacy.wand;

/**
 * Immutable intrinsic properties of a construction wand tier.
 */
public final class WandSpec {
    private final int basePlacementLimit;
    private final int durability;
    private final int angelRange;
    private final int destructionLimit;
    private final int creativePlacementLimit;

    public WandSpec(int basePlacementLimit, int durability, int angelRange, int destructionLimit,
                    int creativePlacementLimit) {
        if (basePlacementLimit < 1) {
            throw new IllegalArgumentException("basePlacementLimit must be positive");
        }
        if (durability < 1) {
            throw new IllegalArgumentException("durability must be positive");
        }
        if (angelRange < 0) {
            throw new IllegalArgumentException("angelRange must not be negative");
        }
        if (destructionLimit < 0) {
            throw new IllegalArgumentException("destructionLimit must not be negative");
        }
        if (creativePlacementLimit < 1) {
            throw new IllegalArgumentException("creativePlacementLimit must be positive");
        }

        this.basePlacementLimit = basePlacementLimit;
        this.durability = durability;
        this.angelRange = angelRange;
        this.destructionLimit = destructionLimit;
        this.creativePlacementLimit = creativePlacementLimit;
    }

    public int getBasePlacementLimit() {
        return basePlacementLimit;
    }

    public int getDurability() {
        return durability;
    }

    public int getAngelRange() {
        return angelRange;
    }

    public int getDestructionLimit() {
        return destructionLimit;
    }

    public int getCreativePlacementLimit() {
        return creativePlacementLimit;
    }
}
