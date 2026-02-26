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
            throw new CommandSyntaxException(
                    null,
                    Component.literal("Only players can use this command without specifying an item.")
            );
        }

        ItemStack heldItem = player.getMainHandItem();

        if (heldItem.isEmpty()) {
            throw new CommandSyntaxException(
                    null,
                    Component.literal("You must either hold an item in your hand or specify an item in the command.")
            );
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
        return itemId.toString();
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context, ConfigType type) {
        String fullInput = StringArgumentType.getString(context, "requirements");
        return processAddCommand(context.getSource(), fullInput, type, "skill lock", context);
    }

    private static int executeCraftAdd(CommandContext<CommandSourceStack> context, ConfigType type) {
        String fullInput = StringArgumentType.getString(context, "requirements");
        return processAddCommand(context.getSource(), fullInput, type, "craft lock", context);
    }

    private static int processAddCommand(CommandSourceStack source, String fullInput,
                                         ConfigType type, String lockTypeName,
                                         CommandContext<CommandSourceStack> context) {
        try {
            if (!ReskillableConfigManager.isReskillableInstalled()) {
                source.sendFailure(Component.literal("§cError: Reskillable Reimagined is not installed or its configuration directory could not be found."));
                return 0;
            }

            String[] parts = fullInput.trim().split("\\s+");

            if (parts.length < 2) {
                source.sendFailure(Component.literal(
                        "§cIncorrect usage. Format: /rpg_tweaks " +
                                (type == ConfigType.CRAFT_LOCKS ? "craftskills" : "skills") +
                                " add <skill> <level> [<skill> <level> ...] [<item>]"
                ));
                source.sendFailure(Component.literal("§eExample: /rpg_tweaks skills add attack 15 agility 10 minecraft:diamond_sword"));
                source.sendFailure(Component.literal("§eOr without an item (use the one in your hand): /rpg_tweaks skills add attack 15 agility 10"));
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
                        source.sendFailure(Component.literal("§cYou must specify an item or be a player to use the item in your hand"));
                        return 0;
                    }

                    ItemStack heldItem = player.getMainHandItem();

                    if (heldItem.isEmpty()) {
                        source.sendFailure(Component.literal("§cYou must either hold an item in your hand or specify an item in the command"));
                        source.sendFailure(Component.literal("§eExample: /rpg_tweaks skills add attack 15 minecraft:diamond_sword"));
                        return 0;
                    }

                    ResourceLocation itemIdRes = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
                    itemId = itemIdRes.toString();

                } catch (Exception e) {
                    source.sendFailure(Component.literal("§cError getting the item from your hand: " + e.getMessage()));
                    return 0;
                }
            }

            if (!ReskillableConfigManager.isValidItemId(itemId)) {
                source.sendFailure(Component.literal("§cInvalid item format: " + itemId));
                source.sendFailure(Component.literal("§eCorrect format: modid:itemname (Example: minecraft:diamond_sword)"));
                return 0;
            }

            List<String> skillRequirements = new ArrayList<>();

            for (int i = 0; i < endIndex; i += 2) {
                if (i + 1 >= endIndex) {
                    source.sendFailure(Component.literal("§cError: Skill with no specified level near '" + parts[i] + "'"));
                    return 0;
                }

                String skill = parts[i].toLowerCase();
                String levelStr = parts[i + 1];

                if (!VALID_SKILLS.contains(skill)) {
                    source.sendFailure(Component.literal("§cInvalid skill: " + skill));
                    source.sendFailure(Component.literal("§eValid skills: " + String.join(", ", VALID_SKILLS)));
                    return 0;
                }

                int level;
                try {
                    level = Integer.parseInt(levelStr);
                    if (level <= 0 || level > 100) {
                        source.sendFailure(Component.literal("§cInvalid level: " + level + " (Must be integer between 1 and 100)"));
                        return 0;
                    }
                } catch (NumberFormatException e) {
                    source.sendFailure(Component.literal("§cInvalid level: '" + levelStr + "' is not a number."));
                    return 0;
                }

                skillRequirements.add(skill + ":" + level);
            }

            if (skillRequirements.isEmpty()) {
                source.sendFailure(Component.literal("§cYou must specify at least one skill requirement."));
                return 0;
            }

            boolean success = ReskillableConfigManager.addItemSkillLock(type, itemId, skillRequirements);

            if (success) {
                final String finalItemId = itemId;
                source.sendSuccess(
                        () -> Component.literal("§a✓ Item successfully added to " + type.filename),
                        true
                );
                source.sendSuccess(
                        () -> Component.literal("§7Item: §f" + finalItemId),
                        false
                );
                source.sendSuccess(
                        () -> Component.literal("§7Requirements: §f" + String.join(", ", skillRequirements)),
                        false
                );
                return 1;
            } else {
                source.sendFailure(Component.literal("§cError adding item. Check the logs for details."));
                return 0;
            }

        } catch (Exception e) {
            source.sendFailure(Component.literal("§cUnexpected error: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context, ConfigType type, boolean hasItemArg) {
        CommandSourceStack source = context.getSource();

        try {
            if (!ReskillableConfigManager.isReskillableInstalled()) {
                source.sendFailure(Component.literal("§cError: Reskillable Reimagined is not installed."));
                return 0;
            }

            String itemId = getItemId(context, hasItemArg);

            if (!ReskillableConfigManager.isValidItemId(itemId)) {
                source.sendFailure(Component.literal("§cInvalid item format: " + itemId));
                return 0;
            }

            boolean success = ReskillableConfigManager.removeItemSkillLock(type, itemId);

            if (success) {
                final String finalItemId = itemId;
                source.sendSuccess(
                        () -> Component.literal("§a✓ Item successfully removed from " + type.filename + ": §f" + finalItemId),
                        true
                );
                return 1;
            } else {
                source.sendFailure(Component.literal("§cThe item does not exist in " + type.filename + " or there was an error when deleting it."));
                return 0;
            }

        } catch (CommandSyntaxException e) {
            source.sendFailure((Component) e.getRawMessage());
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    private static int executeInfo(CommandContext<CommandSourceStack> context, ConfigType type, boolean hasItemArg) {
        CommandSourceStack source = context.getSource();

        try {
            if (!ReskillableConfigManager.isReskillableInstalled()) {
                source.sendFailure(Component.literal("§cError: Reskillable Reimagined is not installed."));
                return 0;
            }

            String itemId = getItemId(context, hasItemArg);

            if (!ReskillableConfigManager.isValidItemId(itemId)) {
                source.sendFailure(Component.literal("§cInvalid item format: " + itemId));
                return 0;
            }

            List<String> requirements = ReskillableConfigManager.getItemSkillLock(type, itemId);

            if (requirements == null || requirements.isEmpty()) {
                source.sendFailure(Component.literal("§eThe item §f" + itemId + "§e has no requirements configured in " + type.filename));
                return 0;
            }

            final String finalItemId = itemId;
            source.sendSuccess(
                    () -> Component.literal("§b=== Information of " + type.filename + " ==="),
                    false
            );
            source.sendSuccess(
                    () -> Component.literal("§7Item: §f" + finalItemId),
                    false
            );
            source.sendSuccess(
                    () -> Component.literal("§7Requirements: §f" + String.join(", ", requirements)),
                    false
            );

            return 1;

        } catch (CommandSyntaxException e) {
            source.sendFailure((Component) e.getRawMessage());
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
}