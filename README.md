# RPG-Tweaks

<div align="center">

[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1476807?logo=curseforge&label=CurseForge&color=F16436&style=for-the-badge)](https://www.curseforge.com/minecraft/mc-mods/rpg-tweaks)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/rpg_tweaks?logo=modrinth&label=Modrinth&color=00AF5C&style=for-the-badge)](https://modrinth.com/mod/rpg_tweaks)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1--1.21.8-62B47A?logo=minecraft&style=for-the-badge)](https://minecraft.net)
[![NeoForge](https://img.shields.io/badge/Loader-NeoForge-D16600?logo=neoforge&style=for-the-badge)](https://neoforged.net)

</div>

RPG-Tweaks is a mod for Minecraft NeoForge 1.21.1-1.21.8 that adds functionality for RPG servers/modpacks.

## 🚀 Features

#### **Advancement XP Blocking**
- Configurable custom XP reward per advancement, split by type: **Task**, **Goal**, and **Challenge**
- Optional debug logs

#### **Award Custom XP When Mining Blocks**
- Award experience orbs (points) when mining blocks.
- IDs or tags can be configured for each additional block. By default, tags are configured for vanilla ores.
- Experience is calculated between a minimum and maximum value, awarding the amount randomly.
- Optional **Fortune bonus**: configurable XP multiplier applied per Fortune enchantment level.
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
- **Quest-gated Sleep:** Optionally require players to claim a specific FTB Quests reward before being allowed to sleep. Each player is checked individually. Requires FTB Quests.

#### **Dimension Blocking**
- Block player access to specific dimensions globally via config or command.
- **Custom block message** per dimension (overrides default message).
- Per-player exceptions: individually allow or block dimensions for specific players, overriding global rules.
- Pending exceptions: configure an exception for an offline player — it will be applied the next time they connect.
- Commands: `/rpg_tweaks dimension block/allow <dimension> <player>`
- Full GUI support in the config screen (add/remove global blocked dimensions and per-player exceptions).

#### **Chat Blocking**
- Globally disable chat for all players, or block specific players from chatting individually.
- Commands: `/rpg_tweaks chat on/off` and `/rpg_tweaks chat block/allow <player>`

#### **Death XP Loss**
- Players lose a configurable percentage of their total XP on death. The retained XP is dropped as orbs at the death location and can be recovered.
- Configurable via the config screen.

#### **Death Respawn Near Death Location**
- On death, players respawn at a random location near where they died (Overworld only).
- Minimum and maximum respawn distance are independently configurable.

#### **Custom Mob Loot (Loot Sacks)**
- Five tiers of loot sacks that drop from mobs: **Common**, **Uncommon**, **Rare**, **Epic**, and **Legendary**.
- Drop chances for each tier are fully configurable per mob via the config screen or JSON.
- Each tier supports advanced conditional drops:
  - **Required dimension** — only drops in a specific dimension.
  - **Required biome** — only drops in a specific biome.
  - **Night only** — only drops at night.
  - **Moon phases** — only drops during specific moon phases.
  - **Minimum distance from player** — only drops if the mob was killed far enough away.
  - **Surface / Cave only** — restrict drops to above or below the surface.
- Optional **Looting bonus**: configurable percentage added to drop chances per Looting enchantment level (evaluated highest-tier first, always yields at most one sack).
- Loot sacks can be opened by right-clicking to receive randomized loot from their respective loot table.
- Loot table contents are fully customizable via the data files included in the mod.
- Per-mob configuration also supports **removing specific vanilla drops** from a mob's loot table.
- Default configuration includes a zombie example with pre-configured drop chances and removed vanilla drops.

#### **Mob XP on Kill**
- Award a configurable amount of XP when killing mobs. Configured per mob alongside the loot sack settings.
- Optional **Looting bonus**: XP multiplier applied per Looting enchantment level.

## 🔌 Integrations and Compatibility with other mods (ONLY 1.21.1)

> **Optional integrations:** The following integrations are only available when the respective mod is installed.  
> RPG-Tweaks does **not** bundle, redistribute, or modify these mods.

#### Integration with **[Reskillable Reimagined](https://www.curseforge.com/minecraft/mc-mods/reskillable-reimagined)**
- **Skill-up Toast Notifications:** When a player levels up a skill (including custom skills), any items unlocked at that level are displayed as in-game toast notifications showing the item icon and the skill/level that unlocked it.
- **Progression Book:** Press `L` in-game to open a book-style GUI displaying all items configured in `skill_locks.json`, organized by skill category. Each item shows its icon, name, required skill level, and whether the current player has already unlocked it.
- New command: `/rpg_tweaks skills add/remove/info <skill> <level> ... <item>`
- New command: `/rpg_tweaks craftskills add/remove/info <skill> <level> ... <item>`
- New command: `/rpg_tweaks attackskills add/remove/info <skill> <level> ... <entity>` — manages entity attack locks (`attack_skill_locks.json`).
- The `<item>` field is optional in skills/craftskills; if not specified, the desired item must be held in the main hand.
- Full support for **custom skills** defined in `custom_skills.json`.

#### Integration with **[FTB Quests](https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge)**
- **Quest-gated dimension access:** Configure a Reward ID so that players who claim that reward gain personal access to a globally blocked dimension. Players who haven't completed the quest remain restricted.
- **Quest-gated loot sack tiers:** Require a specific Quest ID to be completed before Epic and/or Legendary sacks can drop for a player.
- **Quest-gated sleep:** Configure a Reward ID that players must individually claim before being allowed to sleep.
- **Skill rewards on quest completion:** Configure a Reward ID so that when a player claims that reward, their Reskillable skills are automatically increased by the configured amount. Requires Reskillable Reimagined to be installed.

#### Integration with **[Item Obliterator](https://modrinth.com/mod/item-obliterator)**
- New command: `/rpg_tweaks banitem/unbanitem` — Adds or removes an item from the mod's ban list.
- The item can be specified, or it will automatically use the item held in the main hand.

## 🖥️ Commands

| Command | Permission | Description |
|---|---|---|
| `/rpg_tweaks config reload` | OP 2 | Reloads the mod configuration |
| `/rpg_tweaks config reset <category>` | OP 2 | Reset config or a specific category |
| `/rpg_tweaks pvp on\|off` | OP 2 | Enables or disables PvP |
| `/rpg_tweaks chat on\|off` | OP 2 | Enables or disables global chat |
| `/rpg_tweaks chat block/allow <player>` | OP 2 | Block or allow a specific player from chatting |
| `/rpg_tweaks dimension block <dimension> <players>` | OP 2 | Block one or more players from entering a dimension |
| `/rpg_tweaks dimension allow <dimension> <players>` | OP 2 | Allow one or more players to enter a dimension (overrides global block) |
| `/rpg_tweaks skills add/remove/info` | OP 2 | Manage Reskillable skill locks |
| `/rpg_tweaks craftskills add/remove/info` | OP 2 | Manage Reskillable craft locks |
| `/rpg_tweaks attackskills add/remove/info` | OP 2 | Manage Reskillable entity attack locks |
| `/rpg_tweaks banitem/unbanitem` | OP 2 | Manage Item Obliterator blacklist |

## 📦 Dependencies

### Required
- **Minecraft**: 1.21.1-1.21.8
- **NeoForge**: 21.1.219+
- **[YetAnotherConfigLib](https://modrinth.com/mod/yacl)**: 3.8.2+1.21.1-neoforge+
- **Java**: 21

### Optional (for integrations)(ONLY 1.21.1)
- **[Reskillable Reimagined](https://www.curseforge.com/minecraft/mc-mods/reskillable-reimagined)** (only required to use the Reskillable-related commands/integration)
- **[FTB Quests](https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge)** (only required to use the FTB Quests integration)
- **[Item Obliterator](https://modrinth.com/mod/item-obliterator)** (only required to use the banitem/unbanitem commands/integration)

## 🌍 Localization

RPG-Tweaks includes translations for:
- **English** (`en_us`)
- **Spanish** (`es_es`)

## ⚖️ Disclaimer / Not Affiliated

RPG-Tweaks is an independent project and is **not affiliated with, endorsed by, or sponsored by**
Reskillable Reimagined, FTB Quests, Item Obliterator, NeoForge, or Mojang/Microsoft.

All product and mod names are trademarks of their respective owners.  
If you are a maintainer of any referenced mod and would like a link, name, or wording changed, please contact me.

## 📝 License

All Rights Reserved © 2026 MateoF24

_It can be used for Modpacks, only attribution is requested._

## 👤 Author

**MateoF24**