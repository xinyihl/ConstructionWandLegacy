package com.xinyihl.constructionwandlegacy.basics.option;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.wand.action.WandAction;
import com.xinyihl.constructionwandlegacy.wand.upgrade.IWandCore;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class WandDataCodecTest {
    private static final ResourceLocation TEST_CORE_ID = new ResourceLocation("constructionwandlegacy_test", "core");
    private static final TestCore TEST_CORE = new TestCore();

    @BeforeClass
    public static void bootstrapRegistries() {
        Bootstrap.register();
        ConstructionWandLegacy.LOGGER = LogManager.getLogger(WandDataCodecTest.class);
        TEST_CORE.setRegistryName(TEST_CORE_ID);
        ForgeRegistries.ITEMS.register(TEST_CORE);
    }

    @Test
    public void readingAnUninitializedStackUsesDefaultsWithoutCreatingNbt() {
        ItemStack stack = wand();

        WandState state = WandDataCodec.read(stack);

        assertNull(stack.getTagCompound());
        assertEquals(WandState.Lock.NOLOCK, state.getLock());
        assertEquals(WandState.Direction.TARGET, state.getDirection());
        assertTrue(state.isReplace());
        assertEquals(WandState.Match.SIMILAR, state.getMatch());
        assertFalse(state.isRandom());
        assertEquals(1, state.getCores().size());
        assertEquals(0, state.getSelectedCoreIndex());
    }

    @Test
    public void readsAllLegacyOptionKeysAndCoreSelectorWithoutMutation() {
        ItemStack stack = wandWithLegacyData();
        NBTTagCompound before = stack.getTagCompound().copy();

        WandState state = WandDataCodec.read(stack);

        assertEquals(before, stack.getTagCompound());
        assertEquals(WandState.Lock.VERTICAL, state.getLock());
        assertEquals(WandState.Direction.PLAYER, state.getDirection());
        assertFalse(state.isReplace());
        assertEquals(WandState.Match.EXACT, state.getMatch());
        assertTrue(state.isRandom());
        assertEquals(2, state.getCores().size());
        assertEquals(1, state.getSelectedCoreIndex());
        assertSame(TEST_CORE, state.getSelectedCore());
    }

    @Test
    public void updatesUseTheLegacyKeyAndValueFormatsAndPreserveUnrelatedData() {
        ItemStack stack = wandWithLegacyData();
        NBTTagCompound data = stack.getSubCompound(WandDataCodec.TAG_ROOT);
        data.setIntArray("bound_container_pos", new int[]{1, 2, 3});
        NBTTagList coresBefore = data.getTagList("cores", 8).copy();

        assertTrue(WandDataCodec.update(stack, WandOption.LOCK, "horizontal"));
        assertTrue(WandDataCodec.update(stack, WandOption.DIRECTION, "target"));
        assertTrue(WandDataCodec.update(stack, WandOption.REPLACE, "yes"));
        assertTrue(WandDataCodec.update(stack, WandOption.MATCH, "any"));
        assertTrue(WandDataCodec.update(stack, WandOption.RANDOM, "no"));
        assertTrue(WandDataCodec.update(stack, WandOption.CORES, TEST_CORE_ID.toString()));

        assertEquals("horizontal", data.getString("lock"));
        assertEquals("target", data.getString("direction"));
        assertTrue(data.getBoolean("replace"));
        assertEquals("any", data.getString("match"));
        assertFalse(data.getBoolean("random"));
        assertEquals(1, data.getByte("cores_sel"));
        assertEquals(coresBefore, data.getTagList("cores", 8));
        assertEquals(3, data.getIntArray("bound_container_pos").length);
    }

    @Test
    public void addingACoreWritesTheLegacyStringListFormat() {
        ItemStack stack = wand();

        assertTrue(WandDataCodec.addUpgrade(stack, TEST_CORE));

        NBTTagCompound data = stack.getSubCompound(WandDataCodec.TAG_ROOT);
        NBTTagList cores = data.getTagList("cores", 8);
        assertEquals(1, cores.tagCount());
        assertEquals(TEST_CORE_ID.toString(), cores.getStringTagAt(0));
        assertEquals(0, data.getByte("cores_sel"));
        assertEquals(2, WandDataCodec.read(stack).getCores().size());
    }

    @Test
    public void invalidLegacyCoreEntriesAreIgnoredWithoutRepairingNbt() {
        ItemStack stack = wand();
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList cores = new NBTTagList();
        cores.appendTag(new NBTTagString("not a valid id"));
        cores.appendTag(new NBTTagString("test:missing_core"));
        cores.appendTag(new NBTTagString(ItemsForTest.nonCoreId()));
        data.setTag("cores", cores);
        data.setByte("cores_sel", (byte) 12);
        stack.setTagInfo(WandDataCodec.TAG_ROOT, data);
        NBTTagCompound before = stack.getTagCompound().copy();

        WandState state = WandDataCodec.read(stack);

        assertEquals(before, stack.getTagCompound());
        assertEquals(1, state.getCores().size());
        assertEquals(0, state.getSelectedCoreIndex());
    }

    @Test
    public void invalidUpdateDoesNotCreateOrChangeNbt() {
        ItemStack emptyData = wand();
        assertFalse(WandDataCodec.update(emptyData, WandOption.LOCK, "sideways"));
        assertNull(emptyData.getTagCompound());

        ItemStack legacy = wandWithLegacyData();
        NBTTagCompound before = legacy.getTagCompound().copy();
        assertFalse(WandDataCodec.update(legacy, WandOption.REPLACE, "sometimes"));
        assertEquals(before, legacy.getTagCompound());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void returnedCoreListIsImmutable() {
        WandDataCodec.read(wand()).getCores().clear();
    }

    @Test
    public void networkValuesAreBoundedAndUseLegacyNbtEncoding() {
        ItemStack stack = wandWithLegacyData();
        WandState state = WandDataCodec.read(stack);

        assertEquals(state.getLock().ordinal(),
                WandDataCodec.getNetworkValue(state, WandOption.LOCK));
        assertTrue(WandDataCodec.updateNetworkValue(
                stack, WandOption.LOCK, WandState.Lock.EASTWEST.ordinal()));
        assertEquals("eastwest", stack.getSubCompound(WandDataCodec.TAG_ROOT).getString("lock"));
        assertFalse(WandDataCodec.updateNetworkValue(
                stack, WandOption.LOCK, WandState.Lock.values().length));
        assertFalse(WandDataCodec.updateNetworkValue(stack, WandOption.REPLACE, 2));
        assertFalse(WandDataCodec.updateNetworkValue(stack, WandOption.CORES, 127));
    }

    private static ItemStack wand() {
        return new ItemStack(new Item().setRegistryName(new ResourceLocation("constructionwandlegacy_test", "wand")));
    }

    private static ItemStack wandWithLegacyData() {
        ItemStack stack = wand();
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList cores = new NBTTagList();
        cores.appendTag(new NBTTagString(TEST_CORE_ID.toString()));
        data.setTag("cores", cores);
        data.setByte("cores_sel", (byte) 1);
        data.setString("lock", "vertical");
        data.setString("direction", "player");
        data.setBoolean("replace", false);
        data.setString("match", "exact");
        data.setBoolean("random", true);
        stack.setTagInfo(WandDataCodec.TAG_ROOT, data);
        return stack;
    }

    private static final class TestCore extends Item implements IWandCore {
        @Override
        public int getColor() {
            return 0x123456;
        }

        @Override
        public WandAction getWandAction() {
            return null;
        }
    }

    private static final class ItemsForTest {
        private static String nonCoreId() {
            return net.minecraft.init.Items.DIAMOND.getRegistryName().toString();
        }
    }
}
