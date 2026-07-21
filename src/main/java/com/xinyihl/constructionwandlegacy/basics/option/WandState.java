package com.xinyihl.constructionwandlegacy.basics.option;

import com.xinyihl.constructionwandlegacy.config.ConfigRuntime;
import com.xinyihl.constructionwandlegacy.wand.upgrade.IWandCore;
import com.xinyihl.constructionwandlegacy.wand.upgrade.IWandUpgrade;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class WandState {
    private final List<IWandCore> cores;
    private final int selectedCoreIndex;
    private final Lock lock;
    private final Direction direction;
    private final boolean replace;
    private final Match match;
    private final boolean random;

    WandState(List<IWandCore> cores, int selectedCoreIndex, Lock lock, Direction direction, boolean replace, Match match, boolean random) {
        if (cores.isEmpty()) {
            throw new IllegalArgumentException("A wand state must contain its default core");
        }
        this.cores = Collections.unmodifiableList(new ArrayList<>(cores));
        this.selectedCoreIndex = selectedCoreIndex >= 0 && selectedCoreIndex < cores.size() ? selectedCoreIndex : 0;
        this.lock = lock;
        this.direction = direction;
        this.replace = replace;
        this.match = match;
        this.random = random;
    }

    private static String enumValue(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    public List<IWandCore> getCores() {
        return cores;
    }

    public IWandCore getSelectedCore() {
        return cores.get(selectedCoreIndex);
    }

    public int getSelectedCoreIndex() {
        return selectedCoreIndex;
    }

    public Lock getLock() {
        return lock;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isReplace() {
        return replace;
    }

    public Match getMatch() {
        return match;
    }

    public boolean isRandom() {
        return random;
    }

    public boolean isEnabled(WandOption option) {
        return option != WandOption.CORES || cores.size() > 1;
    }

    public String getValue(WandOption option) {
        switch (option) {
            case CORES:
                return getSelectedCore().getRegistryName().toString();
            case LOCK:
                return enumValue(lock);
            case DIRECTION:
                return enumValue(direction);
            case REPLACE:
                return replace ? "yes" : "no";
            case MATCH:
                return enumValue(match);
            case RANDOM:
                return random ? "yes" : "no";
            default:
                throw new IllegalArgumentException("Unsupported wand option: " + option);
        }
    }

    public boolean testLock(Lock checkLock) {
        return lock == Lock.NOLOCK || lock == checkLock;
    }

    public boolean matchBlocks(IBlockState first, IBlockState second) {
        if (first == null || second == null) {
            return false;
        }

        int firstMeta = first.getBlock().getMetaFromState(first);
        int secondMeta = second.getBlock().getMetaFromState(second);
        switch (match) {
            case EXACT:
                return first.getBlock() == second.getBlock() && firstMeta == secondMeta;
            case SIMILAR:
                return ConfigRuntime.getBlockEquivalenceIndex().matchBlocks(first.getBlock(), second.getBlock());
            case ANY:
                return first.getBlock() != Blocks.AIR && second.getBlock() != Blocks.AIR;
            default:
                return false;
        }
    }

    public boolean hasUpgrade(IWandUpgrade upgrade) {
        return upgrade instanceof IWandCore && cores.contains(upgrade);
    }

    public enum Lock {
        HORIZONTAL, VERTICAL, NORTHSOUTH, EASTWEST, NOLOCK
    }

    public enum Direction {
        TARGET, PLAYER
    }

    public enum Match {
        EXACT, SIMILAR, ANY
    }
}
