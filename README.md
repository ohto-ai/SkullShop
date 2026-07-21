# SkullShop 🎭

[![Version](https://img.shields.io/badge/version-1.0.0-blue)](https://github.com/ohto-ai/SkullShop)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.6+-brightgreen)](https://papermc.io)
[![Platform](https://img.shields.io/badge/platform-Paper-purple)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

*A Paper plugin for buying player heads with multiple economy options — GUI-driven, lightweight, and fully customizable.*

**一款支持多种经济系统的 Paper 玩家头颅商店插件 — GUI 交互、轻量级、完全可定制。**

---

## Features / 功能

| Feature | 功能 |
|---|---|
| **3 Economy Types** — Vault (coins), PlayerPoints (points), Custom NBT Items | **3 种经济系统** — Vault 金币、PlayerPoints 点券、自定义 NBT 物品货币 |
| **Interactive GUI** — Choose payment method, select quantity, preview head skin | **交互式 GUI** — 选择支付方式、数量，预览头颅皮肤 |
| **Direct Command Purchase** — `/hs buy <player> via <type> [amount]` | **命令直接购买** — 支持一键购买无需打开 GUI |
| **Multi-Language** — English & Simplified Chinese, easy to add more | **多语言支持** — 英文 & 简体中文，易于扩展 |
| **Runtime Config** — Hot-reload, toggle currencies, change prices without restart | **热重载** — 重载配置、开关货币、修改价格无需重启 |
| **Async Skin Loading** — Paper PlayerProfile API with Mojang fallback | **异步皮肤加载** — Paper API + Mojang API 回退 |
| **Admin Controls** — Allow/disallow buying other players' heads | **管理员控制** — 可禁止购买他人头颅 |

---

## Requirements / 环境要求

| Requirement | 要求 |
|---|---|
| Paper 1.20.6+ | **Server / 服务端** |
| Java 21+ | **Java** |
| Vault *(optional)* | For coin economy / 金币经济 |
| PlayerPoints *(optional)* | For points economy / 点券经济 |

---

## Installation / 安装

1. Download `SkullShop-1.0.0.jar` from [Releases](https://github.com/ohto-ai/SkullShop/releases).
   从 [Releases](https://github.com/ohto-ai/SkullShop/releases) 下载最新版本。
2. Place it in your server's `plugins/` folder.
   放入服务器的 `plugins/` 文件夹。
3. Restart the server (or run `/reload confirm`).
   重启服务器（或执行 `/reload confirm`）。
4. Edit `plugins/SkullShop/config.yml` to configure prices and currencies.
   编辑 `plugins/SkullShop/config.yml` 配置价格和货币。

---

## Commands / 命令

All commands use `/hs` (alias: `/skullshop`).
所有命令使用 `/hs`（别名：`/skullshop`）。

### Player Commands / 玩家命令 — `skullshop.use`

| Command | Description |
|---|---|
| `/hs buy` | Open GUI to buy your own head / 打开 GUI 购买自己的头颅 |
| `/hs buy <player>` | Open GUI to buy another player's head / 打开 GUI 购买其他玩家的头颅 |
| `/hs buy [player] via <vault\|player_points\|item> [1\|8\|64]` | Direct purchase with quantity / 指定数量和货币直接购买 |
| `/hs price [vault\|player_points\|item]` | View prices / 查看各货币价格 |
| `/hs help` | Show help / 显示帮助 |

### Admin Commands / 管理员命令 — `skullshop.admin`

| Command | Description |
|---|---|
| `/hs price <vault\|player_points\|item> <amount>` | Set price / 设置价格 |
| `/hs currency <vault\|player_points\|item> <enable\|disable>` | Toggle currency / 开关货币类型 |
| `/hs allowother <on\|off>` | Allow/deny buying other players' heads / 允许/禁止购买他人头颅 |
| `/hs setitem` | Set held item as currency reference (records full NBT) / 将手持物品设为货币参考物品 |
| `/hs reload` | Reload config & messages / 重载配置和语言文件 |

---

## Permissions / 权限

| Permission | Scope | Default |
|---|---|---|
| `skullshop.use` | `/hs buy`, `/hs price`, `/hs help` | `true` (all players) |
| `skullshop.admin` | All admin subcommands | `op` |

---

## Economy Systems / 经济系统

### 1. Vault (金币)

Uses the server's Vault-compatible economy plugin (e.g., EssentialsX). Players pay in-game coins.

```yaml
economy:
  vault:
    enabled: true
    price: 100.0
    display_name: "金币"
```

### 2. PlayerPoints (点券)

Uses the PlayerPoints plugin. Players pay with points.

```yaml
economy:
  player_points:
    enabled: false
    price: 150
    display_name: "点券"
```

### 3. Custom Item (物品货币)

Any NBT item can serve as currency. Hold the item and run `/hs setitem` to configure. All NBT tags are recorded for exact matching.

```yaml
economy:
  item:
    enabled: false
    price: 1
    display_name: "货币物品"
    reference_item: {}  # Set automatically by /hs setitem
```

---

## Configuration / 配置

Config file: `plugins/SkullShop/config.yml`

```yaml
# Whether players can buy other players' heads
allow_other_players: true

# Language: "zh" for Chinese, "en" for English
language: "zh"

# Economy systems — each independently toggleable
economy:
  vault:
    enabled: true
    price: 100.0
    display_name: "金币"
  player_points:
    enabled: false
    price: 150
    display_name: "点券"
  item:
    enabled: false
    price: 1
    display_name: "货币物品"
    reference_item: {}
```

### Adding a New Language / 添加新语言

1. Copy `plugins/SkullShop/messages_en.yml` to `messages_<lang>.yml`
2. Translate all values (keep the YAML keys and `&` color codes)
3. Set `language: "<lang>"` in `config.yml`
4. Run `/hs reload`

---

## GUI Preview / GUI 预览

```
┌──────────────────────────────┐
│    Select Payment Method      │
│                              │
│   [Player Head Preview]       │
│                              │
│  [💰 Vault]   Price: 100     │
│  [💎 Points]  Price: 150     │
│  [📦 Item]    Price: 1       │
│                              │
└──────────────────────────────┘

┌──────────────────────────────┐
│    Buy <Player>'s Head       │
│                              │
│   [Player Head Preview]       │
│                              │
│  [x1]  Total: 100  Balance   │
│  [x8]  Total: 800  Balance   │
│  [x64] Total: 6400 Balance   │
│                              │
│        [← Back]              │
└──────────────────────────────┘
```

---

## FAQ / 常见问题

**Q: Can players buy heads of offline players?**
A: Yes! The plugin uses Mojang API as fallback for players who have never joined the server.

**Q: 能购买离线玩家的头颅吗？**
A: 可以！插件会通过 Mojang API 异步获取从未上过线的玩家皮肤。

**Q: How do I add more languages?**
A: Copy `messages_en.yml`, rename to `messages_<lang>.yml`, translate, and set `language` in config.

**Q: 如何添加更多语言？**
A: 复制 `messages_en.yml`，重命名为 `messages_<语言>.yml`，翻译后在配置中修改 `language`。

**Q: Does it work on Spigot/Bukkit?**
A: It's built for Paper 1.20.6+. May work on Spigot but is untested.

**Q: 支持 Spigot/Bukkit 吗？**
A: 基于 Paper 1.20.6+ 构建，可能兼容 Spigot 但未测试。

---

## Links / 链接

- **Source Code / 源码**: [GitHub](https://github.com/ohto-ai/SkullShop)
- **Issues / 问题反馈**: [GitHub Issues](https://github.com/ohto-ai/SkullShop/issues)
- **Releases / 下载**: [GitHub Releases](https://github.com/ohto-ai/SkullShop/releases)

---

## License / 许可证

MIT License — see [LICENSE](LICENSE) for details.
详见 [LICENSE](LICENSE)。
