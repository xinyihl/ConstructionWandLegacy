package com.xinyihl.constructionwandlegacy.compat;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.compat.containers.handlers.HandlerCapability;
import com.xinyihl.constructionwandlegacy.compat.containers.handlers.HandlerShulkerbox;
import com.xinyihl.constructionwandlegacy.compat.inventory.handlers.HandlerBaubles;
import com.xinyihl.constructionwandlegacy.compat.inventory.handlers.HandlerPlayer;
import com.xinyihl.constructionwandlegacy.compat.inventory.handlers.HandlerProjectE;
import net.minecraftforge.fml.common.Loader;

public final class CompatRegistrar {
    private CompatRegistrar() {
    }

    public static void register() {
        ConstructionWandLegacy.instance.containerManager.register(new HandlerCapability());
        ConstructionWandLegacy.instance.containerManager.register(new HandlerShulkerbox());
        ConstructionWandLegacy.instance.inventoryManager.register(new HandlerPlayer());
        if (Loader.isModLoaded("baubles")) {
            ConstructionWandLegacy.instance.inventoryManager.register(new HandlerBaubles());
        }
        if (Loader.isModLoaded("projecte")) {
            ConstructionWandLegacy.instance.inventoryManager.register(new HandlerProjectE());
        }
    }
}
