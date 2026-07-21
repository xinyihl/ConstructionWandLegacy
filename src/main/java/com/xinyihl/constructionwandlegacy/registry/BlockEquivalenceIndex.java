package com.xinyihl.constructionwandlegacy.registry;

import com.xinyihl.constructionwandlegacy.config.ConfigWarnings;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public final class BlockEquivalenceIndex {
    private static final BlockEquivalenceIndex EMPTY = new BlockEquivalenceIndex(Collections.emptyMap());

    private final Map<Item, Set<Item>> matchingItems;

    private BlockEquivalenceIndex(Map<Item, Set<Item>> matchingItems) {
        this.matchingItems = matchingItems;
    }

    public static BlockEquivalenceIndex empty() {
        return EMPTY;
    }

    public static BlockEquivalenceIndex compile(String[] groups) {
        return compile(groups, ConfigWarnings::warn);
    }

    public static BlockEquivalenceIndex compile(String[] groups, Consumer<String> warningSink) {
        Objects.requireNonNull(warningSink, "warningSink");
        if (groups == null) {
            warn(warningSink, null, null);
            return EMPTY;
        }

        Map<Item, Set<Item>> mutableIndex = new HashMap<>();
        for (String rawGroup : groups) {
            List<Item> group = parseGroup(rawGroup, warningSink);
            for (Item item : group) {
                Set<Item> matches = mutableIndex.computeIfAbsent(item, ignored -> new LinkedHashSet<>());
                matches.addAll(group);
                matches.remove(item);
            }
        }

        Map<Item, Set<Item>> immutableIndex = new HashMap<>();
        for (Map.Entry<Item, Set<Item>> entry : mutableIndex.entrySet()) {
            immutableIndex.put(entry.getKey(), Collections.unmodifiableSet(new LinkedHashSet<>(entry.getValue())));
        }
        return new BlockEquivalenceIndex(Collections.unmodifiableMap(immutableIndex));
    }

    private static List<Item> parseGroup(@Nullable String rawGroup, Consumer<String> warningSink) {
        if (rawGroup == null || rawGroup.trim().isEmpty()) {
            warn(warningSink, rawGroup, null);
            return Collections.emptyList();
        }

        Set<Item> items = new LinkedHashSet<>();
        for (String rawId : rawGroup.split(";", -1)) {
            String value = rawId.trim();
            if (value.isEmpty()) {
                warn(warningSink, rawGroup, rawId);
                continue;
            }

            ResourceLocation id;
            try {
                id = new ResourceLocation(value);
            } catch (RuntimeException exception) {
                warn(warningSink, rawGroup, rawId);
                continue;
            }

            Item item = Item.REGISTRY.getObject(id);
            if (item == null || item == Items.AIR) {
                warn(warningSink, rawGroup, rawId);
                continue;
            }
            items.add(item);
        }
        return new ArrayList<>(items);
    }

    private static void warn(Consumer<String> warningSink, @Nullable String rawGroup, @Nullable String rawId) {
        String detail = rawId == null ? "" : ", invalid id: " + rawId;
        warningSink.accept("Invalid similar-block config entry (raw value: " + rawGroup + detail + ")");
    }

    public Set<Item> matchingItems(Item item) {
        Set<Item> matches = matchingItems.get(item);
        return matches == null ? Collections.emptySet() : matches;
    }

    public boolean matchBlocks(Block first, Block second) {
        if (first == second) {
            return true;
        }
        if (first == Blocks.AIR || second == Blocks.AIR) {
            return false;
        }

        Item firstItem = Item.getItemFromBlock(first);
        Item secondItem = Item.getItemFromBlock(second);
        return matchingItems(firstItem).contains(secondItem);
    }
}
