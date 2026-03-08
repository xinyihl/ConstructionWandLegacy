package com.xinyihl.constructionwandlegacy.basics.config;

import com.xinyihl.constructionwandlegacy.Tags;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
@Config(modid = Tags.MOD_ID, name = Tags.MOD_NAME)
public class Configurations {
    @Config.Name("wandLimits")
    public static final WandLimits wandLimits = new WandLimits();

    @Config.Name("placement")
    public static final Placement placement = new Placement();

    @Config.Name("matching")
    public static final Matching matching = new Matching();

    @SubscribeEvent
    public static void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(Tags.MOD_ID)) {
            ConfigManager.sync(Tags.MOD_ID, Config.Type.INSTANCE);
            ConfigServer.sync();
        }
    }

    public static final class WandLimits {
        @Config.RangeInt(min = 1, max = 4096)
        @Config.Comment("Default max placement count for Stone Wand")
        public int stoneWandMaxBlocks = 9;

        @Config.RangeInt(min = 1, max = 4096)
        @Config.Comment("Default max placement count for Iron Wand")
        public int ironWandMaxBlocks = 27;

        @Config.RangeInt(min = 1, max = 4096)
        @Config.Comment("Default max placement count for Diamond Wand")
        public int diamondWandMaxBlocks = 81;

        @Config.RangeInt(min = 1, max = 4096)
        @Config.Comment("Default max placement count for Infinity Wand")
        public int infinityWandMaxBlocks = 256;
    }

    public static final class Placement {
        @Config.Comment("Allow placing blocks with TileEntity via wand")
        public boolean allowTileEntityPlacement = false;

        @Config.Comment("Placement whitelist entries. Format: modid:block or modid:block@meta. Empty = disabled.")
        public String[] blockWhitelist = new String[0];

        @Config.Comment("Placement blacklist entries. Format: modid:block or modid:block@meta.")
        public String[] blockBlacklist = new String[0];

        @Config.Comment("Whitelist keywords for TARGET mode property copy. Property name containing any keyword will be copied.")
        public String[] propertyCopyWhitelist = new String[]{"facing", "axis", "rotation", "half", "hinge", "shape", "part", "face"};
    }

    public static final class Matching {
        @Config.Comment("Similar block matching groups for SIMILAR mode, entries separated by ';', e.g. minecraft:dirt;minecraft:grass")
        public String[] similarBlocks = new String[0];
    }
}
