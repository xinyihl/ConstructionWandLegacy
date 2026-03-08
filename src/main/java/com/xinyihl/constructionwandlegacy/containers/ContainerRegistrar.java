package com.xinyihl.constructionwandlegacy.containers;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.containers.handlers.HandlerCapability;
import com.xinyihl.constructionwandlegacy.containers.handlers.HandlerShulkerbox;

public final class ContainerRegistrar {
    private ContainerRegistrar() {
    }

    public static void register() {
        ConstructionWandLegacy.instance.containerManager.register(new HandlerCapability());
        ConstructionWandLegacy.instance.containerManager.register(new HandlerShulkerbox());
    }
}
