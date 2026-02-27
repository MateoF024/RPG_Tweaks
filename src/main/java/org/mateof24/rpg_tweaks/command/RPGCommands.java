package org.mateof24.rpg_tweaks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.mateof24.rpg_tweaks.config.ModConfig;

public class RPGCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpg_tweaks")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(RPGCommands::reloadConfig)
                )
                .then(Commands.literal("pvp")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("on").executes(ctx -> setPvP(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> setPvP(ctx, false)))
                )
        );
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        ModConfig.reload();
        context.getSource().sendSuccess(
                () -> Component.literal("§aRPG Tweaks config reloaded!"),
                true
        );
        return 1;
    }

    private static int setPvP(CommandContext<CommandSourceStack> context, boolean enabled) {
        ModConfig.getInstance().pvpEnabled = enabled;
        ModConfig.save();
        context.getSource().sendSuccess(
                () -> Component.literal("§aPvP " + (enabled ? "§aenabled" : "§cdisabled") + "§a."),
                true
        );
        return 1;
    }
}