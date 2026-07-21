package com.xinyihl.constructionwandlegacy.config;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlacementRulesTest {
    private static final String BLOCK_ID = "test:rule_block";

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void whitelistParsesBlockIdAndOptionalMetadata() {
        TestMetaBlock block = new TestMetaBlock(false);
        ItemStack stack = stackFor(block);
        List<String> warnings = new ArrayList<>();
        PlacementRules rules = compileWithTestBlock(
                new String[]{"  " + BLOCK_ID + "@1  "}, new String[0], new String[0], false, warnings::add);

        assertTrue(rules.isPlacementAllowed(stack, block.state(true)));
        assertFalse(rules.isPlacementAllowed(stack, block.state(false)));
        assertTrue(warnings.isEmpty());
    }

    @Test
    public void blacklistRejectsMatchingStateButNotOtherMetadata() {
        TestMetaBlock block = new TestMetaBlock(false);
        ItemStack stack = stackFor(block);
        PlacementRules rules = compileWithTestBlock(
                new String[0], new String[]{BLOCK_ID + "@1"}, new String[0], false, ignored -> { });

        assertFalse(rules.isPlacementAllowed(stack, block.state(true)));
        assertTrue(rules.isPlacementAllowed(stack, block.state(false)));
    }

    @Test
    public void invalidRulesAreReportedAndIgnored() {
        TestMetaBlock block = new TestMetaBlock(false);
        List<String> warnings = new ArrayList<>();
        PlacementRules rules = compileWithTestBlock(
                new String[]{null, "", "not a valid id", BLOCK_ID + "@not-a-number"},
                new String[0], new String[0], false, warnings::add);

        assertTrue(rules.isPlacementAllowed(stackFor(block), block.state(false)));
        assertEquals(4, warnings.size());
    }

    @Test
    public void propertyWhitelistIsTrimmedCaseInsensitiveAndSubstringBased() {
        List<String> warnings = new ArrayList<>();
        PlacementRules rules = compileWithTestBlock(
                new String[0], new String[0], new String[]{"  FACING  ", "", null}, false, warnings::add);

        assertTrue(rules.isPropertyCopyAllowed(PropertyBool.create("north_facing")));
        assertFalse(rules.isPropertyCopyAllowed(PropertyBool.create("powered")));
        assertFalse(rules.isPropertyCopyAllowed(null));
        assertEquals(2, warnings.size());
    }

    @Test
    public void tileEntityPlacementRequiresExplicitOptIn() {
        TestMetaBlock tileBlock = new TestMetaBlock(true);
        ItemStack stack = stackFor(tileBlock);
        PlacementRules denied = compileWithTestBlock(
                new String[0], new String[0], new String[0], false, ignored -> { });
        PlacementRules allowed = compileWithTestBlock(
                new String[0], new String[0], new String[0], true, ignored -> { });

        assertFalse(denied.isPlacementAllowed(stack, tileBlock.state(false)));
        assertTrue(allowed.isPlacementAllowed(stack, tileBlock.state(false)));
    }

    @Test
    public void compiledRulesDoNotTrackLaterArrayMutation() {
        TestMetaBlock block = new TestMetaBlock(false);
        String[] whitelist = {BLOCK_ID};
        PlacementRules rules = compileWithTestBlock(
                whitelist, new String[0], new String[0], false, ignored -> { });
        whitelist[0] = "test:other";

        assertTrue(rules.isPlacementAllowed(stackFor(block), block.state(false)));
    }

    @Test
    public void unknownRegisteredBlockIdIsReportedWithRawValueAndIgnored() {
        TestMetaBlock block = new TestMetaBlock(false);
        List<String> warnings = new ArrayList<>();
        PlacementRules rules = PlacementRules.compile(
                new String[]{"typo:missing_block"}, new String[0], new String[0], false, warnings::add);

        assertTrue(rules.isPlacementAllowed(stackFor(block), block.state(false)));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("typo:missing_block"));
    }

    @Test
    public void registeredAirIdIsAcceptedWithoutWarning() {
        List<String> warnings = new ArrayList<>();
        PlacementRules.compile(
                new String[]{"minecraft:air"}, new String[0], new String[0], false, warnings::add);

        assertTrue(warnings.isEmpty());
    }

    private static PlacementRules compileWithTestBlock(String[] whitelist, String[] blacklist,
                                                       String[] propertyKeywords, boolean allowTileEntities,
                                                       Consumer<String> warningSink) {
        return PlacementRules.compile(whitelist, blacklist, propertyKeywords, allowTileEntities,
                warningSink, id -> BLOCK_ID.equals(id.toString()));
    }

    private static ItemStack stackFor(Block block) {
        Item item = new ItemBlock(block).setRegistryName(new ResourceLocation("test", "rule_block_item"));
        return new ItemStack(item);
    }

    private static final class TestMetaBlock extends Block {
        private static final PropertyBool VARIANT = PropertyBool.create("variant");
        private final boolean tileEntity;

        private TestMetaBlock(boolean tileEntity) {
            super(Material.ROCK);
            this.tileEntity = tileEntity;
            setRegistryName(new ResourceLocation(BLOCK_ID));
            setDefaultState(blockState.getBaseState().withProperty(VARIANT, false));
        }

        private IBlockState state(boolean variant) {
            return getDefaultState().withProperty(VARIANT, variant);
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, VARIANT);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(VARIANT) ? 1 : 0;
        }

        @Override
        public boolean hasTileEntity(IBlockState state) {
            return tileEntity;
        }
    }
}
