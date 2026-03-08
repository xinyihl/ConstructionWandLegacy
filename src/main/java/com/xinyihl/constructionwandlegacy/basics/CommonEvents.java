package com.xinyihl.constructionwandlegacy.basics;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class CommonEvents {
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (ConstructionWandLegacy.instance != null && ConstructionWandLegacy.instance.undoHistory != null && event.player != null) {
            ConstructionWandLegacy.instance.undoHistory.clearHistory(event.player.getUniqueID());
        }
    }

    public void onServerStarting(FMLServerStartingEvent event) {
        ReplacementRegistry.init();
    }
}
