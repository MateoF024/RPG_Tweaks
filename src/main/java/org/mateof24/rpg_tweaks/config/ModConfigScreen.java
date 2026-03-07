package org.mateof24.rpg_tweaks.config;

import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.mateof24.rpg_tweaks.config.OreXPConfig.OreXPValues;
import org.mateof24.rpg_tweaks.data.PlayerDimensionData;

import java.util.*;

public class ModConfigScreen {

    private static Screen lastParent = null;

    public static Screen createConfigScreen(Screen parent) {
        if (parent != null) lastParent = parent;
        ModConfig config = ModConfig.getInstance();

        ConfigCategory.Builder oreCategoryBuilder = ConfigCategory.createBuilder()
                .name(Component.translatable("config.rpg_tweaks.category.ore_xp").withStyle(ChatFormatting.AQUA))
                .tooltip(Component.translatable("config.rpg_tweaks.category.ore_xp.tooltip"))
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.enable_ore_xp"))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.enable_ore_xp.desc")))
                        .binding(true, () -> config.enableCustomOreXP, value -> config.enableCustomOreXP = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.log_ore_xp").withStyle(ChatFormatting.GRAY))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.log_ore_xp.desc")))
                        .binding(false, () -> config.logOreXP, value -> config.logOreXP = value)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .option(Option.<Float>createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.ore_xp_fortune_bonus"))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.ore_xp_fortune_bonus.desc")))
                        .binding(0.0f, () -> config.oreXPFortuneBonus, value -> config.oreXPFortuneBonus = value)
                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 2.0f).step(0.1f)
                                .formatValue(v -> v == 0f
                                        ? Component.translatable("config.rpg_tweaks.value.disabled").withStyle(ChatFormatting.GRAY)
                                        : Component.translatable("config.rpg_tweaks.option.ore_xp_fortune_bonus.value",
                                        String.format("%.1f", v)).withStyle(ChatFormatting.YELLOW)))
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.add_block_tag").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.add_block_tag.desc")))
                        .text(Component.translatable("config.rpg_tweaks.button.action"))
                        .action((screen, opt) -> net.minecraft.client.Minecraft.getInstance().setScreen(createAddOreScreen(config)))
                        .build());
        List<OptionGroup> oreGroups = createOreGroups(config);
        if (!oreGroups.isEmpty()) oreCategoryBuilder.groups(oreGroups);

        ConfigCategory.Builder dimCategoryBuilder = ConfigCategory.createBuilder()
                .name(Component.translatable("config.rpg_tweaks.category.dimensions").withStyle(ChatFormatting.DARK_PURPLE))
                .tooltip(Component.translatable("config.rpg_tweaks.category.dimensions.tooltip"))
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.add_dimension").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.add_dimension.desc")))
                        .text(Component.translatable("config.rpg_tweaks.button.action"))
                        .action((screen, opt) -> net.minecraft.client.Minecraft.getInstance().setScreen(createAddGlobalDimensionScreen(config)))
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.add_player_exception").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.add_player_exception.desc")))
                        .text(Component.translatable("config.rpg_tweaks.button.action"))
                        .action((screen, opt) -> net.minecraft.client.Minecraft.getInstance().setScreen(createAddPlayerExceptionScreen(config)))
                        .build());

        List<OptionGroup> dimGroups = createDimensionGroups(config);
        if (!dimGroups.isEmpty()) dimCategoryBuilder.groups(dimGroups);

        List<OptionGroup> playerGroups = createPlayerExceptionGroups();
        if (!playerGroups.isEmpty()) dimCategoryBuilder.groups(playerGroups);

        ConfigCategory.Builder mobLootCategoryBuilder = ConfigCategory.createBuilder()
                .name(Component.translatable("config.rpg_tweaks.category.mob_loot").withStyle(ChatFormatting.DARK_RED))
                .tooltip(Component.translatable("config.rpg_tweaks.category.mob_loot.tooltip"))
                .option(Option.<Float>createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.looting_bonus"))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.looting_bonus.desc")))
                        .binding(5.0f, () -> config.lootSackLootingBonus, value -> config.lootSackLootingBonus = value)
                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 50.0f).step(0.5f)
                                .formatValue(v -> v == 0f
                                        ? Component.translatable("config.rpg_tweaks.value.disabled").withStyle(ChatFormatting.GRAY)
                                        : Component.translatable("config.rpg_tweaks.value.percent", String.format("%.1f", v)).withStyle(ChatFormatting.YELLOW)))
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.add_mob").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.add_mob.desc")))
                        .text(Component.translatable("config.rpg_tweaks.button.action"))
                        .action((screen, opt) -> net.minecraft.client.Minecraft.getInstance().setScreen(createAddMobScreen(config)))
                        .build());
        List<OptionGroup> mobGroups = createMobLootGroups(config);
        if (!mobGroups.isEmpty()) mobLootCategoryBuilder.groups(mobGroups);

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.rpg_tweaks.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .save(ModConfig::save)

                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.category.advancements").withStyle(ChatFormatting.YELLOW))
                        .tooltip(Component.translatable("config.rpg_tweaks.category.advancements.tooltip"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.block_advancement_xp"))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.block_advancement_xp.desc")))
                                .binding(true, () -> config.blockAdvancementXP, value -> config.blockAdvancementXP = value)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.advancement_xp_reward"))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.advancement_xp_reward.desc")))
                                .binding(0, () -> config.advancementXPReward, value -> config.advancementXPReward = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(0, 1000).step(1)
                                        .formatValue(v -> Component.translatable("config.rpg_tweaks.value.xp", v).withStyle(ChatFormatting.GOLD)))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.goal_xp_reward"))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.goal_xp_reward.desc")))
                                .binding(0, () -> config.goalXPReward, value -> config.goalXPReward = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(0, 1000).step(1)
                                        .formatValue(v -> Component.translatable("config.rpg_tweaks.value.xp", v).withStyle(ChatFormatting.GOLD)))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.challenge_xp_reward"))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.challenge_xp_reward.desc")))
                                .binding(0, () -> config.challengeXPReward, value -> config.challengeXPReward = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(0, 1000).step(1)
                                        .formatValue(v -> Component.translatable("config.rpg_tweaks.value.xp", v).withStyle(ChatFormatting.GOLD)))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.log_xp_blocking").withStyle(ChatFormatting.GRAY))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.log_xp_blocking.desc")))
                                .binding(false, () -> config.logXPBlocking, value -> config.logXPBlocking = value)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())

                .category(oreCategoryBuilder.build())

                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.category.player").withStyle(ChatFormatting.GREEN))
                        .tooltip(Component.translatable("config.rpg_tweaks.category.player.tooltip"))
                        .option(Option.<Float>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.exhaustion_rate"))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.exhaustion_rate.desc")))
                                .binding(1.0f, () -> config.exhaustionRateMultiplier, value -> config.exhaustionRateMultiplier = value)
                                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 5.0f).step(0.1f)
                                        .formatValue(v -> Component.translatable("config.rpg_tweaks.value.multiplier", String.format("%.1f", v)).withStyle(ChatFormatting.YELLOW)))
                                .build())
                        .option(Option.<Float>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.regen_rate"))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.regen_rate.desc")))
                                .binding(1.0f, () -> config.naturalRegenRateMultiplier, value -> config.naturalRegenRateMultiplier = value)
                                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 5.0f).step(0.1f)
                                        .formatValue(v -> Component.translatable("config.rpg_tweaks.value.multiplier", String.format("%.1f", v)).withStyle(ChatFormatting.YELLOW)))
                                .build())
                        .option(Option.<Float>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.durability"))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.durability.desc")))
                                .binding(0f, () -> config.durabilityMultiplier, value -> config.durabilityMultiplier = value)
                                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(-1.0f, 5.0f).step(0.1f)
                                        .formatValue(v -> {
                                            if (v == -1f) return Component.translatable("config.rpg_tweaks.option.durability.no_loss").withStyle(ChatFormatting.GREEN);
                                            if (v == 0f) return Component.translatable("config.rpg_tweaks.option.durability.vanilla").withStyle(ChatFormatting.GRAY);
                                            return Component.translatable("config.rpg_tweaks.value.multiplier", String.format("%.1f", v)).withStyle(ChatFormatting.YELLOW);
                                        }))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.max_xp_cap"))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.max_xp_cap.desc")))
                                .binding(0, () -> config.maxStorableXP, value -> config.maxStorableXP = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 100000).step(100)
                                        .formatValue(v -> v == 0
                                                ? Component.translatable("config.rpg_tweaks.option.max_xp_cap.unlimited").withStyle(ChatFormatting.GREEN)
                                                : Component.translatable("config.rpg_tweaks.value.xp", v).withStyle(ChatFormatting.YELLOW)))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.pvp"))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.pvp.desc")))
                                .binding(true, () -> config.pvpEnabled, value -> config.pvpEnabled = value)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())

                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.category.sleep").withStyle(ChatFormatting.BLUE))
                        .tooltip(Component.translatable("config.rpg_tweaks.category.sleep.tooltip"))
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.sleep_from_night"))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.sleep_from_night.desc")))
                                .binding(0, () -> config.sleepFromNight, value -> config.sleepFromNight = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 100).step(1)
                                        .formatValue(v -> v == 0
                                                ? Component.translatable("config.rpg_tweaks.option.sleep_from_night.always").withStyle(ChatFormatting.GREEN)
                                                : Component.translatable("config.rpg_tweaks.option.sleep_from_night.night", v).withStyle(ChatFormatting.YELLOW)))
                                .build())
                        .option(Option.<Float>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.sleep_heal"))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.sleep_heal.desc")))
                                .binding(0f, () -> config.sleepHealPercent, value -> config.sleepHealPercent = value)
                                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0f, 100f).step(1f)
                                        .formatValue(v -> v == 0f
                                                ? Component.translatable("config.rpg_tweaks.value.disabled").withStyle(ChatFormatting.GRAY)
                                                : Component.translatable("config.rpg_tweaks.value.percent", String.format("%.0f", v)).withStyle(ChatFormatting.GREEN)))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.sleep_hunger"))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.sleep_hunger.desc")))
                                .binding(0, () -> config.sleepHungerPoints, value -> config.sleepHungerPoints = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 20).step(1)
                                        .formatValue(v -> v == 0
                                                ? Component.translatable("config.rpg_tweaks.value.disabled").withStyle(ChatFormatting.GRAY)
                                                : Component.translatable("config.rpg_tweaks.value.points", v).withStyle(ChatFormatting.YELLOW)))
                                .build())
                        .option(Option.<Float>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.sleep_hunger_chance"))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.sleep_hunger_chance.desc")))
                                .binding(0f, () -> config.sleepHungerChance, value -> config.sleepHungerChance = value)
                                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0f, 100f).step(1f)
                                        .formatValue(v -> v == 0f
                                                ? Component.translatable("config.rpg_tweaks.value.disabled").withStyle(ChatFormatting.GRAY)
                                                : Component.translatable("config.rpg_tweaks.value.percent", String.format("%.0f", v)).withStyle(ChatFormatting.GREEN)))
                                .build())
                        .build())

                .category(dimCategoryBuilder.build())
                .category(mobLootCategoryBuilder.build())

                .build()
                .generateScreen(parent);
    }

    private static void refreshScreen(int categoryIndex) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        ModConfig.save();
        Screen newScreen = createConfigScreen(lastParent);
        mc.setScreen(newScreen);
        selectCategory(newScreen, categoryIndex);
    }

    private static void selectCategory(Screen screen, int index) {
        try {
            Class<?> clazz = screen.getClass();
            while (clazz != null && clazz != Screen.class) {
                try {
                    java.lang.reflect.Field f = clazz.getDeclaredField("tabNavigationBar");
                    f.setAccessible(true);
                    Object bar = f.get(screen);
                    if (bar != null) {
                        bar.getClass().getMethod("selectTab", int.class, boolean.class).invoke(bar, index, false);
                        return;
                    }
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception ignored) {}
    }

    private static List<OptionGroup> createOreGroups(ModConfig config) {
        List<OptionGroup> groups = new ArrayList<>();
        Map<String, OreConfig> allOres = new java.util.TreeMap<>();
        config.oreXPConfig.blockConfigs.forEach((id, values) -> allOres.put(id, new OreConfig(id, values, false)));
        config.oreXPConfig.tagConfigs.forEach((tag, values) -> allOres.put(tag, new OreConfig(tag, values, true)));
        for (OreConfig ore : allOres.values()) groups.add(createOreGroup(ore, config));
        return groups;
    }

    private static OptionGroup createOreGroup(OreConfig ore, ModConfig config) {
        ChatFormatting color = ore.isTag ? ChatFormatting.AQUA : ChatFormatting.GREEN;
        return OptionGroup.createBuilder()
                .name(Component.literal(ore.identifier).withStyle(color))
                .description(OptionDescription.of(Component.translatable(ore.isTag ? "config.rpg_tweaks.ore.tag_desc" : "config.rpg_tweaks.ore.block_desc")))
                .collapsed(true)
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.min_xp").withStyle(ChatFormatting.YELLOW))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.min_xp.desc")))
                        .binding(ore.values.minXP, () -> ore.values.minXP, value -> ore.values.minXP = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 100).step(1)
                                .formatValue(v -> Component.translatable("config.rpg_tweaks.value.xp", v).withStyle(ChatFormatting.GOLD)))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.max_xp").withStyle(ChatFormatting.YELLOW))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.max_xp.desc")))
                        .binding(ore.values.maxXP, () -> ore.values.maxXP, value -> ore.values.maxXP = value)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 100).step(1)
                                .formatValue(v -> Component.translatable("config.rpg_tweaks.value.xp", v).withStyle(ChatFormatting.GOLD)))
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.remove_entry").withStyle(ChatFormatting.RED))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.remove_entry.desc")))
                        .text(Component.translatable("config.rpg_tweaks.button.action"))
                        .action((screen, opt) -> {
                            if (ore.isTag) config.oreXPConfig.tagConfigs.remove(ore.identifier);
                            else config.oreXPConfig.blockConfigs.remove(ore.identifier);
                            refreshScreen(1);
                        })
                        .build())
                .build();
    }

    private static Screen createAddOreScreen(ModConfig config) {
        final String[] newIdentifier = {""};
        final int[] minXP = {0};
        final int[] maxXP = {0};
        final boolean[] isTag = {false};

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.rpg_tweaks.add_screen.title").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .save(() -> {
                    if (!newIdentifier[0].isEmpty()) {
                        OreXPValues values = new OreXPValues(minXP[0], maxXP[0]);
                        if (isTag[0]) config.oreXPConfig.tagConfigs.put(newIdentifier[0], values);
                        else config.oreXPConfig.blockConfigs.put(newIdentifier[0], values);
                        ModConfig.save();
                    }
                    net.minecraft.client.Minecraft.getInstance().execute(() -> refreshScreen(1));
                })
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.add_screen.category"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.add_screen.is_tag").withStyle(ChatFormatting.AQUA))
                                .description(OptionDescription.of(
                                        Component.translatable("config.rpg_tweaks.add_screen.is_tag.desc1"),
                                        Component.translatable("config.rpg_tweaks.add_screen.is_tag.desc2")))
                                .binding(false, () -> isTag[0], value -> isTag[0] = value)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.add_screen.identifier").withStyle(ChatFormatting.YELLOW))
                                .description(OptionDescription.of(
                                        Component.translatable("config.rpg_tweaks.add_screen.identifier.desc1"),
                                        Component.translatable("config.rpg_tweaks.add_screen.identifier.desc2"),
                                        Component.translatable("config.rpg_tweaks.add_screen.identifier.desc3")))
                                .binding("", () -> newIdentifier[0], value -> newIdentifier[0] = value)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.min_xp").withStyle(ChatFormatting.GOLD))
                                .binding(0, () -> minXP[0], value -> minXP[0] = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 100).step(1)
                                        .formatValue(v -> Component.translatable("config.rpg_tweaks.value.xp", v)))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.max_xp").withStyle(ChatFormatting.GOLD))
                                .binding(0, () -> maxXP[0], value -> maxXP[0] = value)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 100).step(1)
                                        .formatValue(v -> Component.translatable("config.rpg_tweaks.value.xp", v)))
                                .build())
                        .build())
                .build()
                .generateScreen(lastParent);
    }

    private static List<OptionGroup> createDimensionGroups(ModConfig config) {
        List<OptionGroup> groups = new ArrayList<>();
        for (Map.Entry<String, String> entry : new java.util.LinkedHashMap<>(config.blockedDimensions).entrySet()) {
            String dim = entry.getKey();
            groups.add(OptionGroup.createBuilder()
                    .name(Component.literal("🌍 " + dim).withStyle(ChatFormatting.DARK_PURPLE))
                    .collapsed(false)
                    .option(Option.<String>createBuilder()
                            .name(Component.translatable("config.rpg_tweaks.option.dimension_custom_msg").withStyle(ChatFormatting.YELLOW))
                            .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.dimension_custom_msg.desc")))
                            .binding("", () -> config.blockedDimensions.getOrDefault(dim, ""), value -> config.blockedDimensions.put(dim, value))
                            .controller(StringControllerBuilder::create)
                            .build())
                    .option(ButtonOption.createBuilder()
                            .name(Component.translatable("config.rpg_tweaks.option.remove_dimension").withStyle(ChatFormatting.RED))
                            .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.remove_dimension.desc")))
                            .text(Component.translatable("config.rpg_tweaks.button.action"))
                            .action((screen, opt) -> {
                                config.blockedDimensions.remove(dim);
                                refreshScreen(4);
                            })
                            .build())
                    .build());
        }
        return groups;
    }

    private static Screen createAddGlobalDimensionScreen(ModConfig config) {
        final String[] dimensionId = {""};
        final String[] customMsg = {""};
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.rpg_tweaks.add_dimension.title").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD))
                .save(() -> {
                    String id = dimensionId[0].trim();
                    if (!id.isEmpty() && !config.blockedDimensions.containsKey(id)) {
                        config.blockedDimensions.put(id, customMsg[0].trim());
                        ModConfig.save();
                    }
                    net.minecraft.client.Minecraft.getInstance().execute(() -> refreshScreen(4));
                })
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.add_screen.category"))
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.add_dimension.identifier").withStyle(ChatFormatting.YELLOW))
                                .description(OptionDescription.of(
                                        Component.translatable("config.rpg_tweaks.add_dimension.identifier.desc1"),
                                        Component.translatable("config.rpg_tweaks.add_dimension.identifier.desc2"),
                                        Component.translatable("config.rpg_tweaks.add_dimension.identifier.desc3"),
                                        Component.translatable("config.rpg_tweaks.add_dimension.identifier.desc4")))
                                .binding("", () -> dimensionId[0], value -> dimensionId[0] = value)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.dimension_custom_msg").withStyle(ChatFormatting.YELLOW))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.dimension_custom_msg.desc")))
                                .binding("", () -> customMsg[0], value -> customMsg[0] = value)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .build())
                .build()
                .generateScreen(lastParent);
    }

    private static List<OptionGroup> createPlayerExceptionGroups() {
        List<OptionGroup> groups = new ArrayList<>();

        for (Map.Entry<String, PlayerDimensionData.PendingEntry> entry : PlayerDimensionData.getPending().entrySet()) {
            String label = entry.getKey() + " ⏳ " + (entry.getValue().isAllow ? "✅" : "⛔") + " " + entry.getValue().dimension;
            groups.add(OptionGroup.createBuilder()
                    .name(Component.literal(label).withStyle(ChatFormatting.GRAY))
                    .collapsed(false)
                    .option(ButtonOption.createBuilder()
                            .name(Component.translatable("config.rpg_tweaks.player_dim.remove_player").withStyle(ChatFormatting.RED))
                            .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.player_dim.remove_player.desc")))
                            .text(Component.translatable("config.rpg_tweaks.button.action"))
                            .action((screen, opt) -> {
                                PlayerDimensionData.removePending(entry.getKey());
                                refreshScreen(4);
                            })
                            .build())
                    .build());
        }

        for (String uuid : PlayerDimensionData.getAllConfiguredPlayers()) {
            String playerName = PlayerDimensionData.getPlayerName(uuid);
            UUID playerUUID;
            try { playerUUID = UUID.fromString(uuid); } catch (IllegalArgumentException e) { continue; }

            Set<String> blockedDims = PlayerDimensionData.getBlockedDimensions(playerUUID);
            Set<String> allowedDims = PlayerDimensionData.getAllowedDimensions(playerUUID);

            List<Option<?>> options = new ArrayList<>();

            options.add(ButtonOption.createBuilder()
                    .name(Component.translatable("config.rpg_tweaks.player_dim.add_block").withStyle(ChatFormatting.RED))
                    .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.player_dim.add_block.desc")))
                    .text(Component.translatable("config.rpg_tweaks.button.action"))
                    .action((screen, opt) -> net.minecraft.client.Minecraft.getInstance().setScreen(
                            createAddPlayerDimScreen(playerUUID, playerName, false)))
                    .build());

            for (String dim : new ArrayList<>(blockedDims)) {
                options.add(ButtonOption.createBuilder()
                        .name(Component.literal("✏ ⛔ " + dim).withStyle(ChatFormatting.RED))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.dimension_custom_msg.desc")))
                        .text(Component.translatable("config.rpg_tweaks.button.action"))
                        .action((screen, opt) -> net.minecraft.client.Minecraft.getInstance().setScreen(
                                createEditBlockedDimScreen(playerUUID, playerName, dim)))
                        .build());
                options.add(ButtonOption.createBuilder()
                        .name(Component.literal("🗑 ⛔ " + dim).withStyle(ChatFormatting.DARK_RED))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.player_dim.remove_block.desc")))
                        .text(Component.translatable("config.rpg_tweaks.button.action").withStyle(ChatFormatting.DARK_RED))
                        .action((screen, opt) -> {
                            PlayerDimensionData.removeDimension(playerUUID, dim);
                            refreshScreen(4);
                        })
                        .build());
            }

            options.add(ButtonOption.createBuilder()
                    .name(Component.translatable("config.rpg_tweaks.player_dim.add_allow").withStyle(ChatFormatting.GREEN))
                    .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.player_dim.add_allow.desc")))
                    .text(Component.translatable("config.rpg_tweaks.button.action"))
                    .action((screen, opt) -> net.minecraft.client.Minecraft.getInstance().setScreen(
                            createAddPlayerDimScreen(playerUUID, playerName, true)))
                    .build());

            for (String dim : new ArrayList<>(allowedDims)) {
                options.add(ButtonOption.createBuilder()
                        .name(Component.literal("✅ " + dim).withStyle(ChatFormatting.GREEN))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.player_dim.remove_allow.desc")))
                        .text(Component.translatable("config.rpg_tweaks.button.action").withStyle(ChatFormatting.DARK_GREEN))
                        .action((screen, opt) -> {
                            PlayerDimensionData.removeDimension(playerUUID, dim);
                            refreshScreen(4);
                        })
                        .build());
            }

            options.add(ButtonOption.createBuilder()
                    .name(Component.translatable("config.rpg_tweaks.player_dim.remove_player").withStyle(ChatFormatting.DARK_RED))
                    .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.player_dim.remove_player.desc")))
                    .text(Component.translatable("config.rpg_tweaks.button.action").withStyle(ChatFormatting.DARK_RED))
                    .action((screen, opt) -> {
                        PlayerDimensionData.removeAllForPlayer(playerUUID);
                        refreshScreen(4);
                    })
                    .build());

            groups.add(OptionGroup.createBuilder()
                    .name(Component.literal(playerName).withStyle(ChatFormatting.LIGHT_PURPLE))
                    .collapsed(true)
                    .options(options)
                    .build());
        }
        return groups;
    }

    private static Screen createAddPlayerExceptionScreen(ModConfig config) {
        final String[] playerNameInput = {""};
        final String[] dimensionInput = {""};
        final boolean[] isAllow = {false};
        final String[] customMessage = {""};

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.rpg_tweaks.add_player_exception.title").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                .save(() -> {
                    String name = playerNameInput[0].trim();
                    String dim = dimensionInput[0].trim();
                    if (!name.isEmpty() && !dim.isEmpty()) {
                        String msg = isAllow[0] ? "" : customMessage[0].trim();
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        boolean applied = false;
                        if (mc.getSingleplayerServer() != null) {
                            net.minecraft.server.level.ServerPlayer online =
                                    mc.getSingleplayerServer().getPlayerList().getPlayerByName(name);
                            if (online != null) {
                                PlayerDimensionData.setDimension(online.getUUID(), online.getName().getString(), dim, isAllow[0], msg);
                                applied = true;
                            }
                        }
                        if (!applied) {
                            PlayerDimensionData.addPending(name, dim, isAllow[0], msg);
                        }
                    }
                    net.minecraft.client.Minecraft.getInstance().execute(() -> refreshScreen(4));
                })
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.add_screen.category"))
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.add_player_exception.player").withStyle(ChatFormatting.YELLOW))
                                .description(OptionDescription.of(
                                        Component.translatable("config.rpg_tweaks.add_player_exception.player.desc"),
                                        Component.translatable("config.rpg_tweaks.add_player_exception.player.desc2").withStyle(ChatFormatting.GRAY)))
                                .binding("", () -> playerNameInput[0], value -> playerNameInput[0] = value)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.add_player_exception.dimension").withStyle(ChatFormatting.YELLOW))
                                .description(OptionDescription.of(
                                        Component.translatable("config.rpg_tweaks.add_player_exception.dimension.desc")))
                                .binding("", () -> dimensionInput[0], value -> dimensionInput[0] = value)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.add_player_exception.is_allow").withStyle(ChatFormatting.AQUA))
                                .description(OptionDescription.of(
                                        Component.translatable("config.rpg_tweaks.add_player_exception.is_allow.desc_on").withStyle(ChatFormatting.GREEN),
                                        Component.translatable("config.rpg_tweaks.add_player_exception.is_allow.desc_off").withStyle(ChatFormatting.RED)))
                                .binding(false, () -> isAllow[0], value -> isAllow[0] = value)
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.dimension_custom_msg").withStyle(ChatFormatting.YELLOW))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.dimension_custom_msg.desc")))
                                .binding("", () -> customMessage[0], value -> customMessage[0] = value)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .build())
                .build()
                .generateScreen(lastParent);
    }

    private static Screen createAddPlayerDimScreen(UUID playerUUID, String playerName, boolean isAllow) {
        final String[] dimensionInput = {""};
        final String[] customMessage = {""};

        ConfigCategory.Builder catBuilder = ConfigCategory.createBuilder()
                .name(Component.translatable("config.rpg_tweaks.add_screen.category"))
                .option(Option.<String>createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.add_player_exception.dimension").withStyle(ChatFormatting.YELLOW))
                        .description(OptionDescription.of(
                                Component.translatable("config.rpg_tweaks.add_player_exception.dimension.desc")))
                        .binding("", () -> dimensionInput[0], value -> dimensionInput[0] = value)
                        .controller(StringControllerBuilder::create)
                        .build());

        if (!isAllow) {
            catBuilder.option(Option.<String>createBuilder()
                    .name(Component.translatable("config.rpg_tweaks.option.dimension_custom_msg").withStyle(ChatFormatting.YELLOW))
                    .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.dimension_custom_msg.desc")))
                    .binding("", () -> customMessage[0], value -> customMessage[0] = value)
                    .controller(StringControllerBuilder::create)
                    .build());
        }

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable(isAllow
                                ? "config.rpg_tweaks.add_player_dim.title_allow"
                                : "config.rpg_tweaks.add_player_dim.title_block", playerName)
                        .withStyle(isAllow ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD))
                .save(() -> {
                    String dim = dimensionInput[0].trim();
                    if (!dim.isEmpty()) {
                        PlayerDimensionData.setDimension(playerUUID, playerName, dim, isAllow, isAllow ? "" : customMessage[0].trim());
                    }
                    net.minecraft.client.Minecraft.getInstance().execute(() -> refreshScreen(4));
                })
                .category(catBuilder.build())
                .build()
                .generateScreen(lastParent);
    }

    private static Screen createEditBlockedDimScreen(UUID playerUUID, String playerName, String dim) {
        String current = PlayerDimensionData.getBlockedMessage(playerUUID, dim);
        final String[] msg = {current != null ? current : ""};
        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("⛔ " + dim).withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .save(() -> {
                    PlayerDimensionData.setDimension(playerUUID, playerName, dim, false, msg[0].trim());
                    net.minecraft.client.Minecraft.getInstance().execute(() -> refreshScreen(4));
                })
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.add_screen.category"))
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.option.dimension_custom_msg").withStyle(ChatFormatting.YELLOW))
                                .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.dimension_custom_msg.desc")))
                                .binding("", () -> msg[0], value -> msg[0] = value)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .build())
                .build()
                .generateScreen(lastParent);
    }

    private static List<OptionGroup> createMobLootGroups(ModConfig config) {
        List<OptionGroup> groups = new ArrayList<>();
        for (Map.Entry<String, MobLootConfig.MobSackDrops> entry : config.mobLootConfig.mobDrops.entrySet()) {
            groups.add(createMobLootGroup(entry.getKey(), entry.getValue(), config));
        }
        return groups;
    }

    private static OptionGroup createMobLootGroup(String mobId, MobLootConfig.MobSackDrops drops, ModConfig config) {
        return OptionGroup.createBuilder()
                .name(Component.literal(mobId).withStyle(ChatFormatting.DARK_RED))
                .collapsed(true)
                .option(Option.<Float>createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.mob_loot.common_chance").withStyle(ChatFormatting.GRAY))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.mob_loot.chance_desc")))
                        .binding(drops.commonChance, () -> drops.commonChance, v -> drops.commonChance = v)
                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0f, 100f).step(0.5f)
                                .formatValue(v -> v == 0f
                                        ? Component.translatable("config.rpg_tweaks.value.disabled").withStyle(ChatFormatting.GRAY)
                                        : Component.translatable("config.rpg_tweaks.value.percent", String.format("%.1f", v)).withStyle(ChatFormatting.WHITE)))
                        .build())
                .option(Option.<Float>createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.mob_loot.uncommon_chance").withStyle(ChatFormatting.GREEN))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.mob_loot.chance_desc")))
                        .binding(drops.uncommonChance, () -> drops.uncommonChance, v -> drops.uncommonChance = v)
                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0f, 100f).step(0.5f)
                                .formatValue(v -> v == 0f
                                        ? Component.translatable("config.rpg_tweaks.value.disabled").withStyle(ChatFormatting.GRAY)
                                        : Component.translatable("config.rpg_tweaks.value.percent", String.format("%.1f", v)).withStyle(ChatFormatting.GREEN)))
                        .build())
                .option(Option.<Float>createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.mob_loot.rare_chance").withStyle(ChatFormatting.AQUA))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.mob_loot.chance_desc")))
                        .binding(drops.rareChance, () -> drops.rareChance, v -> drops.rareChance = v)
                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0f, 100f).step(0.5f)
                                .formatValue(v -> v == 0f
                                        ? Component.translatable("config.rpg_tweaks.value.disabled").withStyle(ChatFormatting.GRAY)
                                        : Component.translatable("config.rpg_tweaks.value.percent", String.format("%.1f", v)).withStyle(ChatFormatting.AQUA)))
                        .build())
                .option(Option.<Float>createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.mob_loot.epic_chance").withStyle(ChatFormatting.LIGHT_PURPLE))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.mob_loot.chance_desc")))
                        .binding(drops.epicChance, () -> drops.epicChance, v -> drops.epicChance = v)
                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0f, 100f).step(0.5f)
                                .formatValue(v -> v == 0f
                                        ? Component.translatable("config.rpg_tweaks.value.disabled").withStyle(ChatFormatting.GRAY)
                                        : Component.translatable("config.rpg_tweaks.value.percent", String.format("%.1f", v)).withStyle(ChatFormatting.LIGHT_PURPLE)))
                        .build())
                .option(Option.<Float>createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.mob_loot.legendary_chance").withStyle(ChatFormatting.GOLD))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.mob_loot.chance_desc")))
                        .binding(drops.legendaryChance, () -> drops.legendaryChance, v -> drops.legendaryChance = v)
                        .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0f, 100f).step(0.1f)
                                .formatValue(v -> v == 0f
                                        ? Component.translatable("config.rpg_tweaks.value.disabled").withStyle(ChatFormatting.GRAY)
                                        : Component.translatable("config.rpg_tweaks.value.percent", String.format("%.1f", v)).withStyle(ChatFormatting.GOLD)))
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.manage_removed_drops").withStyle(ChatFormatting.YELLOW))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.manage_removed_drops.desc")))
                        .text(Component.translatable("config.rpg_tweaks.button.action"))
                        .action((screen, opt) -> {
                            ModConfig.save();
                            net.minecraft.client.Minecraft.getInstance().setScreen(createRemovedDropsScreen(config, mobId, drops));
                        })
                        .build())
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.remove_mob").withStyle(ChatFormatting.RED))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.remove_mob.desc")))
                        .text(Component.translatable("config.rpg_tweaks.button.action").withStyle(ChatFormatting.RED))
                        .action((screen, opt) -> {
                            config.mobLootConfig.mobDrops.remove(mobId);
                            refreshScreen(5);
                        })
                        .build())
                .build();
    }

    private static Screen createAddMobScreen(ModConfig config) {
        final String[] mobId = {""};
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.rpg_tweaks.add_mob.title").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
                .save(() -> {
                    String id = mobId[0].trim();
                    if (!id.isEmpty() && !config.mobLootConfig.mobDrops.containsKey(id)) {
                        config.mobLootConfig.mobDrops.put(id, new MobLootConfig.MobSackDrops());
                        ModConfig.save();
                    }
                    net.minecraft.client.Minecraft.getInstance().execute(() -> refreshScreen(5));
                })
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.add_screen.category"))
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.add_mob.identifier").withStyle(ChatFormatting.YELLOW))
                                .description(OptionDescription.of(
                                        Component.translatable("config.rpg_tweaks.add_mob.identifier.desc1"),
                                        Component.translatable("config.rpg_tweaks.add_mob.identifier.desc2"),
                                        Component.translatable("config.rpg_tweaks.add_mob.identifier.desc3"),
                                        Component.translatable("config.rpg_tweaks.add_mob.identifier.desc4")))
                                .binding("", () -> mobId[0], value -> mobId[0] = value)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .build())
                .build()
                .generateScreen(lastParent);
    }

    private static Screen createRemovedDropsScreen(ModConfig config, String mobId, MobLootConfig.MobSackDrops drops) {
        ConfigCategory.Builder categoryBuilder = ConfigCategory.createBuilder()
                .name(Component.translatable("config.rpg_tweaks.add_screen.category"))
                .option(ButtonOption.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.option.add_removed_drop").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                        .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.add_removed_drop.desc")))
                        .text(Component.translatable("config.rpg_tweaks.button.action"))
                        .action((screen, opt) -> {
                            ModConfig.save();
                            net.minecraft.client.Minecraft.getInstance().setScreen(createAddRemovedDropScreen(config, mobId, drops));
                        })
                        .build());

        List<OptionGroup> dropGroups = new ArrayList<>();
        for (String drop : new ArrayList<>(drops.removedDrops)) {
            dropGroups.add(OptionGroup.createBuilder()
                    .name(Component.literal(drop).withStyle(ChatFormatting.YELLOW))
                    .collapsed(false)
                    .option(ButtonOption.createBuilder()
                            .name(Component.translatable("config.rpg_tweaks.option.remove_drop_entry").withStyle(ChatFormatting.RED))
                            .description(OptionDescription.of(Component.translatable("config.rpg_tweaks.option.remove_drop_entry.desc")))
                            .text(Component.translatable("config.rpg_tweaks.button.action").withStyle(ChatFormatting.RED))
                            .action((screen, opt) -> {
                                drops.removedDrops.remove(drop);
                                ModConfig.save();
                                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                                mc.execute(() -> mc.setScreen(createRemovedDropsScreen(config, mobId, drops)));
                            })
                            .build())
                    .build());
        }
        if (!dropGroups.isEmpty()) categoryBuilder.groups(dropGroups);

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.rpg_tweaks.removed_drops.title", mobId).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
                .save(ModConfig::save)
                .category(categoryBuilder.build())
                .build()
                .generateScreen(lastParent);
    }

    private static Screen createAddRemovedDropScreen(ModConfig config, String mobId, MobLootConfig.MobSackDrops drops) {
        final String[] dropId = {""};
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.rpg_tweaks.add_removed_drop.title").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .save(() -> {
                    String id = dropId[0].trim();
                    if (!id.isEmpty() && !drops.removedDrops.contains(id)) {
                        drops.removedDrops.add(id);
                        ModConfig.save();
                    }
                })
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("config.rpg_tweaks.add_screen.category"))
                        .option(Option.<String>createBuilder()
                                .name(Component.translatable("config.rpg_tweaks.add_removed_drop.identifier").withStyle(ChatFormatting.YELLOW))
                                .description(OptionDescription.of(
                                        Component.translatable("config.rpg_tweaks.add_removed_drop.identifier.desc1"),
                                        Component.translatable("config.rpg_tweaks.add_removed_drop.identifier.desc2"),
                                        Component.translatable("config.rpg_tweaks.add_removed_drop.identifier.desc3")))
                                .binding("", () -> dropId[0], value -> dropId[0] = value)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .build())
                .build()
                .generateScreen(createRemovedDropsScreen(config, mobId, drops));
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