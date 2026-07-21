package com.xinyihl.constructionwandlegacy.wand.upgrade;

import com.xinyihl.constructionwandlegacy.material.MaterialSourceFactory;
import com.xinyihl.constructionwandlegacy.wand.action.WandAction;

public interface IWandCore extends IWandUpgrade {
    int getColor();

    WandAction getWandAction();

    default MaterialSourceFactory getMaterialSourceFactory() {
        return null;
    }
}
