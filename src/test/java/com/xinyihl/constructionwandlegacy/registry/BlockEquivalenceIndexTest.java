package com.xinyihl.constructionwandlegacy.registry;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class BlockEquivalenceIndexTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    private static String id(Item item) {
        return item.getRegistryName().toString();
    }

    private static Set<Item> set(Item... items) {
        return new java.util.LinkedHashSet<>(Arrays.asList(items));
    }

    @Test
    public void overlappingGroupsPreserveLegacyNonTransitiveRelationships() {
        BlockEquivalenceIndex index = BlockEquivalenceIndex.compile(new String[]{id(Items.DIAMOND) + ";" + id(Items.EMERALD), id(Items.EMERALD) + ";" + id(Items.IRON_INGOT)}, ignored -> {
        });

        assertEquals(set(Items.EMERALD), index.matchingItems(Items.DIAMOND));
        assertEquals(set(Items.DIAMOND, Items.IRON_INGOT), index.matchingItems(Items.EMERALD));
        assertFalse(index.matchingItems(Items.DIAMOND).contains(Items.IRON_INGOT));
    }

    @Test
    public void duplicateIdsAreCollapsedAndTheSourceItemIsExcluded() {
        BlockEquivalenceIndex index = BlockEquivalenceIndex.compile(new String[]{id(Items.DIAMOND) + ";" + id(Items.DIAMOND) + ";" + id(Items.EMERALD)}, ignored -> {
        });

        assertEquals(set(Items.EMERALD), index.matchingItems(Items.DIAMOND));
        assertFalse(index.matchingItems(Items.DIAMOND).contains(Items.DIAMOND));
    }

    @Test
    public void invalidEntriesAreReportedWithoutDiscardingValidGroupMembers() {
        List<String> warnings = new ArrayList<>();
        BlockEquivalenceIndex index = BlockEquivalenceIndex.compile(new String[]{id(Items.DIAMOND) + ";;test:missing_item;not a valid id;" + id(Items.EMERALD), null, ""}, warnings::add);

        assertEquals(set(Items.EMERALD), index.matchingItems(Items.DIAMOND));
        assertEquals(5, warnings.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void matchingSetsAreImmutable() {
        BlockEquivalenceIndex index = BlockEquivalenceIndex.compile(new String[]{id(Items.DIAMOND) + ";" + id(Items.EMERALD)}, ignored -> {
        });

        index.matchingItems(Items.DIAMOND).add(Items.IRON_INGOT);
    }

    @Test
    public void blockMatchingUsesCompiledItemRelationshipAndRejectsAir() {
        BlockEquivalenceIndex index = BlockEquivalenceIndex.compile(new String[]{id(Item.getItemFromBlock(Blocks.STONE)) + ";" + id(Item.getItemFromBlock(Blocks.COBBLESTONE))}, ignored -> {
        });

        assertTrue(index.matchBlocks(Blocks.STONE, Blocks.STONE));
        assertTrue(index.matchBlocks(Blocks.STONE, Blocks.COBBLESTONE));
        assertFalse(index.matchBlocks(Blocks.STONE, Blocks.AIR));
    }
}
