package com.xinyihl.constructionwandlegacy.items.core;

import com.xinyihl.constructionwandlegacy.api.IWandAction;
import com.xinyihl.constructionwandlegacy.api.IWandSupplier;
import com.xinyihl.constructionwandlegacy.basics.option.WandOptions;
import com.xinyihl.constructionwandlegacy.wand.action.ActionConstruction;
import com.xinyihl.constructionwandlegacy.wand.supplier.SupplierAE;
import net.minecraft.entity.player.EntityPlayer;

public class ItemCoreAE extends ItemCore {
    @Override
    public int getColor() {
        return 0x00ACC1;
    }

    @Override
    public IWandAction getWandAction() {
        return new ActionConstruction();
    }

    @Override
    public IWandSupplier createSupplier(EntityPlayer player, WandOptions options) {
        return new SupplierAE(player, options);
    }
}
