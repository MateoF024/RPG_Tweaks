package org.mateof24.rpg_tweaks.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.mateof24.rpg_tweaks.RPG_Tweaks;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RPG_Tweaks.MODID);

    public static final DeferredItem<LootSackItem> LOOT_SACK_COMMON = ITEMS.register("loot_sack_common",
            () -> new LootSackItem(new Item.Properties().stacksTo(16),
                    ResourceKey.create(Registries.LOOT_TABLE,
                            ResourceLocation.fromNamespaceAndPath(RPG_Tweaks.MODID, "loot_sack/common")),
                    ChatFormatting.GRAY));

    public static final DeferredItem<LootSackItem> LOOT_SACK_UNCOMMON = ITEMS.register("loot_sack_uncommon",
            () -> new LootSackItem(new Item.Properties().stacksTo(16),
                    ResourceKey.create(Registries.LOOT_TABLE,
                            ResourceLocation.fromNamespaceAndPath(RPG_Tweaks.MODID, "loot_sack/uncommon")),
                    ChatFormatting.GREEN));

    public static final DeferredItem<LootSackItem> LOOT_SACK_RARE = ITEMS.register("loot_sack_rare",
            () -> new LootSackItem(new Item.Properties().stacksTo(16),
                    ResourceKey.create(Registries.LOOT_TABLE,
                            ResourceLocation.fromNamespaceAndPath(RPG_Tweaks.MODID, "loot_sack/rare")),
                    ChatFormatting.AQUA));

    public static final DeferredItem<LootSackItem> LOOT_SACK_EPIC = ITEMS.register("loot_sack_epic",
            () -> new LootSackItem(new Item.Properties().stacksTo(16),
                    ResourceKey.create(Registries.LOOT_TABLE,
                            ResourceLocation.fromNamespaceAndPath(RPG_Tweaks.MODID, "loot_sack/epic")),
                    ChatFormatting.LIGHT_PURPLE));

    public static final DeferredItem<LootSackItem> LOOT_SACK_LEGENDARY = ITEMS.register("loot_sack_legendary",
            () -> new LootSackItem(new Item.Properties().stacksTo(16),
                    ResourceKey.create(Registries.LOOT_TABLE,
                            ResourceLocation.fromNamespaceAndPath(RPG_Tweaks.MODID, "loot_sack/legendary")),
                    ChatFormatting.GOLD));
}