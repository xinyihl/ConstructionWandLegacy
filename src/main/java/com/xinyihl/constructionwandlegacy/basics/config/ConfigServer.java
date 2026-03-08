package com.xinyihl.constructionwandlegacy.basics.config;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;

public final class ConfigServer {

    public static final int LIMIT_CREATIVE = 1024;
    private static final List<BlockRule> placementWhitelist = new ArrayList<>();
    private static final List<BlockRule> placementBlacklist = new ArrayList<>();
    private static final List<String> propertyCopyWhitelist = new ArrayList<>();
    private static final WandProperties DEFAULT_PROPERTIES = new WandProperties(9, 131, 32, 0, true);
    private static final Map<Item, WandProperties> BASE_WAND_PROPERTIES = new HashMap<>();
    private static final Map<Item, WandProperties> WAND_PROPERTIES = new HashMap<>();
    public static String[] similarBlocks = new String[0];
    public static boolean allowTileEntityPlacement = false;

    public static void registerWandProperties(Item item, WandProperties properties) {
        BASE_WAND_PROPERTIES.put(item, properties);
        WAND_PROPERTIES.put(item, applyConfiguredLimit(item, properties));
    }

    public static void sync() {
        allowTileEntityPlacement = Configurations.placement.allowTileEntityPlacement;
        similarBlocks = Configurations.matching.similarBlocks == null ? new String[0] : Configurations.matching.similarBlocks;
        rebuildRules(placementWhitelist, Configurations.placement.blockWhitelist);
        rebuildRules(placementBlacklist, Configurations.placement.blockBlacklist);
        rebuildStringWhitelist(Configurations.placement.propertyCopyWhitelist);
        recomputeRegisteredWandProperties();
    }

    public static WandProperties getWandProperties(Item item) {
        if (item == null) {
            return DEFAULT_PROPERTIES;
        }
        WandProperties properties = WAND_PROPERTIES.get(item);
        return properties == null ? DEFAULT_PROPERTIES : properties;
    }

    public static boolean isPlacementAllowed(ItemStack placeStack, @Nullable IBlockState placeState) {
        if (placeStack.isEmpty() || !(placeStack.getItem() instanceof ItemBlock)) {
            return false;
        }

        ItemBlock item = (ItemBlock) placeStack.getItem();
        Block block = item.getBlock();
        IBlockState state = placeState != null ? placeState : block.getDefaultState();

        if (!allowTileEntityPlacement && block.hasTileEntity(state)) {
            return false;
        }

        if (!placementWhitelist.isEmpty() && matchesAnyRule(placementWhitelist, state)) {
            return false;
        }

        return matchesAnyRule(placementBlacklist, state);
    }

    public static boolean isPropertyCopyAllowed(IProperty<?> property) {
        if (property == null) {
            return false;
        }
        if (propertyCopyWhitelist.isEmpty()) {
            return false;
        }

        String name = property.getName().toLowerCase(Locale.ROOT);
        for (String keyword : propertyCopyWhitelist) {
            if (!keyword.isEmpty() && name.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static int getConfiguredLimit(Item item, int fallback) {
        ResourceLocation registryName = item.getRegistryName();
        if (registryName == null) {
            return fallback;
        }

        String path = registryName.getPath();
        switch (path) {
            case "stone_wand":
                return Configurations.wandLimits.stoneWandMaxBlocks;
            case "iron_wand":
                return Configurations.wandLimits.ironWandMaxBlocks;
            case "diamond_wand":
                return Configurations.wandLimits.diamondWandMaxBlocks;
            case "infinity_wand":
                return Configurations.wandLimits.infinityWandMaxBlocks;
        }
        return fallback;
    }

    private static WandProperties applyConfiguredLimit(Item item, WandProperties base) {
        return new WandProperties(
                getConfiguredLimit(item, base.getLimit()),
                base.getDurability(),
                base.getAngel(),
                base.getDestruction(),
                base.isUpgradeable()
        );
    }

    private static void recomputeRegisteredWandProperties() {
        if (BASE_WAND_PROPERTIES.isEmpty()) {
            return;
        }

        WAND_PROPERTIES.clear();
        for (Map.Entry<Item, WandProperties> entry : BASE_WAND_PROPERTIES.entrySet()) {
            WAND_PROPERTIES.put(entry.getKey(), applyConfiguredLimit(entry.getKey(), entry.getValue()));
        }
    }

    private static void rebuildRules(List<BlockRule> out, String[] entries) {
        out.clear();
        if (entries == null) {
            return;
        }

        for (String raw : entries) {
            BlockRule rule = BlockRule.parse(raw);
            if (rule != null) {
                out.add(rule);
            }
        }
    }

    private static void rebuildStringWhitelist(String[] entries) {
        ConfigServer.propertyCopyWhitelist.clear();
        if (entries == null) {
            return;
        }

        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String value = entry.trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) {
                ConfigServer.propertyCopyWhitelist.add(value);
            }
        }
    }

    private static boolean matchesAnyRule(List<BlockRule> rules, IBlockState state) {
        for (BlockRule rule : rules) {
            if (rule.matches(state)) {
                return false;
            }
        }
        return true;
    }

    private static final class BlockRule {
        private final ResourceLocation blockId;
        @Nullable
        private final Integer meta;

        private BlockRule(ResourceLocation blockId, @Nullable Integer meta) {
            this.blockId = blockId;
            this.meta = meta;
        }

        @Nullable
        private static BlockRule parse(String raw) {
            if (raw == null) {
                return null;
            }

            String value = raw.trim();
            if (value.isEmpty()) {
                return null;
            }

            String[] parts = value.split("@", 2);
            ResourceLocation id;
            try {
                id = new ResourceLocation(parts[0]);
            } catch (Exception ignored) {
                return null;
            }

            Integer meta = null;
            if (parts.length == 2) {
                try {
                    meta = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }

            return new BlockRule(id, meta);
        }

        private boolean matches(IBlockState state) {
            ResourceLocation stateId = state.getBlock().getRegistryName();
            if (stateId == null || !stateId.equals(blockId)) {
                return false;
            }

            if (meta == null) {
                return true;
            }

            int stateMeta = state.getBlock().getMetaFromState(state);
            return stateMeta == meta;
        }
    }

    public static final class WandProperties {
        private final int limit;
        private final int durability;
        private final int angel;
        private final int destruction;
        private final boolean upgradeable;

        public WandProperties(int limit, int durability, int angel, int destruction, boolean upgradeable) {
            this.limit = limit;
            this.durability = durability;
            this.angel = angel;
            this.destruction = destruction;
            this.upgradeable = upgradeable;
        }

        public int getLimit() {
            return limit;
        }

        public int getDurability() {
            return durability;
        }

        public int getAngel() {
            return angel;
        }

        public int getDestruction() {
            return destruction;
        }

        public boolean isUpgradeable() {
            return upgradeable;
        }
    }

}
