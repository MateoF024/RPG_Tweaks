# RPG-Tweaks

RPG-Tweaks is a mod for Minecraft NeoForge 1.21.1-1.21.8 that adds functionality for RPG servers/modpacks.

## 🚀 Features

#### **Advancement XP Blocking**
- Configurable custom XP reward per advancement
- Optional debug logs

#### **Award Custom XP When Mining Mineral Blocks**
- Award experience orbs (points) when mining ores.
- IDs or tags can be configured for each additional block. By default, tags are configured for vanilla minerals.
- Experience is calculated between a minimum and maximum value, awarding the amount randomly.
- Optional debug logs.

#### **Player Mechanics**
- **Exhaustion Rate:** Multiplier for hunger exhaustion gained from actions (1.0 = vanilla).
- **Natural Regen Rate:** Multiplier for HP regeneration from food (1.0 = vanilla).
- **Durability Multiplier:** Controls tool and armor wear. `-1` = no durability loss, `0` = vanilla, `>0` = damage multiplier.
- **Max Storable XP:** Cap on total experience a player can accumulate (0 = unlimited).
- **PvP Toggle:** Enable or disable player-vs-player damage via config or command.

#### **Sleep Mechanics**
- **Sleep From Night:** Minimum in-game night before players are allowed to sleep (0 = always).
- **Sleep Heal:** Restore a configurable percentage of max HP after sleeping (0 = disabled).
- **Sleep Hunger Loss:** Reduce food level after sleeping by a set number of points, with a configurable probability (0 = disabled).

#### **Dimension Blocking**
- Block player access to specific dimensions globally via config or command.
- Per-player exceptions: individually allow or block dimensions for specific players, overriding global rules.
- Pending exceptions: configure an exception for an offline player — it will be applied the next time they connect.
- Commands: `/rpg_tweaks dimension block/allow <dimension> <player>`
- Full GUI support in the config screen (add/remove global blocked dimensions and per-player exceptions).

#### **Custom Mob Loot (Loot Sacks)**
- Five tiers of loot sacks that drop from mobs: **Common**, **Uncommon**, **Rare**, **Epic**, and **Legendary**.
- Drop chances for each tier are fully configurable per mob via the config screen or JSON.
- Loot sacks can be opened by right-clicking to receive randomized loot from their respective loot table.
- Loot table contents are fully customizable via the data files included in the mod.
- Per-mob configuration also supports **removing specific vanilla drops** from a mob's loot table.
- Default configuration includes a zombie example with pre-configured drop chances and removed vanilla drops.

## 🔌 Integrations and Compatibility with other mods (ONLY 1.21.1)

> **Optional integrations:** The following integrations are only available when the respective mod is installed.  
> RPG-Tweaks does **not** bundle, redistribute, or modify these mods.

#### Integration with **Reskillable Reimagined**
- New command: `/rpg_tweaks skills add/remove <skill> <level> ... <item>`
- New command: `/rpg_tweaks craftskills add/remove <skill> <level> ... <item>`
- The `<item>` field is optional; if not specified, the desired item must be held in the main hand.

#### Integration with **Item Obliterator**
- New command: `/rpg_tweaks banitem/unbanitem` — Adds or removes an item from the mod's ban list.
- The item can be specified, or it will automatically use the item held in the main hand.

## 🖥️ Commands

| Command | Permission | Description |
|---|---|---|
| `/rpg_tweaks reload` | OP 2 | Reloads the mod configuration |
| `/rpg_tweaks pvp on\|off` | OP 2 | Enables or disables PvP |
| `/rpg_tweaks dimension block <dimension> <players>` | OP 2 | Block one or more players from entering a dimension |
| `/rpg_tweaks dimension allow <dimension> <players>` | OP 2 | Allow one or more players to enter a dimension (overrides global block) |
| `/rpg_tweaks skills add/remove/info` | OP 2 | Manage Reskillable skill locks |
| `/rpg_tweaks craftskills add/remove/info` | OP 2 | Manage Reskillable craft locks |
| `/rpg_tweaks banitem/unbanitem` | OP 2 | Manage Item Obliterator blacklist |

## 📦 Dependencies

### Required
- **Minecraft**: 1.21.1-1.21.8
- **NeoForge**: 21.1.219+
- **YetAnotherConfigLib**: 3.8.2+1.21.1-neoforge+
- **Java**: 21

### Optional (for integrations)(ONLY 1.21.1)
- **Reskillable Reimagined** (only required to use the Reskillable-related commands/integration)
- **Item Obliterator** (only required to use the banitem/unbanitem commands/integration)

## 🌍 Localization

RPG-Tweaks includes translations for:
- **English** (`en_us`)
- **Spanish** (`es_es`)

## ⚖️ Disclaimer / Not Affiliated

RPG-Tweaks is an independent project and is **not affiliated with, endorsed by, or sponsored by**
Reskillable Reimagined, Item Obliterator, NeoForge, or Mojang/Microsoft.

All product and mod names are trademarks of their respective owners.  
If you are a maintainer of any referenced mod and would like a link, name, or wording changed, please contact me.

## 📝 License

All Rights Reserved © 2026 MateoF24

## 👤 Author

**MateoF24**