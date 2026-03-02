# ConstructionWandLgeacy

Minecraft 1.12.2 上的 Construction Wand 功能回移植（Forge）。

> 当前仓库为开发中版本，目标是尽量保持与高版本 Construction Wand 的交互与行为一致。

## 项目信息

- Mod ID: `constructionwandlgeacy`
- Minecraft: `1.12.2`
- 构建系统: Gradle + RetroFuturaGradle
- 当前版本: `0.0.1`

## 已实现内容

### 物品与核心
- 4 种手杖：Stone / Iron / Diamond / Infinity
- 2 种核心：Angel / Destruction
- 核心覆盖层模型与着色（装备核心后手杖外观变化）

### 放置与破坏逻辑
- 建筑模式（Construction）
- 天使模式（Angel）：支持空中放置
- 破坏模式（Destruction）
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