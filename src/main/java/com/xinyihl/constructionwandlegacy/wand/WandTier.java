package com.xinyihl.constructionwandlegacy.wand;

import com.xinyihl.constructionwandlegacy.config.ModConfig;

public enum WandTier {
    STONE(new WandSpec(9, 131, 16, 9, 1024)), IRON(new WandSpec(27, 250, 32, 27, 1024)), DIAMOND(new WandSpec(81, 1561, 64, 81, 1024)), INFINITY(new WandSpec(256, Integer.MAX_VALUE, 128, 256, 1024));

    private final WandSpec spec;

    WandTier(WandSpec spec) {
        this.spec = spec;
    }

    public WandSpec getSpec() {
        return spec;
    }

    public int getConfiguredPlacementLimit() {
        switch (this) {
            case STONE:
                return ModConfig.wandLimits.stoneWandMaxBlocks;
            case IRON:
                return ModConfig.wandLimits.ironWandMaxBlocks;
            case DIAMOND:
                return ModConfig.wandLimits.diamondWandMaxBlocks;
            case INFINITY:
                return ModConfig.wandLimits.infinityWandMaxBlocks;
            default:
                return spec.getBasePlacementLimit();
        }
    }
}
