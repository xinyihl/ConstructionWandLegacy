package com.xinyihl.constructionwandlegacy.basics.option;

import com.xinyihl.constructionwandlegacy.ConstructionWandLegacy;
import com.xinyihl.constructionwandlegacy.api.IWandUpgrade;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;

public class WandUpgrades<T extends IWandUpgrade> {
    protected final NBTTagCompound tag;
    protected final String key;
    protected final ArrayList<T> upgrades;
    protected final T dval;

    public WandUpgrades(NBTTagCompound tag, String key, T dval) {
        this.tag = tag;
        this.key = key;
        this.dval = dval;

        upgrades = new ArrayList<>();
        if (dval != null) {
            upgrades.add(0, dval);
        }

        deserialize();
    }

    protected void deserialize() {
        NBTTagList listTag = tag.getTagList(key, Constants.NBT.TAG_STRING);
        boolean requireFix = false;

        for (int i = 0; i < listTag.tagCount(); i++) {
            String str = listTag.getStringTagAt(i);
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(str));

            T data;
            try {
                data = (T) item;
                upgrades.add(data);
            } catch (ClassCastException e) {
                ConstructionWandLegacy.LOGGER.warn("Invalid wand upgrade: {}", str);
                requireFix = true;
            }
        }
        if (requireFix) {
            serialize();
        }
    }

    protected void serialize() {
        NBTTagList listTag = new NBTTagList();
        for (T item : upgrades) {
            if (item == dval) {
                continue;
            }
            listTag.appendTag(new NBTTagString(item.getRegistryName().toString()));
        }
        tag.setTag(key, listTag);
    }

    public boolean addUpgrade(T upgrade) {
        if (hasUpgrade(upgrade)) {
            return false;
        }

        upgrades.add(upgrade);
        serialize();
        return true;
    }

    public boolean hasUpgrade(T upgrade) {
        return upgrades.contains(upgrade);
    }

    public ArrayList<T> getUpgrades() {
        return upgrades;
    }
}
