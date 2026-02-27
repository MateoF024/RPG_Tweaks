package org.mateof24.rpg_tweaks.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.mateof24.rpg_tweaks.config.OreXPConfig.OreXPValues;

import java.util.*;

public class ModConfigScreen {

    public static Screen createConfigScreen(Screen parent) {
        ModConfig config = ModConfig.getInstance();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("RPG Tweaks").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .save(ModConfig::save)

                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("⭐ Advancements").withStyle(ChatFormatting.YELLOW))
                        .tooltip(Component.literal("Control experience from advancements"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Block Advancement XP"))
                                .description(OptionDescription.of(
                                        Component.literal("When enabled, players will not receive experience from completing advancements.")
                                ))
                                .binding(
                                        true,
                                        () -> config.blockAdvancementXP,
                                        value -> config.blockAdvancementXP = value
                                )
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Advancement XP Reward"))
                                .description(OptionDescription.of(
                                        Component.literal("XP granted when completing an advancement (0 = none).")
                                ))
                                .binding(
                                        0,
                                        () -> config.advancementXPReward,
                                        value -> config.advancementXPReward = value
                                )
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(0, 1000)
                                        .step(1)
                                        .formatValue(v -> Component.literal(v + " XP").withStyle(ChatFormatting.GOLD)))
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Log XP Blocking").withStyle(ChatFormatting.GRAY))
                                .description(OptionDescription.of(
                                        Component.literal("Debug: Logs detailed information to console")
                                ))
                                .binding(
                                        false,
                                        () -> config.logXPBlocking,
                                        value -> config.logXPBlocking = value
                                )
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("⛏ Ore Experience").withStyle(ChatFormatting.AQUA))
                        .tooltip(Component.literal("Customize XP drops from mining ores"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Enable Custom Ore XP"))
                                .description(OptionDescription.of(
                                        Component.literal("Master switch for the custom ore XP system")
                                ))
                                .binding(
                                        true,
                                        () -> config.enableCustomOreXP,
                                        value -> config.enableCustomOreXP = value
                                )
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Log Ore XP").withStyle(ChatFormatting.GRAY))
                                .description(OptionDescription.of(
                                        Component.literal("Debug: Logs XP drops to console")
                                ))
                                .binding(
                                        false,
                                        () -> config.logOreXP,
                                        value -> config.logOreXP = value
                                )
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .option(ButtonOption.createBuilder()
                                .name(Component.literal("➕ Add New Block/Tag").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                                .description(OptionDescription.of(
                                        Component.literal("Add a new block ID or tag to configure")
                                ))
                                .action((screen, opt) -> {
                                    net.minecraft.client.Minecraft.getInstance().setScreen(
                                            createAddNewScreen(screen, config)
                                    );
                                })
                                .build())

                        .groups(createOreGroups(config))

                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("⚡ Player Mechanics").withStyle(ChatFormatting.GREEN))
                        .tooltip(Component.literal("Modify base player mechanics"))

                        .option(Option.<Float>createBuilder()
                                .name(Component.literal("Exhaustion Rate Multiplier"))
                                .description(OptionDescription.of(
                                        Component.literal("Multiplies the exhaustion gained from actions. 1.0 = vanilla.")
                                ))
                                .binding(1.0f, () -> config.exhaustionRateMultiplier, value -> config.exhaustionRateMultiplier = value)
                                .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                        .range(0.0f, 5.0f)
                                        .step(0.1f)
                                        .formatValue(v -> Component.literal(String.format("%.1fx", v)).withStyle(ChatFormatting.YELLOW)))
                                .build())

                        .option(Option.<Float>createBuilder()
                                .name(Component.literal("Natural Regen Rate Multiplier"))
                                .description(OptionDescription.of(
                                        Component.literal("Multiplies natural HP regeneration from food. 1.0 = vanilla.")
                                ))
                                .binding(1.0f, () -> config.naturalRegenRateMultiplier, value -> config.naturalRegenRateMultiplier = value)
                                .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                        .range(0.0f, 5.0f)
                                        .step(0.1f)
                                        .formatValue(v -> Component.literal(String.format("%.1fx", v)).withStyle(ChatFormatting.YELLOW)))
                                .build())

