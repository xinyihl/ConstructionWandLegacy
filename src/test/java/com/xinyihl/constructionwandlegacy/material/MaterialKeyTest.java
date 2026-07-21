package com.xinyihl.constructionwandlegacy.material;

import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class MaterialKeyTest {
    private final Item item = new Item().setRegistryName(new ResourceLocation("test", "material_key"));

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void countDoesNotParticipateInIdentity() {
        assertEquals(MaterialKey.of(new ItemStack(item, 1, 3)),
                MaterialKey.of(new ItemStack(item, 64, 3)));
    }

    @Test
    public void metadataAndNbtParticipateInIdentity() {
        NBTTagCompound red = new NBTTagCompound();
        red.setString("variant", "red");
        NBTTagCompound blue = new NBTTagCompound();
        blue.setString("variant", "blue");

        assertNotEquals(MaterialKey.of(stack(1, 0, red)), MaterialKey.of(stack(1, 1, red)));
        assertNotEquals(MaterialKey.of(stack(1, 0, red)), MaterialKey.of(stack(1, 0, blue)));
        assertEquals(MaterialKey.of(stack(1, 0, red)), MaterialKey.of(stack(32, 0, red.copy())));
    }

    @Test
    public void copiedNbtCannotMutateKey() {
        NBTTagCompound original = new NBTTagCompound();
        original.setString("variant", "red");
        MaterialKey key = MaterialKey.of(stack(1, 0, original));

        original.setString("variant", "blue");
        NBTTagCompound exposed = key.getTag();
        exposed.setString("variant", "green");

        assertEquals("red", key.createStack(1).getTagCompound().getString("variant"));
    }

    private ItemStack stack(int count, int metadata, NBTTagCompound tag) {
        ItemStack stack = new ItemStack(item, count, metadata);
        stack.setTagCompound(tag);
        return stack;
    }
}
