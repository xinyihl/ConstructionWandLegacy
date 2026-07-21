package com.xinyihl.constructionwandlegacy.testfixture;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MaterialAggregationFixtureTest {
    private final Item blockItem = new Item().setRegistryName(new ResourceLocation("test", "block"));

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void duplicateStacksAreCountedOncePerInventorySlot() {
        List<MaterialAggregationFixture.Aggregate> result = MaterialAggregationFixture.aggregate(Arrays.asList(
                stack(32, 0, null),
                stack(16, 0, null),
                stack(7, 1, null)
        ));

        assertEquals(2, result.size());
        assertEquals(48, result.get(0).getCount());
        assertEquals(0, result.get(0).getKey().getMetadata());
        assertEquals(7, result.get(1).getCount());
        assertEquals(1, result.get(1).getKey().getMetadata());
    }

    @Test
    public void differentNbtCreatesDifferentMaterialKeys() {
        NBTTagCompound red = new NBTTagCompound();
        red.setString("variant", "red");
        NBTTagCompound blue = new NBTTagCompound();
        blue.setString("variant", "blue");

        List<MaterialAggregationFixture.Aggregate> result = MaterialAggregationFixture.aggregate(Arrays.asList(
                stack(10, 0, red),
                stack(4, 0, blue),
                stack(6, 0, red.copy())
        ));

        assertEquals(2, result.size());
        assertEquals(16, result.get(0).getCount());
        assertEquals("red", result.get(0).getKey().getTagCompound().getString("variant"));
        assertEquals(4, result.get(1).getCount());
        assertEquals("blue", result.get(1).getKey().getTagCompound().getString("variant"));
    }

    @Test
    public void aggregateCountSaturatesInsteadOfOverflowing() {
        List<MaterialAggregationFixture.Aggregate> result = MaterialAggregationFixture.aggregate(Arrays.asList(
                stack(Integer.MAX_VALUE - 4, 0, null),
                stack(10, 0, null)
        ));

        assertEquals(1, result.size());
        assertEquals(Integer.MAX_VALUE, result.get(0).getCount());
    }

    private ItemStack stack(int count, int meta, NBTTagCompound tag) {
        ItemStack stack = new ItemStack(blockItem, count, meta);
        if (tag != null) {
            stack.setTagCompound(tag);
        }
        return stack;
    }
}
