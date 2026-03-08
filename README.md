# ConstructionWandLegacy

Backport of Construction Wand features for Minecraft 1.12.2 (Forge).

## Implemented Features

### Items and Core Modules
- 4 wand types: Stone / Iron / Diamond / Infinity
- 2 cores: Angel / Destruction
- Core overlay model and tinting (wand appearance changes after installing a core)

### Placement and Destruction Logic
- Construction mode
- Angel mode: supports mid-air placement
- Destruction mode
- Uses an interaction flow close to the original implementation:
  - Placement goes through `ItemBlock.placeBlockAt`
  - Breaking goes through `removedByPlayer` + `onPlayerDestroy`
  - Integrated with Forge Place/Break events

### Upgrades and Options
- Core installation upgrade (combine wand + core in the crafting grid)
- Toggleable options (lock/direction/replace/match/random/core)
- Wand GUI (open with key combo while right-clicking in air)

### Undo and Preview
- Undo history
- Undo preview sync (triggered by key query)
- Automatic preview refresh after undo
- Preview colors:
  - Destruction core: red
  - Undo preview: green
  - Angel core supports air-target preview

### Assets and Localization
- Complete item models and textures
- `en_us.lang` / `zh_cn.lang`

## Default Controls

Current default interactions:

- `Shift + Ctrl + Mouse Wheel`: toggle lock mode
- `Shift + Ctrl + Left Click` (swing in air): switch core
- `Shift + Ctrl + Right Click` (in air): open wand config GUI
- Hold `Shift + Ctrl`: show undo preview
- `Shift + Right Click` on block (while holding wand): perform undo

> Note: The GUI only opens when right-clicking in air, to avoid conflicts with block right-click undo.

## Configuration File

A config file is generated after first launch (Forge default: `config/constructionwandlegacy.cfg`).

### Configurable Options

- `wandLimits.stoneWandMaxBlocks`: default max placement count for Stone Wand
- `wandLimits.ironWandMaxBlocks`: default max placement count for Iron Wand
- `wandLimits.diamondWandMaxBlocks`: default max placement count for Diamond Wand
- `wandLimits.infinityWandMaxBlocks`: default max placement count for Infinity Wand
- `placement.allowTileEntityPlacement`: whether wand placement of TileEntity blocks is allowed
- `placement.blockWhitelist`: placement whitelist (empty means whitelist disabled)
- `placement.blockBlacklist`: placement blacklist
- `placement.propertyCopyWhitelist`: keyword whitelist of property names allowed to copy in `TARGET` mode (e.g. `facing`, `axis`)

Whitelist/blacklist entry formats:

- `modid:block` (matches all variants of the block)
- `modid:block@meta` (matches only the specific meta)

Example:

```cfg
placement {
  B:allowTileEntityPlacement=false
  S:propertyCopyWhitelist <
    facing
    axis
    rotation
    half
    hinge
    shape
    part
    face
   >
  S:blockWhitelist <
    minecraft:stone
    minecraft:stained_hardened_clay@14
   >
  S:blockBlacklist <
    minecraft:chest
    minecraft:mob_spawner
   >
}

wandLimits {
  I:stoneWandMaxBlocks=9
  I:ironWandMaxBlocks=27
  I:diamondWandMaxBlocks=81
  I:infinityWandMaxBlocks=256
}
```

## Development Build

### Requirements
- JDK 17 is recommended to run Gradle (the project uses Java Toolchain to compile to Java 8 target)
- Use `gradlew.bat` on Windows, and `./gradlew` on Linux/macOS

### Common Commands

```bash
# Compile source code
./gradlew compileJava

# Process resources
./gradlew processResources

# Build artifacts
./gradlew build

# Run development client
./gradlew runClient
```
