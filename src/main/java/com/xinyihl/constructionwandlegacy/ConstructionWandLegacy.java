package com.xinyihl.constructionwandlegacy;

import com.xinyihl.constructionwandlegacy.basics.CommonEvents;
import com.xinyihl.constructionwandlegacy.basics.ReplacementRegistry;
import com.xinyihl.constructionwandlegacy.basics.config.ConfigServer;
import com.xinyihl.constructionwandlegacy.client.ClientEvents;
import com.xinyihl.constructionwandlegacy.client.RenderBlockPreview;
import com.xinyihl.constructionwandlegacy.compat.CompatRegistrar;
import com.xinyihl.constructionwandlegacy.compat.containers.ContainerManager;
import com.xinyihl.constructionwandlegacy.compat.inventory.InventoryManager;
import com.xinyihl.constructionwandlegacy.network.ModMessages;
import com.xinyihl.constructionwandlegacy.wand.undo.UndoHistory;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class ConstructionWandLegacy {

    @Mod.Instance(Tags.MOD_ID)
    public static ConstructionWandLegacy instance;

    public static Logger LOGGER;

    public ContainerManager containerManager;
    public InventoryManager inventoryManager;
    public UndoHistory undoHistory;
    private CommonEvents commonEvents;

    public static ResourceLocation loc(String name) {
        return new ResourceLocation(Tags.MOD_ID, name);
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        ConfigServer.sync();
        containerManager = new ContainerManager();
        inventoryManager = new InventoryManager();
        undoHistory = new UndoHistory();
        commonEvents = new CommonEvents();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(commonEvents);

        if (event.getSide() == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(new ClientEvents());
            RenderBlockPreview renderBlockPreview = new RenderBlockPreview();
            MinecraftForge.EVENT_BUS.register(renderBlockPreview);
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        ReplacementRegistry.init();
        CompatRegistrar.register();
        ModMessages.register();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        if (commonEvents != null) {
            commonEvents.onServerStarting(event);
        }
    }
}
