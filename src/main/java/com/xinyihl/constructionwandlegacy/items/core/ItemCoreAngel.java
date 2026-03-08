package com.xinyihl.constructionwandlegacy.items.core;

import com.xinyihl.constructionwandlegacy.api.IWandAction;
import com.xinyihl.constructionwandlegacy.wand.action.ActionAngel;

public class ItemCoreAngel extends ItemCore {
    @Override
    public int getColor() {
        return 0xE9B115;
    }

    @Override
    public IWandAction getWandAction() {
        return new ActionAngel();
    }
}
