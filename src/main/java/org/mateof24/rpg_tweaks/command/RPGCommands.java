package org.mateof24.rpg_tweaks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.mateof24.rpg_tweaks.config.OreXPConfig;
import org.mateof24.rpg_tweaks.config.XPValues;
import org.mateof24.rpg_tweaks.config.MobLootConfig;
import org.mateof24.rpg_tweaks.data.PlayerDimensionData;
import org.mateof24.rpg_tweaks.event.OreXPHandler;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class RPGCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpg_tweaks")
                .then(Commands.literal("config")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(RPGCommands::reloadConfig))
                        .then(Commands.literal("reset")
                                .executes(RPGCommands::resetAll)
                                .then(Commands.literal("advancements")
                                        .executes(RPGCommands::resetAdvancements))
                                .then(Commands.literal("ore_xp")
                                        .executes(RPGCommands::resetOreXP)
                                        .then(Commands.argument("entry", StringArgumentType.greedyString())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(getOreEntries(), builder))
                                                .executes(RPGCommands::resetOreEntry)))
                                .then(Commands.literal("player")
                                        .executes(RPGCommands::resetPlayer))
                                .then(Commands.literal("sleep")
                                        .executes(RPGCommands::resetSleep))
                                .then(Commands.literal("dimensions")
                                        .executes(RPGCommands::resetDimensions)
                                        .then(Commands.argument("entry", StringArgumentType.greedyString())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(getDimensionEntries(), builder))
                                                .executes(RPGCommands::resetDimensionEntry)))
                                .then(Commands.literal("mob_loot")
                                        .executes(RPGCommands::resetMobLoot)
                                        .then(Commands.argument("entry", StringArgumentType.greedyString())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(getMobEntries(), builder))
                                                .executes(RPGCommands::resetMobEntry)))))
                .then(Commands.literal("pvp")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("on").executes(ctx -> setPvP(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setPvP(ctx, false))))
                .then(Commands.literal("chat")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("on").executes(ctx -> setChat(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setChat(ctx, false)))
                        .then(Commands.literal("block")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> setChatPlayer(ctx, true))))
                        .then(Commands.literal("allow")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> setChatPlayer(ctx, false)))))
                .then(Commands.literal("dimension")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("block")
                                .then(Commands.argument("dimension", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> suggestDimensions(ctx, builder))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> setPlayerDimension(ctx, false, ""))
                                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                                        .executes(ctx -> setPlayerDimension(ctx, false,
                                                                StringArgumentType.getString(ctx, "message")))))))
                        .then(Commands.literal("allow")
                                .then(Commands.argument("dimension", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> suggestDimensions(ctx, builder))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> setPlayerDimension(ctx, true, ""))))))
        );
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        ModConfig.reload();
        PlayerDimensionData.load();
        context.getSource().sendSuccess(
                () -> Component.translatable("rpg_tweaks.command.reload.success"), true);
        return 1;
    }

    private static int resetAll(CommandContext<CommandSourceStack> context) {
        ModConfig config = ModConfig.getInstance();
        applyAdvancementsDefaults(config);
        applyOreXPDefaults(config);
        applyPlayerDefaults(config);
        applySleepDefaults(config);
        applyDimensionsDefaults(config);
        applyMobLootDefaults(config);
        OreXPHandler.invalidateCache();
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("rpg_tweaks.command.reset.all"), true);
        return 1;
    }

    private static int resetAdvancements(CommandContext<CommandSourceStack> context) {
        applyAdvancementsDefaults(ModConfig.getInstance());
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("rpg_tweaks.command.reset.category", "advancements"), true);
        return 1;
    }

    private static int resetOreXP(CommandContext<CommandSourceStack> context) {
        ModConfig config = ModConfig.getInstance();
        applyOreXPDefaults(config);
        OreXPHandler.invalidateCache();
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("rpg_tweaks.command.reset.category", "ore_xp"), true);
        return 1;
    }

    private static int resetOreEntry(CommandContext<CommandSourceStack> context) {
        String entry = StringArgumentType.getString(context, "entry");
        ModConfig config = ModConfig.getInstance();
        if (config.oreXPConfig.blockConfigs.containsKey(entry)) {
            config.oreXPConfig.blockConfigs.put(entry, new XPValues(0, 0));
        } else if (config.oreXPConfig.tagConfigs.containsKey(entry)) {
            config.oreXPConfig.tagConfigs.put(entry, new XPValues(0, 0));
        } else {
            context.getSource().sendFailure(Component.translatable("rpg_tweaks.command.reset.entry_not_found", entry));
            return 0;
        }
        OreXPHandler.invalidateCache();
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("rpg_tweaks.command.reset.entry_reset", entry), true);
        return 1;
    }

    private static int resetPlayer(CommandContext<CommandSourceStack> context) {
        applyPlayerDefaults(ModConfig.getInstance());
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("rpg_tweaks.command.reset.category", "player"), true);
        return 1;
    }

    private static int resetSleep(CommandContext<CommandSourceStack> context) {
        applySleepDefaults(ModConfig.getInstance());
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("rpg_tweaks.command.reset.category", "sleep"), true);
        return 1;
    }

    private static int resetDimensions(CommandContext<CommandSourceStack> context) {
        applyDimensionsDefaults(ModConfig.getInstance());
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("rpg_tweaks.command.reset.category", "dimensions"), true);
        return 1;
    }

    private static int resetDimensionEntry(CommandContext<CommandSourceStack> context) {
        String entry = StringArgumentType.getString(context, "entry");
        ModConfig config = ModConfig.getInstance();
        if (!config.blockedDimensions.containsKey(entry)) {
            context.getSource().sendSuccess(() -> Component.translatable("rpg_tweaks.command.reset.entry_reset", entry), true);
            return 0;
        }
        config.blockedDimensions.remove(entry);
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("rpg_tweaks.command.reset.entry", entry), true);
        return 1;
    }

    private static int resetMobLoot(CommandContext<CommandSourceStack> context) {
        applyMobLootDefaults(ModConfig.getInstance());
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("rpg_tweaks.command.reset.category", "mob_loot"), true);
        return 1;
    }

    private static int resetMobEntry(CommandContext<CommandSourceStack> context) {
        String entry = StringArgumentType.getString(context, "entry");
        ModConfig config = ModConfig.getInstance();
        if (!config.mobLootConfig.mobDrops.containsKey(entry)) {
            context.getSource().sendFailure(Component.translatable("rpg_tweaks.command.reset.entry_not_found", entry));
            return 0;
        }
        MobLootConfig.MobSackDrops fresh = new MobLootConfig.MobSackDrops();
        fresh.ensureDefaults();
        config.mobLootConfig.mobDrops.put(entry, fresh);
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("rpg_tweaks.command.reset.entry_reset", entry), true);
        return 1;
    }

    private static void applyAdvancementsDefaults(ModConfig config) {
        config.blockAdvancementXP = true;
        config.logXPBlocking = false;
        config.advancementXPReward = 0;
        config.goalXPReward = 0;
        config.challengeXPReward = 0;
    }

    private static void applyOreXPDefaults(ModConfig config) {
        config.enableCustomOreXP = true;
        config.oreXPFortuneBonus = 0.0f;
        config.logOreXP = false;
        config.oreXPConfig = new OreXPConfig();
        ModConfig config2 = config;
        config2.oreXPConfig.tagConfigs.put("minecraft:coal_ores", new XPValues(1, 3));
        config2.oreXPConfig.tagConfigs.put("minecraft:copper_ores", new XPValues(1, 3));
        config2.oreXPConfig.tagConfigs.put("minecraft:iron_ores", new XPValues(2, 4));
        config2.oreXPConfig.tagConfigs.put("minecraft:gold_ores", new XPValues(2, 5));
        config2.oreXPConfig.tagConfigs.put("minecraft:redstone_ores", new XPValues(3, 7));
        config2.oreXPConfig.tagConfigs.put("minecraft:lapis_ores", new XPValues(4, 8));
        config2.oreXPConfig.tagConfigs.put("minecraft:diamond_ores", new XPValues(5, 12));
        config2.oreXPConfig.tagConfigs.put("minecraft:emerald_ores", new XPValues(5, 12));
        config2.oreXPConfig.blockConfigs.put("minecraft:nether_quartz_ore", new XPValues(7, 15));
    }

    private static void applyPlayerDefaults(ModConfig config) {
        config.exhaustionRateMultiplier = 1.0f;
        config.naturalRegenRateMultiplier = 1.0f;
        config.durabilityMultiplier = 0f;
        config.maxStorableXP = 0;
        config.pvpEnabled = true;
        config.chatEnabled = true;
        config.chatBlockedPlayers = new java.util.ArrayList<>();
        config.deathXPLossPercent = 0f;
        config.deathRespawnEnabled = false;
        config.deathRespawnMinDistance = 100;
        config.deathRespawnMaxDistance = 300;
    }

    private static void applySleepDefaults(ModConfig config) {
        config.sleepFromNight = 0;
        config.sleepHealPercent = 0f;
        config.sleepHungerPoints = 0;
        config.sleepHungerChance = 0f;
    }

    private static void applyDimensionsDefaults(ModConfig config) {
        config.blockedDimensions = new java.util.LinkedHashMap<>();
        config.dimensionQuestRequirements = new java.util.LinkedHashMap<>();
    }

    private static void applyMobLootDefaults(ModConfig config) {
        config.lootSackLootingBonus = 5.0f;
        config.enableMobXP = false;
        config.mobXPLootingBonus = 0f;
        config.mobLootConfig = new MobLootConfig();
        config.mobLootConfig.initDefaults();
        config.requiredQuestEpic = "";
        config.requiredQuestLegendary = "";
    }

    private static Iterable<String> getOreEntries() {
        ModConfig config = ModConfig.getInstance();
        Set<String> entries = new LinkedHashSet<>();
        entries.addAll(config.oreXPConfig.blockConfigs.keySet());
        entries.addAll(config.oreXPConfig.tagConfigs.keySet());
        return entries;
    }

    private static Iterable<String> getMobEntries() {
        return ModConfig.getInstance().mobLootConfig.mobDrops.keySet();
    }

    private static Iterable<String> getDimensionEntries() {
        return ModConfig.getInstance().blockedDimensions.keySet();
    }

    private static int setPvP(CommandContext<CommandSourceStack> context, boolean enabled) {
        ModConfig.getInstance().pvpEnabled = enabled;
        ModConfig.save();
        context.getSource().sendSuccess(
                () -> Component.translatable(enabled ? "rpg_tweaks.command.pvp.enabled" : "rpg_tweaks.command.pvp.disabled"),
                true);
        return 1;
    }

    private static int setChat(CommandContext<CommandSourceStack> context, boolean enabled) {
        ModConfig.getInstance().chatEnabled = enabled;
        ModConfig.save();
        context.getSource().sendSuccess(
                () -> Component.translatable(enabled ? "rpg_tweaks.command.chat.enabled" : "rpg_tweaks.command.chat.disabled"),
                true);
        return 1;
    }

    private static int setChatPlayer(CommandContext<CommandSourceStack> context, boolean block) {
        String playerName = StringArgumentType.getString(context, "player").toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> blocked = ModConfig.getInstance().chatBlockedPlayers;
        if (block) {
            if (!blocked.contains(playerName)) blocked.add(playerName);
            context.getSource().sendSuccess(
                    () -> Component.translatable("rpg_tweaks.command.chat.player_blocked", playerName), true);
        } else {
            blocked.remove(playerName);
            context.getSource().sendSuccess(
                    () -> Component.translatable("rpg_tweaks.command.chat.player_unblocked", playerName), true);
        }
        ModConfig.save();
        return 1;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestDimensions(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        try {
            return SharedSuggestionProvider.suggestResource(
                    ctx.getSource().getServer().levelKeys().stream().map(key -> key.location()),
                    builder);
        } catch (Exception e) {
            return builder.buildFuture();
        }
    }

    private static int setPlayerDimension(CommandContext<CommandSourceStack> context, boolean isAllow, String customMessage) {
        CommandSourceStack source = context.getSource();
        ResourceLocation dimension = ResourceLocationArgument.getId(context, "dimension");
        String dimStr = dimension.toString();

        Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(context, "targets");
        } catch (Exception e) {
            source.sendFailure(Component.translatable("rpg_tweaks.command.error.players_only"));
            return 0;
        }

        for (ServerPlayer player : targets) {
            PlayerDimensionData.setDimension(player.getUUID(), player.getName().getString(), dimStr, isAllow, customMessage);
            final net.minecraft.network.chat.MutableComponent playerName = player.getName().copy();
            if (isAllow) {
                source.sendSuccess(() -> Component.translatable("rpg_tweaks.dimension.player_allowed", dimStr, playerName), true);
            } else {
                source.sendSuccess(() -> Component.translatable("rpg_tweaks.dimension.player_blocked", dimStr, playerName), true);
            }
        }
        return targets.size();
    }
}