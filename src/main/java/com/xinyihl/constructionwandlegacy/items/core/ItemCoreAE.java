package com.xinyihl.constructionwandlegacy.items.core;

import com.xinyihl.constructionwandlegacy.compat.CompatRegistrar;
import com.xinyihl.constructionwandlegacy.material.MaterialSourceFactory;
import com.xinyihl.constructionwandlegacy.wand.action.ActionConstruction;
import com.xinyihl.constructionwandlegacy.wand.action.WandAction;

public class ItemCoreAE extends ItemCore {
    @Override
    public int getColor() {
        return 0x3CA4FF;
    }

    @Override
    public WandAction getWandAction() {
        return ActionConstruction.INSTANCE;
    }

    @Override
    public MaterialSourceFactory getMaterialSourceFactory() {
        return CompatRegistrar.getAE2MaterialSourceFactory();
    }
}