                        .option(Option.<Float>createBuilder()
                                .name(Component.literal("Durability Multiplier"))
                                .description(OptionDescription.of(
                                        Component.literal("-1 = no durability loss | 0 = vanilla | >0 = damage multiplier")
                                ))
                                .binding(0f, () -> config.durabilityMultiplier, value -> config.durabilityMultiplier = value)
                                .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                        .range(-1.0f, 5.0f)
                                        .step(0.1f)
                                        .formatValue(v -> {
                                            if (v == -1f) return Component.literal("No loss").withStyle(ChatFormatting.GREEN);
                                            if (v == 0f) return Component.literal("Vanilla").withStyle(ChatFormatting.GRAY);
                                            return Component.literal(String.format("%.1fx", v)).withStyle(ChatFormatting.YELLOW);
                                        }))
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Max Storable XP"))
                                .description(OptionDescription.of(
                                        Component.literal("Maximum total XP a player can accumulate. 0 = unlimited.")
                                ))
                                .binding(0, () -> config.maxStorableXP, value -> config.maxStorableXP = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(0, 100000)
                                        .step(100)
                                        .formatValue(v -> v == 0
                                                ? Component.literal("Unlimited").withStyle(ChatFormatting.GREEN)
                                                : Component.literal(v + " XP").withStyle(ChatFormatting.YELLOW)))
                                .build())

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("PvP Enabled"))
                                .description(OptionDescription.of(
                                        Component.literal("Allow players to damage each other.")
                                ))
                                .binding(true, () -> config.pvpEnabled, value -> config.pvpEnabled = value)
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("🌙 Sleep").withStyle(ChatFormatting.BLUE))
                        .tooltip(Component.literal("Configure sleep-related mechanics"))

                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Sleep From Night"))
                                .description(OptionDescription.of(
                                        Component.literal("Minimum night number to allow sleeping. 0 = always allowed.")
                                ))
                                .binding(0, () -> config.sleepFromNight, value -> config.sleepFromNight = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(0, 100)
                                        .step(1)
                                        .formatValue(v -> v == 0
                                                ? Component.literal("Always").withStyle(ChatFormatting.GREEN)
                                                : Component.literal("Night " + v).withStyle(ChatFormatting.YELLOW)))
                                .build())

                        .option(Option.<Float>createBuilder()
                                .name(Component.literal("Sleep Heal Percent"))
                                .description(OptionDescription.of(
                                        Component.literal("Percentage of max HP restored after sleeping. 0 = disabled.")
                                ))
                                .binding(0f, () -> config.sleepHealPercent, value -> config.sleepHealPercent = value)
                                .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                        .range(0f, 100f)
                                        .step(1f)
                                        .formatValue(v -> v == 0f
                                                ? Component.literal("Disabled").withStyle(ChatFormatting.GRAY)
                                                : Component.literal(String.format("%.0f%%", v)).withStyle(ChatFormatting.GREEN)))
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Sleep Hunger Loss"))
                                .description(OptionDescription.of(
                                        Component.literal("Food points removed after sleeping. 0 = disabled.")
                                ))
                                .binding(0, () -> config.sleepHungerPoints, value -> config.sleepHungerPoints = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(0, 20)
                                        .step(1)
                                        .formatValue(v -> v == 0
                                                ? Component.literal("Disabled").withStyle(ChatFormatting.GRAY)
                                                : Component.literal(v + " points").withStyle(ChatFormatting.YELLOW)))
                                .build())

                        .option(Option.<Float>createBuilder()
                                .name(Component.literal("Sleep Hunger Chance"))
                                .description(OptionDescription.of(
                                        Component.literal("Chance (%) that hunger loss triggers after sleeping.")
                                ))
                                .binding(0f, () -> config.sleepHungerChance, value -> config.sleepHungerChance = value)
                                .controller(opt -> FloatSliderControllerBuilder.create(opt)
                                        .range(0f, 100f)
                                        .step(1f)
                                        .formatValue(v -> v == 0f
                                                ? Component.literal("Disabled").withStyle(ChatFormatting.GRAY)
                                                : Component.literal(String.format("%.0f%%", v)).withStyle(ChatFormatting.GREEN)))
                                .build())

                        .build())

                .build()
                .generateScreen(parent);
    }


    private static List<OptionGroup> createOreGroups(ModConfig config) {
        List<OptionGroup> groups = new ArrayList<>();

        Map<String, OreConfig> allOres = new TreeMap<>();

        config.oreXPConfig.blockConfigs.forEach((id, values) ->
                allOres.put(id, new OreConfig(id, values, false)));

        config.oreXPConfig.tagConfigs.forEach((tag, values) ->
                allOres.put(tag, new OreConfig(tag, values, true)));

        for (OreConfig ore : allOres.values()) {
            groups.add(createOreGroup(ore, config));
        }

        return groups;
    }

    private static OptionGroup createOreGroup(OreConfig ore, ModConfig config) {
        ChatFormatting color = ore.isTag ? ChatFormatting.AQUA : ChatFormatting.GREEN;


        return OptionGroup.createBuilder()
                .name(Component.literal(ore.identifier).withStyle(color))
                .description(OptionDescription.of(
                        Component.literal(ore.isTag ? "Tag-based configuration" : "Block ID configuration")
                ))
                .collapsed(true)

                .option(Option.<Integer>createBuilder()
                        .name(Component.literal("Minimum XP").withStyle(ChatFormatting.YELLOW))
                        .description(OptionDescription.of(
                                Component.literal("Minimum experience points dropped")
                        ))
                        .binding(
                                ore.values.minXP,
                                () -> ore.values.minXP,
                                value -> ore.values.minXP = value
                        )
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(0, 100)
                                .step(1)
                                .formatValue(v -> Component.literal(v + " XP").withStyle(ChatFormatting.GOLD)))
                        .build())

                // Max XP
                .option(Option.<Integer>createBuilder()
                        .name(Component.literal("Maximum XP").withStyle(ChatFormatting.YELLOW))
                        .description(OptionDescription.of(
                                Component.literal("Maximum experience points dropped")
                        ))
                        .binding(
                                ore.values.maxXP,
                                () -> ore.values.maxXP,
                                value -> ore.values.maxXP = value
                        )
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(0, 100)
                                .step(1)
                                .formatValue(v -> Component.literal(v + " XP").withStyle(ChatFormatting.GOLD)))
                        .build())

                .option(ButtonOption.createBuilder()
                        .name(Component.literal("🗑 Remove This Entry").withStyle(ChatFormatting.RED))
                        .description(OptionDescription.of(
                                Component.literal("Delete this block/tag from configuration")
                        ))
                        .action((screen, opt) -> {
                            if (ore.isTag) {
                                config.oreXPConfig.tagConfigs.remove(ore.identifier);
                            } else {
                                config.oreXPConfig.blockConfigs.remove(ore.identifier);
                            }
                            ModConfig.save();
                            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                            minecraft.execute(() -> {
                                minecraft.setScreen(createConfigScreen(minecraft.screen));
                            });
                        })
                        .build())

                .build();
    }


    private static Screen createAddNewScreen(Screen parent, ModConfig config) {
        final String[] newIdentifier = {""};
        final int[] minXP = {0};
        final int[] maxXP = {0};
        final boolean[] isTag = {false};

        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Add New Block/Tag").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .save(() -> {
                    if (!newIdentifier[0].isEmpty()) {
                        OreXPValues values = new OreXPValues(minXP[0], maxXP[0]);

                        if (isTag[0]) {
                            config.oreXPConfig.tagConfigs.put(newIdentifier[0], values);
                        } else {
                            config.oreXPConfig.blockConfigs.put(newIdentifier[0], values);
                        }

                        ModConfig.save();
                    }
                })

                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Configuration"))

                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Is Tag?").withStyle(ChatFormatting.AQUA))
                                .description(OptionDescription.of(
                                        Component.literal("Enable if adding a tag (e.g., minecraft:iron_ores)"),
                                        Component.literal("Disable if adding a block ID (e.g., minecraft:iron_ore)")
                                ))
                                .binding(
                                        false,
                                        () -> isTag[0],
                                        value -> isTag[0] = value
                                )
                                .controller(TickBoxControllerBuilder::create)
                                .build())

                        .option(Option.<String>createBuilder()
                                .name(Component.literal("Block ID or Tag").withStyle(ChatFormatting.YELLOW))
                                .description(OptionDescription.of(
                                        Component.literal("Examples:"),
                                        Component.literal("Block: minecraft:diamond_ore"),
                                        Component.literal("Tag: minecraft:diamond_ores")
                                ))
                                .binding(
                                        "",
                                        () -> newIdentifier[0],
                                        value -> newIdentifier[0] = value
                                )
                                .controller(StringControllerBuilder::create)
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Minimum XP").withStyle(ChatFormatting.GOLD))
                                .binding(
                                        0,
                                        () -> minXP[0],
                                        value -> minXP[0] = value
                                )
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(0, 100)
                                        .step(1)
                                        .formatValue(v -> Component.literal(v + " XP")))
                                .build())

                        .option(Option.<Integer>createBuilder()
                                .name(Component.literal("Maximum XP").withStyle(ChatFormatting.GOLD))
                                .binding(
                                        0,
                                        () -> maxXP[0],
                                        value -> maxXP[0] = value
                                )
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(0, 100)
                                        .step(1)
                                        .formatValue(v -> Component.literal(v + " XP")))
                                .build())

                        .build())

                .build()
                .generateScreen(parent);
    }

    private static class OreConfig {
        final String identifier;
        final OreXPValues values;
        final boolean isTag;

        OreConfig(String identifier, OreXPValues values, boolean isTag) {
            this.identifier = identifier;
            this.values = values;
            this.isTag = isTag;
        }
    }
}