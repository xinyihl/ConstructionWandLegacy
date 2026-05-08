package com.xinyihl.constructionwandlegacy.api;

public interface IWandCore extends IWandUpgrade {
    int getColor();

    IWandAction getWandAction();

    default IWandSupplier createSupplier(net.minecraft.entity.player.EntityPlayer player,
                                         com.xinyihl.constructionwandlegacy.basics.option.WandOptions options) {
        return null;
    }
}
