package com.xinyihl.constructionwandlegacy.basics;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.basics.config.ConfigServer;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class ReplacementRegistry {
    private static final HashSet<HashSet<Item>> replacements = new HashSet<>();

    public static void init() {
        replacements.clear();

        for (String key : ConfigServer.similarBlocks) {
            HashSet<Item> set = new HashSet<>();
            for (String id : key.split(";")) {
                Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
                if (item == null || item == Items.AIR) {
                    ConstructionWandLegacy.LOGGER.warn("Replacement Registry: Could not find item {}", id);
                    continue;
                }
                set.add(item);
            }

            if (!set.isEmpty()) {
                replacements.add(set);
            }
        }
    }

    public static Set<Item> getMatchingSet(Item item) {
        HashSet<Item> result = new HashSet<>();
        for (HashSet<Item> set : replacements) {
            if (set.contains(item)) {
                result.addAll(set);
            }
        }
        result.remove(item);
        return result;
    }

    public static boolean matchBlocks(Block first, Block second) {
        if (first == second) {
            return true;
        }
        if (first == Blocks.AIR || second == Blocks.AIR) {
            return false;
        }

        Item firstItem = Item.getItemFromBlock(first);
        Item secondItem = Item.getItemFromBlock(second);
        for (HashSet<Item> set : replacements) {
            if (set.contains(firstItem) && set.contains(secondItem)) {
                return true;
            }
        }
        return false;
    }
}
