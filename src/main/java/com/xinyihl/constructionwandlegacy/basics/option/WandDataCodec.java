package com.xinyihl.constructionwandlegacy.basics.option;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.items.core.CoreDefault;
import com.xinyihl.constructionwandlegacy.wand.upgrade.IWandCore;
import com.xinyihl.constructionwandlegacy.wand.upgrade.IWandUpgrade;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public final class WandDataCodec {
    public static final String TAG_ROOT = "wand_options";

    private static final String TAG_CORE_SELECTOR = "cores_sel";
    private static final IWandCore DEFAULT_CORE = new CoreDefault();
    private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern VALID_PATH = Pattern.compile("[a-z0-9/._-]+");

    private WandDataCodec() {
    }

    public static WandState read(ItemStack wandStack) {
        NBTTagCompound data = findData(wandStack);
        List<IWandCore> cores = readCores(data);
        int selectedCore = data == null ? 0 : data.getByte(TAG_CORE_SELECTOR);

        WandState.Lock lock = readEnum(data, WandOption.LOCK, WandState.Lock.class, WandState.Lock.NOLOCK);
        WandState.Direction direction = readEnum(data, WandOption.DIRECTION, WandState.Direction.class,
                WandState.Direction.TARGET);
        boolean replace = readBoolean(data, WandOption.REPLACE, true);
        WandState.Match match = readEnum(data, WandOption.MATCH, WandState.Match.class, WandState.Match.SIMILAR);
        boolean random = readBoolean(data, WandOption.RANDOM, false);

        return new WandState(cores, selectedCore, lock, direction, replace, match, random);
    }

    public static boolean update(ItemStack wandStack, WandOption option, String value) {
        if (option == null || value == null || !isWritableStack(wandStack)) {
            return false;
        }

        WandState state = read(wandStack);
        switch (option) {
            case CORES:
                int selector = findCore(state.getCores(), value);
                if (selector < 0 || selector > Byte.MAX_VALUE) {
                    return false;
                }
                return update(wandStack, data -> data.setByte(TAG_CORE_SELECTOR, (byte) selector));
            case LOCK:
                WandState.Lock lock = parseEnum(WandState.Lock.class, value);
                return lock != null && writeString(wandStack, option, enumValue(lock));
            case DIRECTION:
                WandState.Direction direction = parseEnum(WandState.Direction.class, value);
                return direction != null && writeString(wandStack, option, enumValue(direction));
            case REPLACE:
            case RANDOM:
                Boolean booleanValue = parseBoolean(value);
                return booleanValue != null && update(wandStack,
                        data -> data.setBoolean(option.getId(), booleanValue));
            case MATCH:
                WandState.Match match = parseEnum(WandState.Match.class, value);
                return match != null && writeString(wandStack, option, enumValue(match));
            default:
                return false;
        }
    }

    public static boolean cycle(ItemStack wandStack, WandOption option, boolean forward) {
        if (option == null || !isWritableStack(wandStack)) {
            return false;
        }

        WandState state = read(wandStack);
        int next = cycleNetworkValue(state, option, forward);
        return next >= 0 && updateNetworkValue(wandStack, option, next);
    }

    public static int getNetworkValue(WandState state, WandOption option) {
        if (state == null || option == null) {
            return -1;
        }
        switch (option) {
            case CORES:
                return state.getSelectedCoreIndex();
            case LOCK:
                return state.getLock().ordinal();
            case DIRECTION:
                return state.getDirection().ordinal();
            case REPLACE:
                return state.isReplace() ? 1 : 0;
            case MATCH:
                return state.getMatch().ordinal();
            case RANDOM:
                return state.isRandom() ? 1 : 0;
            default:
                return -1;
        }
    }

    public static int cycleNetworkValue(WandState state, WandOption option, boolean forward) {
        if (state == null || option == null || !state.isEnabled(option)) {
            return -1;
        }
        int current = getNetworkValue(state, option);
        int size;
        switch (option) {
            case CORES:
                size = state.getCores().size();
                break;
            case LOCK:
                size = WandState.Lock.values().length;
                break;
            case DIRECTION:
                size = WandState.Direction.values().length;
                break;
            case MATCH:
                size = WandState.Match.values().length;
                break;
            case REPLACE:
            case RANDOM:
                size = 2;
                break;
            default:
                return -1;
        }
        return wrap(current + (forward ? 1 : -1), size);
    }

    public static boolean isValidNetworkValue(WandState state, WandOption option, int value) {
        if (state == null || option == null || !state.isEnabled(option) || value < 0) {
            return false;
        }
        switch (option) {
            case CORES:
                return value < state.getCores().size() && value <= Byte.MAX_VALUE;
            case LOCK:
                return value < WandState.Lock.values().length;
            case DIRECTION:
                return value < WandState.Direction.values().length;
            case REPLACE:
            case RANDOM:
                return value <= 1;
            case MATCH:
                return value < WandState.Match.values().length;
            default:
                return false;
        }
    }

    public static boolean updateNetworkValue(ItemStack wandStack, WandOption option, int value) {
        WandState state = read(wandStack);
        if (!isWritableStack(wandStack) || !isValidNetworkValue(state, option, value)) {
            return false;
        }
        switch (option) {
            case CORES:
                return update(wandStack, data -> data.setByte(TAG_CORE_SELECTOR, (byte) value));
            case LOCK:
                return writeString(wandStack, option, enumValue(WandState.Lock.values()[value]));
            case DIRECTION:
                return writeString(wandStack, option, enumValue(WandState.Direction.values()[value]));
            case REPLACE:
            case RANDOM:
                return update(wandStack, data -> data.setBoolean(option.getId(), value == 1));
            case MATCH:
                return writeString(wandStack, option, enumValue(WandState.Match.values()[value]));
            default:
                return false;
        }
    }

    public static boolean addUpgrade(ItemStack wandStack, IWandUpgrade upgrade) {
        if (!isWritableStack(wandStack) || !(upgrade instanceof Item) || !(upgrade instanceof IWandCore)) {
            return false;
        }

        Item item = (Item) upgrade;
        ResourceLocation registryName = item.getRegistryName();
        if (registryName == null || ForgeRegistries.ITEMS.getValue(registryName) != item) {
            ConstructionWandLegacy.LOGGER.warn("Unregistered wand upgrade: {}", registryName);
            return false;
        }

        WandState state = read(wandStack);
        if (state.hasUpgrade(upgrade)) {
            return false;
        }

        return update(wandStack, data -> {
            NBTTagList list = new NBTTagList();
            for (IWandCore core : state.getCores()) {
                if (core instanceof Item && core.getRegistryName() != null) {
                    list.appendTag(new NBTTagString(core.getRegistryName().toString()));
                }
            }
            list.appendTag(new NBTTagString(registryName.toString()));
            data.setTag(WandOption.CORES.getId(), list);
            data.setByte(TAG_CORE_SELECTOR, (byte) state.getSelectedCoreIndex());
        });
    }

    public static NBTTagCompound readData(ItemStack wandStack) {
        NBTTagCompound data = findData(wandStack);
        return data == null ? new NBTTagCompound() : data.copy();
    }

    public static boolean update(ItemStack wandStack, Consumer<NBTTagCompound> updater) {
        if (!isWritableStack(wandStack) || updater == null) {
            return false;
        }
        updater.accept(getOrCreateData(wandStack));
        return true;
    }

    private static List<IWandCore> readCores(@Nullable NBTTagCompound data) {
        ArrayList<IWandCore> cores = new ArrayList<>();
        cores.add(DEFAULT_CORE);
        if (data == null) {
            return cores;
        }

        NBTTagList list = data.getTagList(WandOption.CORES.getId(), Constants.NBT.TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) {
            String rawId = list.getStringTagAt(i);
            ResourceLocation id;
            try {
                id = new ResourceLocation(rawId);
            } catch (RuntimeException exception) {
                ConstructionWandLegacy.LOGGER.warn("Invalid wand upgrade id: {}", rawId);
                continue;
            }
            if (!VALID_NAMESPACE.matcher(id.getNamespace()).matches()
                    || !VALID_PATH.matcher(id.getPath()).matches()) {
                ConstructionWandLegacy.LOGGER.warn("Invalid wand upgrade id: {}", rawId);
                continue;
            }

            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null) {
                ConstructionWandLegacy.LOGGER.warn("Unknown wand upgrade: {}", rawId);
                continue;
            }
            if (!(item instanceof IWandCore)) {
                ConstructionWandLegacy.LOGGER.warn("Item is not a wand core: {}", rawId);
                continue;
            }
            if (item.getRegistryName() == null || !id.equals(item.getRegistryName())) {
                ConstructionWandLegacy.LOGGER.warn("Unregistered wand upgrade: {}", rawId);
                continue;
            }

            IWandCore core = (IWandCore) item;
            if (!cores.contains(core)) {
                cores.add(core);
            }
        }
        return cores;
    }

    private static boolean readBoolean(@Nullable NBTTagCompound data, WandOption option, boolean defaultValue) {
        return data != null && data.hasKey(option.getId()) ? data.getBoolean(option.getId()) : defaultValue;
    }

    private static <E extends Enum<E>> E readEnum(@Nullable NBTTagCompound data, WandOption option,
                                                   Class<E> enumClass, E defaultValue) {
        if (data == null) {
            return defaultValue;
        }
        E value = parseEnum(enumClass, data.getString(option.getId()));
        return value == null ? defaultValue : value;
    }

    @Nullable
    private static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Nullable
    private static Boolean parseBoolean(String value) {
        if ("yes".equals(value)) {
            return Boolean.TRUE;
        }
        if ("no".equals(value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static boolean writeString(ItemStack wandStack, WandOption option, String value) {
        return update(wandStack, data -> data.setString(option.getId(), value));
    }

    private static int findCore(List<IWandCore> cores, String registryName) {
        for (int i = 0; i < cores.size(); i++) {
            ResourceLocation id = cores.get(i).getRegistryName();
            if (id != null && id.toString().equals(registryName)) {
                return i;
            }
        }
        return -1;
    }

    private static int wrap(int value, int size) {
        int wrapped = value % size;
        return wrapped < 0 ? wrapped + size : wrapped;
    }

    private static String enumValue(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static boolean isWritableStack(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }

    @Nullable
    private static NBTTagCompound findData(ItemStack wandStack) {
        if (wandStack == null || wandStack.isEmpty()) {
            return null;
        }
        NBTTagCompound root = wandStack.getTagCompound();
        if (root == null || !root.hasKey(TAG_ROOT, Constants.NBT.TAG_COMPOUND)) {
            return null;
        }
        return root.getCompoundTag(TAG_ROOT);
    }

    private static NBTTagCompound getOrCreateData(ItemStack wandStack) {
        NBTTagCompound root = wandStack.getTagCompound();
        if (root == null) {
            root = new NBTTagCompound();
            wandStack.setTagCompound(root);
        }
        if (!root.hasKey(TAG_ROOT, Constants.NBT.TAG_COMPOUND)) {
            root.setTag(TAG_ROOT, new NBTTagCompound());
        }
        return root.getCompoundTag(TAG_ROOT);
    }
}
