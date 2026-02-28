package org.mateof24.rpg_tweaks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.mateof24.rpg_tweaks.data.PlayerDimensionData;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

public class RPGCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpg_tweaks")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(RPGCommands::reloadConfig))
                .then(Commands.literal("pvp")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("on").executes(ctx -> setPvP(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setPvP(ctx, false))))
                .then(Commands.literal("dimension")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("block")
                                .then(Commands.argument("dimension", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> suggestDimensions(ctx, builder))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> setPlayerDimension(ctx, false)))))
                        .then(Commands.literal("allow")
                                .then(Commands.argument("dimension", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> suggestDimensions(ctx, builder))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> setPlayerDimension(ctx, true))))))
        );
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        ModConfig.reload();
        PlayerDimensionData.load();
        context.getSource().sendSuccess(
                () -> Component.translatable("rpg_tweaks.command.reload.success"), true);
        return 1;
    }

    private static int setPvP(CommandContext<CommandSourceStack> context, boolean enabled) {
        ModConfig.getInstance().pvpEnabled = enabled;
        ModConfig.save();
        context.getSource().sendSuccess(
                () -> Component.translatable(enabled ? "rpg_tweaks.command.pvp.enabled" : "rpg_tweaks.command.pvp.disabled"),
                true);
        return 1;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestDimensions(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
            com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        try {
            return SharedSuggestionProvider.suggestResource(
                    ctx.getSource().getServer().levelKeys().stream()
                            .map(key -> key.location()),
                    builder);
        } catch (Exception e) {
            return builder.buildFuture();
        }
    }

    private static int setPlayerDimension(CommandContext<CommandSourceStack> context, boolean isAllow) {
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
            PlayerDimensionData.setDimension(player.getUUID(), player.getName().getString(), dimStr, isAllow);
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