# UltiBot

[![Java 8](https://img.shields.io/badge/Java-8-orange)](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)
[![UltiTools-API](https://img.shields.io/badge/UltiTools--API-6.2.0-blue)](https://github.com/UltiKits/UltiTools-Reborn)
[![Paper 1.21.1](https://img.shields.io/badge/Paper-1.21.1-green)](https://papermc.io/)

假人模块 / Fake Player Module

基于 NMS 的假人模块，可以在服务器上生成和控制假人玩家。假人拥有真实玩家的完整行为能力，包括移动、交互、挖矿、战斗、聊天等。支持动作循环、宏录制/回放和自定义皮肤。

NMS-based fake player module for spawning and controlling server-side bots. Bots are real `ServerPlayer` instances with full player behavior — movement, interaction, mining, combat, chat, and more. Supports repeating actions, macro record/playback, and custom skins.

## 架构 / Architecture

UltiBot 采用多模块 Maven 结构，将 API 接口与 NMS 实现分离：

UltiBot uses a multi-module Maven architecture that separates API interfaces from NMS implementation:

| 模块 / Module | 说明 / Description |
|---------------|-------------------|
| `ultibot-api` | 纯接口定义（`BotPlayer`, `NMSBridge`, `ActionType`）/ Pure interfaces |
| `ultibot-core` | UltiTools 模块（服务、命令、配置、事件）/ UltiTools module (services, commands, config, events) |
| `ultibot-v1_21_R1` | Paper 1.21.1 NMS 实现 / NMS implementation for Paper 1.21.1 |
| `ultibot-dist` | 打包为最终 shaded JAR / Shaded distribution JAR |

## 功能 / Features

| 功能 | Feature | 说明 |
|------|---------|------|
| 假人生成 | Bot Spawning | 基于 NMS 的真实 ServerPlayer，完整加入服务器生命周期 |
| 移动控制 | Movement | 传送、行走、跳跃、蹲下、疾跑、视角转向 |
| 战斗与交互 | Combat & Interaction | 攻击、挖矿、使用物品、交互实体 |
| 循环动作 | Repeating Actions | 10 种动作类型，可设定间隔 tick 数自动循环 |
| 宏录制 | Macro System | 录制/回放/管理动作序列 |
| 自定义皮肤 | Custom Skins | 从 Mojang API 获取玩家皮肤应用到假人 |
| 聊天与命令 | Chat & Commands | 假人可发送聊天消息和执行命令 |
| 自动管理 | Auto Management | 主人下线自动移除、死亡自动重生 |
| 区块加载 | Chunk Loading | 可选让假人保持区块加载 |
| 前缀标识 | Bot Prefix | Tab 列表和聊天中显示可配置前缀 |

## 动作类型 / Action Types

`/bot action <name> <action> <interval>` 支持以下动作：

| 动作 | Action | 说明 |
|------|--------|------|
| `JUMP` | Jump | 跳跃 |
| `SNEAK` | Sneak | 蹲下 |
| `SPRINT` | Sprint | 疾跑 |
| `USE` | Use Item | 使用手持物品 |
| `ATTACK` | Attack | 攻击（空挥） |
| `MINE` | Mine | 挖矿（需朝向方块） |
| `DROP_ITEM` | Drop Item | 丢弃单个物品 |
| `DROP_STACK` | Drop Stack | 丢弃整组物品 |
| `DROP_INVENTORY` | Drop Inventory | 丢弃整个背包 |
| `LOOK_AT_NEAREST` | Look at Nearest | 看向最近实体 |

## 命令 / Commands

### 基础命令 / Basic Commands

| 命令 | 权限 | 说明 |
|------|------|------|
| `/bot spawn <name>` | `ultibot.use` | 在当前位置生成假人 / Spawn bot at your location |
| `/bot remove <name>` | `ultibot.use` | 移除指定假人 / Remove a bot |
| `/bot remove all` | `ultibot.use` | 移除所有假人 / Remove all bots |
| `/bot list` | `ultibot.use` | 列出所有假人 / List all bots |
| `/bot tp <name>` | `ultibot.use` | 传送假人到你的位置 / Teleport bot to you |
| `/bot reload` | `ultibot.admin` | 重载配置 / Reload config |

### 动作命令 / Action Commands

| 命令 | 权限 | 说明 |
|------|------|------|
| `/bot action <name> <action> <interval>` | `ultibot.action` | 开始循环动作 / Start repeating action |
| `/bot stop <name>` | `ultibot.action` | 停止所有动作 / Stop all actions |

### 工具命令 / Utility Commands

| 命令 | 权限 | 说明 |
|------|------|------|
| `/bot chat <name> <message>` | `ultibot.use` | 让假人发送聊天消息 / Make bot chat |
| `/bot cmd <name> <command>` | `ultibot.use` | 让假人执行命令 / Make bot run command |
| `/bot skin <name> <skinName>` | `ultibot.use` | 更换假人皮肤 / Change bot skin |

### 宏命令 / Macro Commands

| 命令 | 权限 | 说明 |
|------|------|------|
| `/bot macro record <name> <macroName>` | `ultibot.use` | 开始录制宏 / Start recording macro |
| `/bot macro stop <name>` | `ultibot.use` | 停止录制 / Stop recording |
| `/bot macro play <name> <macroName>` | `ultibot.use` | 回放宏 / Play macro |
| `/bot macro list` | `ultibot.use` | 列出所有宏 / List all macros |

## 配置 / Configuration

```yaml
# config.yml
# Maximum bots each player can spawn / 每个玩家最多生成的假人数
max-bots-per-player: 5

# Server-wide bot limit / 服务器假人总数限制
max-total-bots: 20

# Default skin name for bots / 假人默认皮肤
default-skin: Steve

# Enable bot ticking for physics / 启用假人物理 tick
tick-bots: true

# Allow bots to keep chunks loaded / 允许假人保持区块加载
allow-chunk-loading: false

# Prefix shown in chat/tab for bots / 假人聊天和Tab列表前缀
bot-prefix: "[Bot] "

# Remove bots when owner disconnects / 主人断开时自动移除假人
auto-remove-on-quit: true

# Auto-respawn bots after death / 假人死亡后自动重生
auto-respawn: true
```

## 权限 / Permissions

| 权限 | 说明 |
|------|------|
| `ultibot.use` | 基础假人命令（生成、移除、列表、传送、聊天、命令、皮肤、宏） |
| `ultibot.action` | 动作循环命令 |
| `ultibot.admin` | 管理命令（重载配置） |

## 支持版本 / Supported Versions

- **Paper 1.21.1** (NMS v1_21_R1)

NMS 实现按版本分离，添加新版本只需新建 `ultibot-v<version>` 模块实现 `NMSBridge` 接口。

NMS implementations are version-isolated. To add a new MC version, create a new `ultibot-v<version>` module implementing the `NMSBridge` interface.

## UltiTools-API Features Used

- `@ConfigEntity` / `@ConfigEntry` / `@Range` / `@NotEmpty` config validation
- `@CmdExecutor` / `@CmdMapping` / `@CmdParam` command system
- `UltiToolsPlugin.i18n()` bilingual translations (Chinese/English)
- `@Service` + constructor injection (IoC)
- `@UltiToolsModule` plugin registration

## 构建 / Build

```bash
cd Modules/UltiBot
mvn clean package
```

最终 JAR: `ultibot-dist/target/UltiBot-1.0.0.jar`

## 测试 / Testing

```bash
cd Modules/UltiBot
mvn test
```

111 tests, 80.2% instruction coverage (JaCoCo).
