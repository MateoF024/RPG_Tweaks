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

    private static final SuggestionProvider<CommandSourceStack> SKILL_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    ReskillableConfigManager.getAllValidSkillIds(), builder);

    private static final SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString), builder);

    private static final SuggestionProvider<CommandSourceStack> ENTITY_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(
                    BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(ResourceLocation::toString), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("rpg_tweaks")
                        .requires(source -> source.hasPermission(2))
                        .then(registerSkillsCommand())
                        .then(registerCraftSkillsCommand())
                        .then(registerAttackSkillsCommand())
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerSkillsCommand() {
        return Commands.literal("skills")
                .then(Commands.literal("add")
                        .then(Commands.argument("requirements", StringArgumentType.greedyString())
                                .suggests(SKILL_SUGGESTIONS)
                                .executes(context -> executeAdd(context, ConfigType.SKILL_LOCKS))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests(ITEM_SUGGESTIONS)
                                .executes(context -> executeRemove(context, ConfigType.SKILL_LOCKS, true)))
                        .executes(context -> executeRemove(context, ConfigType.SKILL_LOCKS, false)))
                .then(Commands.literal("info")
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests(ITEM_SUGGESTIONS)
                                .executes(context -> executeInfo(context, ConfigType.SKILL_LOCKS, true)))
                        .executes(context -> executeInfo(context, ConfigType.SKILL_LOCKS, false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerCraftSkillsCommand() {
        return Commands.literal("craftskills")
                .then(Commands.literal("add")
                        .then(Commands.argument("requirements", StringArgumentType.greedyString())
                                .suggests(SKILL_SUGGESTIONS)
                                .executes(context -> executeAdd(context, ConfigType.CRAFT_LOCKS))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests(ITEM_SUGGESTIONS)
                                .executes(context -> executeRemove(context, ConfigType.CRAFT_LOCKS, true)))
                        .executes(context -> executeRemove(context, ConfigType.CRAFT_LOCKS, false)))
                .then(Commands.literal("info")
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests(ITEM_SUGGESTIONS)
                                .executes(context -> executeInfo(context, ConfigType.CRAFT_LOCKS, true)))
                        .executes(context -> executeInfo(context, ConfigType.CRAFT_LOCKS, false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerAttackSkillsCommand() {
        return Commands.literal("attackskills")
                .then(Commands.literal("add")
                        .then(Commands.argument("requirements", StringArgumentType.greedyString())
                                .suggests(SKILL_SUGGESTIONS)
                                .executes(context -> executeAttackAdd(context, ConfigType.ATTACK_LOCKS))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("entity", ResourceLocationArgument.id())
                                .suggests(ENTITY_SUGGESTIONS)
                                .executes(context -> executeAttackRemove(context, ConfigType.ATTACK_LOCKS, true)))
                        .executes(context -> executeAttackRemove(context, ConfigType.ATTACK_LOCKS, false)))
                .then(Commands.literal("info")
                        .then(Commands.argument("entity", ResourceLocationArgument.id())
                                .suggests(ENTITY_SUGGESTIONS)
                                .executes(context -> executeAttackInfo(context, ConfigType.ATTACK_LOCKS, true)))
                        .executes(context -> executeAttackInfo(context, ConfigType.ATTACK_LOCKS, false)));
    }

    private static String getItemId(CommandContext<CommandSourceStack> context, boolean hasArg)
            throws CommandSyntaxException {
        if (hasArg) return ResourceLocationArgument.getId(context, "item").toString();
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player))
            throw new CommandSyntaxException(null, Component.translatable("rpg_tweaks.command.error.players_only"));
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty())
            throw new CommandSyntaxException(null, Component.translatable("rpg_tweaks.command.error.hand_empty"));
        return BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
    }

    private static String getEntityId(CommandContext<CommandSourceStack> context, boolean hasArg)
            throws CommandSyntaxException {
        if (hasArg) return ResourceLocationArgument.getId(context, "entity").toString();
        throw new CommandSyntaxException(null,
                Component.translatable("rpg_tweaks.reskillable.error.entity_required"));
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context, ConfigType type) {
        return processAddCommand(context.getSource(),
                StringArgumentType.getString(context, "requirements"), type, false);
    }

    private static int executeAttackAdd(CommandContext<CommandSourceStack> context, ConfigType type) {
        return processAddCommand(context.getSource(),
                StringArgumentType.getString(context, "requirements"), type, true);
    }

    private static int processAddCommand(CommandSourceStack source, String fullInput,
                                         ConfigType type, boolean useEntityId) {
        try {
            if (!ReskillableConfigManager.isReskillableInstalled()) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.not_installed"));
                return 0;
            }

            String[] parts = fullInput.trim().split("\\s+");
            if (parts.length < 2) {
                String subCmd = type == ConfigType.CRAFT_LOCKS ? "craftskills"
                        : type == ConfigType.ATTACK_LOCKS ? "attackskills" : "skills";
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.usage.add", subCmd));
                return 0;
            }

            String targetId = null;
            int endIndex = parts.length;
            List<String> validSkills = ReskillableConfigManager.getAllValidSkillIds();

            if (parts[parts.length - 1].contains(":")) {
                targetId = parts[parts.length - 1];
                endIndex = parts.length - 1;
            }

            if (targetId == null) {
                if (useEntityId) {
                    source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.entity_required"));
                    return 0;
                }
                try {
                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                        source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.need_player"));
                        return 0;
                    }
                    ItemStack held = player.getMainHandItem();
                    if (held.isEmpty()) {
                        source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.hand_empty"));
                        return 0;
                    }
                    targetId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
                } catch (Exception e) {
                    source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.hand_get_error", e.getMessage()));
                    return 0;
                }
            }

            if (!ReskillableConfigManager.isValidItemId(targetId)) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.invalid_item", targetId));
                return 0;
            }

            List<String> skillRequirements = new ArrayList<>();
            for (int i = 0; i < endIndex; i += 2) {
                if (i + 1 >= endIndex) {
                    source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.skill_no_level", parts[i]));
                    return 0;
                }
                String skill = parts[i].toLowerCase(java.util.Locale.ROOT);
                String levelStr = parts[i + 1];

                if (!validSkills.contains(skill)) {
                    source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.invalid_skill", skill));
                    source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.valid_skills",
                            String.join(", ", validSkills)));
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

            boolean success = ReskillableConfigManager.addItemSkillLock(type, targetId, skillRequirements);
            if (success) {
                final String fId = targetId;
                final List<String> fReqs = skillRequirements;
                source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.added", type.filename), true);
                source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.item", fId), false);
                source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.requirements",
                        String.join(", ", fReqs)), false);
                return 1;
            } else {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.add_failed"));
                return 0;
            }
        } catch (Exception e) {
            source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.unexpected", e.getMessage()));
            return 0;
        }
    }

    private static int executeRemove(CommandContext<CommandSourceStack> context, ConfigType type, boolean hasArg) {
        CommandSourceStack source = context.getSource();
        try {
            if (!ReskillableConfigManager.isReskillableInstalled()) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.not_installed_short"));
                return 0;
            }
            String itemId = getItemId(context, hasArg);
            if (!ReskillableConfigManager.isValidItemId(itemId)) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.invalid_item", itemId));
                return 0;
            }
            boolean success = ReskillableConfigManager.removeItemSkillLock(type, itemId);
            if (success) {
                final String fId = itemId;
                source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.removed", type.filename, fId), true);
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

    private static int executeAttackRemove(CommandContext<CommandSourceStack> context, ConfigType type, boolean hasArg) {
        CommandSourceStack source = context.getSource();
        try {
            if (!ReskillableConfigManager.isReskillableInstalled()) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.not_installed_short"));
                return 0;
            }
            String entityId = getEntityId(context, hasArg);
            if (!ReskillableConfigManager.isValidItemId(entityId)) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.invalid_item", entityId));
                return 0;
            }
            boolean success = ReskillableConfigManager.removeItemSkillLock(type, entityId);
            if (success) {
                final String fId = entityId;
                source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.removed", type.filename, fId), true);
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

    private static int executeInfo(CommandContext<CommandSourceStack> context, ConfigType type, boolean hasArg) {
        CommandSourceStack source = context.getSource();
        try {
            if (!ReskillableConfigManager.isReskillableInstalled()) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.not_installed_short"));
                return 0;
            }
            String itemId = getItemId(context, hasArg);
            if (!ReskillableConfigManager.isValidItemId(itemId)) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.invalid_item", itemId));
                return 0;
            }
            List<String> requirements = ReskillableConfigManager.getItemSkillLock(type, itemId);
            if (requirements == null || requirements.isEmpty()) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.info.no_requirements", itemId, type.filename));
                return 0;
            }
            final String fId = itemId;
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.info.header", type.filename), false);
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.item", fId), false);
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.requirements",
                    String.join(", ", requirements)), false);
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure((Component) e.getRawMessage());
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.generic", e.getMessage()));
            return 0;
        }
    }

    private static int executeAttackInfo(CommandContext<CommandSourceStack> context, ConfigType type, boolean hasArg) {
        CommandSourceStack source = context.getSource();
        try {
            if (!ReskillableConfigManager.isReskillableInstalled()) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.not_installed_short"));
                return 0;
            }
            String entityId = getEntityId(context, hasArg);
            if (!ReskillableConfigManager.isValidItemId(entityId)) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.error.invalid_item", entityId));
                return 0;
            }
            List<String> requirements = ReskillableConfigManager.getItemSkillLock(type, entityId);
            if (requirements == null || requirements.isEmpty()) {
                source.sendFailure(Component.translatable("rpg_tweaks.reskillable.info.no_requirements", entityId, type.filename));
                return 0;
            }
            final String fId = entityId;
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.info.header", type.filename), false);
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.item", fId), false);
            source.sendSuccess(() -> Component.translatable("rpg_tweaks.reskillable.success.requirements",
                    String.join(", ", requirements)), false);
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