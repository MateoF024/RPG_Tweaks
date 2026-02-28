package org.mateof24.rpg_tweaks.integration;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ItemObliteratorCommands {

    private static final SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString),
                    builder
            );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("rpg_tweaks")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("banitem")
                                .then(Commands.literal("list")
                                        .executes(ItemObliteratorCommands::executeList)
                                )
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .suggests(ITEM_SUGGESTIONS)
                                        .executes(context ->
                                                executeBan(context,
                                                        ResourceLocationArgument.getId(context, "item").toString()))
                                )
                                .executes(context -> executeBan(context, null))
                        )
                        .then(Commands.literal("unbanitem")
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .suggests(ITEM_SUGGESTIONS)
                                        .executes(context ->
                                                executeUnban(context,
                                                        ResourceLocationArgument.getId(context, "item").toString()))
                                )
                                .executes(context -> executeUnban(context, null))
                        )
        );
    }

    private static int executeBan(CommandContext<CommandSourceStack> context, String rawItemArg) {
        CommandSourceStack source = context.getSource();

        if (!ItemObliteratorConfigManager.isItemObliteratorInstalled()) {
            source.sendFailure(Component.translatable("rpg_tweaks.obliterator.not_installed"));
            return 0;
        }

        String itemId;
        try {
            itemId = resolveItemId(source, rawItemArg);
        } catch (CommandSyntaxException e) {
            source.sendFailure((Component) e.getRawMessage());
            return 0;
        }

        if (!ItemObliteratorConfigManager.isValidItemId(itemId)) {
            source.sendFailure(Component.translatable("rpg_tweaks.obliterator.error.invalid_format", itemId));
            return 0;
        }

        if (ItemObliteratorConfigManager.isBlacklisted(itemId)) {
            source.sendFailure(Component.translatable("rpg_tweaks.obliterator.error.already_blacklisted", itemId));
            return 0;
        }

        boolean success = ItemObliteratorConfigManager.addToBlacklist(itemId);

        if (success) {
            final String finalId = itemId;
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.obliterator.success.banned"), true);
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.obliterator.success.item", finalId), false);
            return 1;
        } else {
            source.sendFailure(Component.translatable("rpg_tweaks.obliterator.error.ban_failed"));
            return 0;
        }
    }

    private static int executeUnban(CommandContext<CommandSourceStack> context, String rawItemArg) {
        CommandSourceStack source = context.getSource();

        if (!ItemObliteratorConfigManager.isItemObliteratorInstalled()) {
            source.sendFailure(Component.translatable("rpg_tweaks.obliterator.not_installed"));
            return 0;
        }

        String itemId;
        try {
            itemId = resolveItemId(source, rawItemArg);
        } catch (CommandSyntaxException e) {
            source.sendFailure((Component) e.getRawMessage());
            return 0;
        }

        if (!ItemObliteratorConfigManager.isValidItemId(itemId)) {
            source.sendFailure(Component.translatable("rpg_tweaks.obliterator.error.invalid_format_short", itemId));
            return 0;
        }

        boolean success = ItemObliteratorConfigManager.removeFromBlacklist(itemId);

        if (success) {
            final String finalId = itemId;
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.obliterator.success.unbanned"), true);
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.obliterator.success.item", finalId), false);
            return 1;
        } else {
            source.sendFailure(Component.translatable("rpg_tweaks.obliterator.error.not_blacklisted", itemId));
            return 0;
        }
    }

    private static int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!ItemObliteratorConfigManager.isItemObliteratorInstalled()) {
            source.sendFailure(Component.translatable("rpg_tweaks.obliterator.not_installed_short"));
            return 0;
        }

        List<String> blacklist = ItemObliteratorConfigManager.getBlacklist();

        if (blacklist.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.obliterator.list.empty"), false);
            return 1;
        }

        source.sendSuccess(() -> Component.translatable("rpg_tweaks.obliterator.list.header", blacklist.size()), false);

        for (String item : blacklist) {
            final String finalItem = item;
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.obliterator.list.entry", finalItem), false);
        }

        return 1;
    }

    private static String resolveItemId(CommandSourceStack source, String rawArg)
            throws CommandSyntaxException {

        if (rawArg != null && !rawArg.isBlank()) {
            return rawArg.trim();
        }

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            throw new CommandSyntaxException(null,
                    Component.translatable("rpg_tweaks.obliterator.error.players_only"));
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            throw new CommandSyntaxException(null,
                    Component.translatable("rpg_tweaks.obliterator.error.hand_empty"));
        }

        ResourceLocation key = BuiltInRegistries.ITEM.getKey(held.getItem());
        return key.toString();
    }
}