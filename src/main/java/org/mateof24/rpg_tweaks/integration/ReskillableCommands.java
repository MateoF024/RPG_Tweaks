package org.mateof24.rpg_tweaks.integration;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import org.mateof24.rpg_tweaks.integration.ReskillableConfigManager.ConfigType;

import java.util.ArrayList;
import java.util.List;

public class ReskillableCommands {

    private static final List<String> VALID_SKILLS = List.of(
            "attack", "defense", "mining", "gathering",
            "farming", "building", "agility", "magic"
    );

    private static final SuggestionProvider<CommandSourceStack> SKILL_SUGGESTIONS = (context, builder) ->
            SharedSuggestionProvider.suggest(VALID_SKILLS, builder);

    private static final SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString),
                    builder
            );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("rpg_tweaks")
                        .requires(source -> source.hasPermission(2))
                        .then(registerSkillsCommand())
                        .then(registerCraftSkillsCommand())
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerSkillsCommand() {
        return Commands.literal("skills")
                .then(Commands.literal("add")
                        .then(Commands.argument("requirements", StringArgumentType.greedyString())
                                .suggests(SKILL_SUGGESTIONS)
                                .executes(context -> executeAdd(context, ConfigType.SKILL_LOCKS))
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests(ITEM_SUGGESTIONS)
                                .executes(context -> executeRemove(context, ConfigType.SKILL_LOCKS, true))
                        )
                        .executes(context -> executeRemove(context, ConfigType.SKILL_LOCKS, false))
                )
                .then(Commands.literal("info")
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests(ITEM_SUGGESTIONS)
                                .executes(context -> executeInfo(context, ConfigType.SKILL_LOCKS, true))
                        )
                        .executes(context -> executeInfo(context, ConfigType.SKILL_LOCKS, false))
                );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerCraftSkillsCommand() {
        return Commands.literal("craftskills")
                .then(Commands.literal("add")
                        .then(Commands.argument("requirements", StringArgumentType.greedyString())
                                .suggests(SKILL_SUGGESTIONS)
                                .executes(context -> executeCraftAdd(context, ConfigType.CRAFT_LOCKS))
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests(ITEM_SUGGESTIONS)
                                .executes(context -> executeRemove(context, ConfigType.CRAFT_LOCKS, true))
                        )
                        .executes(context -> executeRemove(context, ConfigType.CRAFT_LOCKS, false))
                )
                .then(Commands.literal("info")
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests(ITEM_SUGGESTIONS)
                                .executes(context -> executeInfo(context, ConfigType.CRAFT_LOCKS, true))
                        )
                        .executes(context -> executeInfo(context, ConfigType.CRAFT_LOCKS, false))
                );
    }

    private static String getItemId(CommandContext<CommandSourceStack> context, boolean hasItemArg)
            throws CommandSyntaxException {
        if (hasItemArg) {
            return ResourceLocationArgument.getId(context, "item").toString();
        }

        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            throw new CommandSyntaxException(null,
                    Component.translatable("rpg_tweaks.command.error.players_only"));
        }

        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.isEmpty()) {
            throw new CommandSyntaxException(null,
                    Component.translatable("rpg_tweaks.command.error.hand_empty"));
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
        return itemId.toString();
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context, ConfigType type) {
        String fullInput = StringArgumentType.getString(context, "requirements");
        return processAddCommand(context.getSource(), fullInput, type, context);
    }

    private static int executeCraftAdd(CommandContext<CommandSourceStack> context, ConfigType type) {
        String fullInput = StringArgumentType.getString(context, "requirements");
        return processAddCommand(context.getSource(), fullInput, type, context);
    }

    private static int processAddCommand(CommandSourceStack source, String fullInput,
                                         ConfigType type,
                                         CommandContext<CommandSourceStack> context) {
        try {
            if (!ReskillableConfigManager.isReskillableInstalled()) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.not_installed"));
                return 0;
            }

            String[] parts = fullInput.trim().split("\\s+");

            if (parts.length < 2) {
                String subCmd = type == ConfigType.CRAFT_LOCKS ? "craftskills" : "skills";
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.usage.add", subCmd));
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.usage.example_full"));
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.usage.example_hand"));
                return 0;
            }

            String itemId = null;
            int endIndex = parts.length;

            if (parts[parts.length - 1].contains(":")) {
                itemId = parts[parts.length - 1];
                endIndex = parts.length - 1;
            }

            if (itemId == null) {
                try {
                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                        source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.need_player"));
                        return 0;
                    }

                    ItemStack heldItem = player.getMainHandItem();

                    if (heldItem.isEmpty()) {
                        source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.hand_empty"));
                        source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.hand_example"));
                        return 0;
                    }

                    ResourceLocation itemIdRes = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
                    itemId = itemIdRes.toString();

                } catch (Exception e) {
                    source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.hand_get_error", e.getMessage()));
                    return 0;
                }
            }

            if (!ReskillableConfigManager.isValidItemId(itemId)) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.invalid_item", itemId));
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.item_format_hint"));
                return 0;
            }

            List<String> skillRequirements = new ArrayList<>();

            for (int i = 0; i < endIndex; i += 2) {
                if (i + 1 >= endIndex) {
                    source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.skill_no_level", parts[i]));
                    return 0;
                }

                String skill = parts[i].toLowerCase();
                String levelStr = parts[i + 1];

                if (!VALID_SKILLS.contains(skill)) {
                    source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.invalid_skill", skill));
                    source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.valid_skills", String.join(", ", VALID_SKILLS)));
                    return 0;
                }

                int level;
                try {
                    level = Integer.parseInt(levelStr);
                    if (level <= 0 || level > 100) {
                        source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.invalid_level_range", level));
                        return 0;
                    }
                } catch (NumberFormatException e) {
                    source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.invalid_level_nan", levelStr));
                    return 0;
                }

                skillRequirements.add(skill + ":" + level);
            }

            if (skillRequirements.isEmpty()) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.no_requirements"));
                return 0;
            }

            boolean success = ReskillableConfigManager.addItemSkillLock(type, itemId, skillRequirements);

            if (success) {
                final String finalItemId = itemId;
                final List<String> finalReqs = skillRequirements;
                source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.added", type.filename), true);
                source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.item", finalItemId), false);
                source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.requirements", String.join(", ", finalReqs)), false);
                return 1;
            } else {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.add_failed"));
                return 0;
            }

        } catch (Exception e) {
            source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.unexpected", e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context, ConfigType type, boolean hasItemArg) {
        CommandSourceStack source = context.getSource();

        try {
            if (!ReskillableConfigManager.isReskillableInstalled()) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.not_installed_short"));
                return 0;
            }

            String itemId = getItemId(context, hasItemArg);

            if (!ReskillableConfigManager.isValidItemId(itemId)) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.invalid_item", itemId));
                return 0;
            }

            boolean success = ReskillableConfigManager.removeItemSkillLock(type, itemId);

            if (success) {
                final String finalItemId = itemId;
                source.sendSuccess(
                        () -> Component.translatable("rpg_tweaks.reskillable.success.removed", type.filename, finalItemId),
                        true
                );
                return 1;
            } else {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.remove_failed", type.filename));
                return 0;
            }

        } catch (CommandSyntaxException e) {
            source.sendFailure((Component) e.getRawMessage());
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.generic", e.getMessage()));
            return 0;
        }
    }

    private static int executeInfo(CommandContext<CommandSourceStack> context, ConfigType type, boolean hasItemArg) {
        CommandSourceStack source = context.getSource();

        try {
            if (!ReskillableConfigManager.isReskillableInstalled()) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.not_installed_short"));
                return 0;
            }

            String itemId = getItemId(context, hasItemArg);

            if (!ReskillableConfigManager.isValidItemId(itemId)) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.invalid_item", itemId));
                return 0;
            }

            List<String> requirements = ReskillableConfigManager.getItemSkillLock(type, itemId);

            if (requirements == null || requirements.isEmpty()) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.info.no_requirements", itemId, type.filename));
                return 0;
            }

            final String finalItemId = itemId;
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.info.header", type.filename), false);
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.item", finalItemId), false);
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.requirements", String.join(", ", requirements)), false);

            return 1;

        } catch (CommandSyntaxException e) {
            source.sendFailure((Component) e.getRawMessage());
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.generic", e.getMessage()));
            return 0;
        }
    }
}