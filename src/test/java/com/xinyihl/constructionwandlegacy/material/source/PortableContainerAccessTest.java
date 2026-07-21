package com.xinyihl.constructionwandlegacy.material.source;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class PortableContainerAccessTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void copiedOrReplacedOwnerInvalidatesCapturedEndpoint() {
        ItemStack owner = new ItemStack(Blocks.WHITE_SHULKER_BOX);
        PortableContainerAccess.ContainerAccess access = PortableContainerAccess.open(owner);

        assertNotNull(access);
        assertTrue(access.isValid(owner));
        assertFalse(access.isValid(owner.copy()));
        owner.setCount(0);
        assertFalse(access.isValid(owner));
    }
}
