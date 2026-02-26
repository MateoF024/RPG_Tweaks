package org.mateof24.rpg_tweaks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.mateof24.rpg_tweaks.config.ModConfig;

public class ReloadCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpg_tweaks")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(ReloadCommand::reloadConfig)
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
}