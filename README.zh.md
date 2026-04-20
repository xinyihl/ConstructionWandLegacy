# ConstructionWandLegacy

Minecraft 1.12.2 上的 Construction Wand 功能回移植（Forge）。

## 已实现内容

### 物品与核心
- 4 种手杖：Stone / Iron / Diamond / Infinity
- 2 种核心：Angel / Destruction
- 核心覆盖层模型与着色（装备核心后手杖外观变化）

### 放置与破坏逻辑
- 建筑模式（Construction）
- 天使模式（Angel）：支持空中放置
- 破坏模式（Destruction）
- 可选 Baubles 兼容：饰品栏中已装备的容器物品也可以提供建筑方块
- 可选 ProjectE 兼容：可以直接消耗 EMC 来建造方块的建筑之杖
- 使用接近原版的交互链路：
  - 放置走 `ItemBlock.placeBlockAt`
  - 破坏走 `removedByPlayer` + `onPlayerDestroy`
  - 集成 Forge Place/Break 事件

### 升级与选项
- 核心安装升级（手杖 + 核心在合成网格组合）
- 可切换选项（锁定/方向/替换/匹配/随机/核心）
- 手杖 GUI（空中右键组合键打开）

### 撤销与预览
- 撤销历史记录
- 撤销预览同步（按键查询）
- 撤销后预览自动刷新
- 预览颜色：
  - Destruction 核心为红色
  - Undo 预览为绿色
  - Angel 核心支持空气目标预览

### 资源与本地化
- 完整物品模型与贴图
- `en_us.lang` / `zh_cn.lang`

## 默认操作说明

以下为当前实现的默认交互：

- `Shift + Ctrl + 鼠标滚轮`：切换锁定模式
- `Shift + Ctrl + 左键空挥`：切换核心
- `Shift + Ctrl + 右键空气`：打开手杖配置 GUI
- `Shift + Ctrl`（按住）：显示可撤销预览
- `Shift + 右键方块`（手持手杖）：执行撤销

> 注意：GUI 仅在“右键空气”时打开，避免与右键方块撤销冲突。

## 配置文件

首次启动后会生成配置文件（Forge 默认 `config/constructionwandlegacy.cfg`）。

### 可配置项

- `wandLimits.stoneWandMaxBlocks`：石手杖默认最大放置数量
- `wandLimits.ironWandMaxBlocks`：铁手杖默认最大放置数量
- `wandLimits.diamondWandMaxBlocks`：钻石手杖默认最大放置数量
- `wandLimits.infinityWandMaxBlocks`：无尽手杖默认最大放置数量
- `placement.allowTileEntityPlacement`：是否允许手杖放置带 TileEntity 的方块
- `placement.blockWhitelist`：放置白名单（为空表示不启用白名单）
- `placement.blockBlacklist`：放置黑名单
- `placement.propertyCopyWhitelist`：`TARGET` 模式下允许复制的属性名关键字白名单（如 `facing`、`axis`）

白名单/黑名单条目格式：

- `modid:block`（匹配该方块所有变体）
- `modid:block@meta`（只匹配指定 meta）

示例：

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

## 开发构建

### 环境要求
- 推荐使用 JDK 17 运行 Gradle（项目会使用 Java Toolchain 编译到 Java 8 目标）
- Windows 下使用 `gradlew.bat`，Linux/macOS 使用 `./gradlew`

### 常用命令

```bash
# 编译源码
./gradlew compileJava

# 处理资源
./gradlew processResources

# 构建产物
./gradlew build

# 运行开发客户端
./gradlew runClient
```