package com.xinyihl.constructionwandlegacy.items;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.Tags;
import com.xinyihl.constructionwandlegacy.basics.config.ConfigServer;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.items.core.ItemCoreAngel;
import com.xinyihl.constructionwandlegacy.items.core.ItemCoreDestruction;
import com.xinyihl.constructionwandlegacy.items.wand.ItemWand;
import com.xinyihl.constructionwandlegacy.items.wand.ItemWandBasic;
import com.xinyihl.constructionwandlegacy.items.wand.ItemWandInfinity;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ModItems {
    public static Item WAND_STONE;
    public static Item WAND_IRON;
    public static Item WAND_DIAMOND;
    public static Item WAND_INFINITY;

    public static Item CORE_ANGEL;
    public static Item CORE_DESTRUCTION;

    private ModItems() {
    }

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        WAND_STONE = register(event, "stone_wand", new ItemWandBasic(new ItemStack(Blocks.COBBLESTONE)), CreativeTabs.TOOLS);
        WAND_IRON = register(event, "iron_wand", new ItemWandBasic(new ItemStack(Items.IRON_INGOT)), CreativeTabs.TOOLS);
        WAND_DIAMOND = register(event, "diamond_wand", new ItemWandBasic(new ItemStack(Items.DIAMOND)), CreativeTabs.TOOLS);
        WAND_INFINITY = register(event, "infinity_wand", new ItemWandInfinity(), CreativeTabs.TOOLS);

        CORE_ANGEL = register(event, "core_angel", new ItemCoreAngel(), CreativeTabs.MISC);
        CORE_DESTRUCTION = register(event, "core_destruction", new ItemCoreDestruction(), CreativeTabs.MISC);

        ConfigServer.registerWandProperties(WAND_STONE, new ConfigServer.WandProperties(9, 131, 16, 9, true));
        ConfigServer.registerWandProperties(WAND_IRON, new ConfigServer.WandProperties(27, 250, 32, 27, true));
        ConfigServer.registerWandProperties(WAND_DIAMOND, new ConfigServer.WandProperties(81, 1561, 64, 81, true));
        ConfigServer.registerWandProperties(WAND_INFINITY, new ConfigServer.WandProperties(256, Integer.MAX_VALUE, 128, 256, true));
    }

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        registerModel(WAND_STONE);
        registerModel(WAND_IRON);
        registerModel(WAND_DIAMOND);
        registerModel(WAND_INFINITY);
        registerModel(CORE_ANGEL);
        registerModel(CORE_DESTRUCTION);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onItemColor(ColorHandlerEvent.Item event) {
        event.getItemColors().registerItemColorHandler((stack, tintIndex) -> {
            if (!(stack.getItem() instanceof ItemWand) || tintIndex != 1) {
                return -1;
            }
            return new WandOptions(stack).cores.get().getColor();
        }, WAND_STONE, WAND_IRON, WAND_DIAMOND, WAND_INFINITY);
    }

    private static Item register(RegistryEvent.Register<Item> event, String name, Item item, CreativeTabs tab) {
        item.setRegistryName(ConstructionWandLegacy.loc(name));
        item.setTranslationKey(Tags.MOD_ID + "." + name);
        item.setCreativeTab(tab);
        event.getRegistry().register(item);
        return item;
    }

    private static void registerModel(Item item) {
        if (item == null || item.getRegistryName() == null) {
            return;
        }
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}
