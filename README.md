# RPG-Tweaks

RPG-Tweaks is a mod for Minecraft NeoForge 1.21.1 that adds functionality for RPG servers/modpacks.

## 🚀 Features

#### **Advancement XP Blocking**
- Prevents players from gaining experience when completing achievements (advancements)
- Configurable with optional debug logs

#### **Award Custom XP When Mining Mineral Blocks**
- Award experience orbs (points) when mining ores.
- IDs or tags can be configured for each additional block. By default, tags are configured for vanilla minerals.
- Experience is calculated between a minimum and maximum value, awarding the amount randomly.
- Optional debug logs.

## 🔌 Integrations and Compatibility with other mods

> **Optional integrations:** The following integrations are only available when the respective mod is installed.  
> RPG-Tweaks does **not** bundle, redistribute, or modify these mods.

#### Integration with **Reskillable Reimagined**
- New command: `/rpg_tweaks skills add/remove <skill> <level> ... <item>`
- New command: `/rpg_tweaks craftskills add/remove <skill> <level> ... <item>`
- The `<item>` field is optional; if not specified, the desired item must be held in the main hand.

#### Integration with **Item Obliterator**
- New command: `/rpg_tweaks banitem/unbanitem` — Adds or removes an item from the mod's ban list.
- The item can be specified, or it will automatically use the item held in the main hand.

## 📦 Dependencies

### Required
- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.219+
- **YetAnotherConfigLib**: 3.8.2+1.21.1-neoforge
- **Java**: 21

### Optional (for integrations)
- **Reskillable Reimagined** (only required to use the Reskillable-related commands/integration)
- **Item Obliterator** (only required to use the banitem/unbanitem commands/integration)

## ⚖️ Disclaimer / Not Affiliated

RPG-Tweaks is an independent project and is **not affiliated with, endorsed by, or sponsored by**
Reskillable Reimagined, Item Obliterator, NeoForge, or Mojang/Microsoft.

All product and mod names are trademarks of their respective owners.  
If you are a maintainer of any referenced mod and would like a link, name, or wording changed, please contact me.

## 📝 License

All Rights Reserved © 2026 MateoF24

## 👤 Author

**MateoF24**