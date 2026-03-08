package com.xinyihl.constructionwandlegacy.items.core;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.api.IWandAction;
import com.xinyihl.constructionwandlegacy.api.IWandCore;
import com.xinyihl.constructionwandlegacy.wand.action.ActionConstruction;
import net.minecraft.util.ResourceLocation;

public class CoreDefault implements IWandCore {
    @Override
    public int getColor() {
        return -1;
    }

    @Override
    public IWandAction getWandAction() {
        return new ActionConstruction();
    }

    @Override
    public ResourceLocation getRegistryName() {
        return ConstructionWandLegacy.loc("default");
    }
}
