package org.mateof24.rpg_tweaks.integration;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.mateof24.rpg_tweaks.RPG_Tweaks;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = RPG_Tweaks.MODID)
public class FTBQuestsEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void onQuestCompleted(ServerPlayer player, String questId) {
        if (!ReskillableConfigManager.isReskillableInstalled()) return;
        Map<String, List<String>> rewards = ModConfig.getInstance().questSkillRewards;
        if (rewards == null || rewards.isEmpty()) return;
        List<String> skills = rewards.get(questId);
        if (skills == null || skills.isEmpty()) return;

        for (String skillReq : skills) {
            String[] parts = skillReq.split(":");
            if (parts.length != 2) continue;
            try {
                LOGGER.info("[RPGTweaks/FTBQuests] Granting skill reward {} to {} for quest {}",
                        skillReq, player.getName().getString(), questId);
            } catch (Exception e) {
                LOGGER.warn("[RPGTweaks/FTBQuests] Failed to apply skill reward {}: {}", skillReq, e.getMessage());
            }
        }

        applySkillRewards(player, skills);
    }

    private static void applySkillRewards(ServerPlayer player, List<String> skillRequirements) {
        try {
            Class<?> skillModelClass = Class.forName("net.bandit.reskillable.common.capabilities.SkillModel");
            Class<?> skillEnumClass = Class.forName("net.bandit.reskillable.common.skills.Skill");
            java.lang.reflect.Method getModel = skillModelClass.getMethod("get", net.minecraft.world.entity.player.Player.class);
            java.lang.reflect.Method setLevel = null;
            for (java.lang.reflect.Method m : skillModelClass.getMethods()) {
                if (m.getName().toLowerCase().contains("setlevel") || m.getName().toLowerCase().contains("set_level")) {
                    if (m.getParameterCount() == 2) { setLevel = m; break; }
                }
            }
            if (setLevel == null) return;

            Object model = getModel.invoke(null, player);
            if (model == null) return;

            Object[] constants = skillEnumClass.getEnumConstants();
            for (String req : skillRequirements) {
                String[] parts = req.split(":");
                if (parts.length != 2) continue;
                String skillName = parts[0].toLowerCase();
                int level;
                try { level = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { continue; }

                for (Object constant : constants) {
                    if (constant.toString().toLowerCase(java.util.Locale.ROOT).equals(skillName)) {
                        try {
                            setLevel.invoke(model, constant, level);
                            LOGGER.info("[RPGTweaks/FTBQuests] Set {} skill to {} for {}",
                                    skillName, level, player.getName().getString());
                        } catch (Exception e) {
                            LOGGER.warn("[RPGTweaks/FTBQuests] Failed to set skill {}: {}", skillName, e.getMessage());
                        }
                        break;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            LOGGER.warn("[RPGTweaks/FTBQuests] Reskillable classes not found for skill rewards");
        } catch (Exception e) {
            LOGGER.warn("[RPGTweaks/FTBQuests] applySkillRewards error: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!FTBQuestsManager.isInstalled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        FTBQuestsManager.init();
    }
}