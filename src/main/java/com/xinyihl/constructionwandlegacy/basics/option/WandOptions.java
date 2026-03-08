package com.xinyihl.constructionwandlegacy.basics.option;

import com.xinyihl.constructionwandlegacy.api.IWandCore;
import com.xinyihl.constructionwandlegacy.api.IWandUpgrade;
import com.xinyihl.constructionwandlegacy.basics.ReplacementRegistry;
import com.xinyihl.constructionwandlegacy.items.core.CoreDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

public class WandOptions {
    private static final String TAG_ROOT = "wand_options";
    public final NBTTagCompound tag;
    public final WandUpgradesSelectable<IWandCore> cores;
    public final OptionEnum<LOCK> lock;
    public final OptionEnum<DIRECTION> direction;
    public final OptionBoolean replace;
    public final OptionEnum<MATCH> match;
    public final OptionBoolean random;
    public final IOption<?>[] allOptions;
    public WandOptions(ItemStack wandStack) {
        tag = wandStack.getOrCreateSubCompound(TAG_ROOT);

        cores = new WandUpgradesSelectable<>(tag, "cores", new CoreDefault());

        lock = new OptionEnum<>(tag, "lock", LOCK.class, LOCK.NOLOCK);
        direction = new OptionEnum<>(tag, "direction", DIRECTION.class, DIRECTION.TARGET);
        replace = new OptionBoolean(tag, "replace", true);
        match = new OptionEnum<>(tag, "match", MATCH.class, MATCH.SIMILAR);
        random = new OptionBoolean(tag, "random", false);

        allOptions = new IOption[]{cores, lock, direction, replace, match, random};
    }

    @Nullable
    public IOption<?> get(String key) {
        for (IOption<?> option : allOptions) {
            if (option.getKey().equals(key)) {
                return option;
            }
        }
        return null;
    }

    public boolean testLock(LOCK checkLock) {
        if (lock.get() == LOCK.NOLOCK) {
            return true;
        }
        return lock.get() == checkLock;
    }

    public boolean matchBlocks(IBlockState first, IBlockState second) {
        if (first == null || second == null) {
            return false;
        }

        int firstMeta = first.getBlock().getMetaFromState(first);
        int secondMeta = second.getBlock().getMetaFromState(second);

        switch (match.get()) {
            case EXACT:
                return first.getBlock() == second.getBlock() && firstMeta == secondMeta;
            case SIMILAR:
                return ReplacementRegistry.matchBlocks(first.getBlock(), second.getBlock());
            case ANY:
                return first.getBlock() != Blocks.AIR && second.getBlock() != Blocks.AIR;
            default:
                return false;
        }
    }

    public boolean hasUpgrade(IWandUpgrade upgrade) {
        if (upgrade instanceof IWandCore) {
            return cores.hasUpgrade((IWandCore) upgrade);
        }
        return false;
    }

    public boolean addUpgrade(IWandUpgrade upgrade) {
        if (upgrade instanceof IWandCore) {
            return cores.addUpgrade((IWandCore) upgrade);
        }
        return false;
    }

    public enum LOCK {
        HORIZONTAL,
        VERTICAL,
        NORTHSOUTH,
        EASTWEST,
        NOLOCK
    }

    public enum DIRECTION {
        TARGET,
        PLAYER
    }

    public enum MATCH {
        EXACT,
        SIMILAR,
        ANY
    }
}
