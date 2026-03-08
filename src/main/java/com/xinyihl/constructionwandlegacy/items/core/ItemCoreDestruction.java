package com.xinyihl.constructionwandlegacy.items.core;

import com.xinyihl.constructionwandlegacy.api.IWandAction;
import com.xinyihl.constructionwandlegacy.wand.action.ActionDestruction;

public class ItemCoreDestruction extends ItemCore {
    @Override
    public int getColor() {
        return 0xFF0000;
    }

    @Override
    public IWandAction getWandAction() {
        return new ActionDestruction();
    }
}
