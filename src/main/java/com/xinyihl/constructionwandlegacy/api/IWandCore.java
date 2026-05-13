package com.xinyihl.constructionwandlegacy.api;

import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import net.minecraft.entity.player.EntityPlayer;

public interface IWandCore extends IWandUpgrade {
    int getColor();

    IWandAction getWandAction();

    default IWandSupplier createSupplier(EntityPlayer player, WandOptions options) {
        return null;
    }
}
