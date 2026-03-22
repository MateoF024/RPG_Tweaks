package org.mateof24.rpg_tweaks.integration;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.mateof24.rpg_tweaks.RPG_Tweaks;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.mateof24.rpg_tweaks.data.PlayerDimensionData;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = RPG_Tweaks.MODID)
public class FTBQuestsPlayerTracker {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int INTERVAL = 60;

    private static final Map<UUID, Integer> cooldowns = new ConcurrentHashMap<>();
    private static final Set<String> processedRewards = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!FTBQuestsManager.isInstalled()) return;

        UUID uuid = player.getUUID();
        int cd = cooldowns.merge(uuid, -1, Integer::sum);
        if (cd > 0) return;
        cooldowns.put(uuid, INTERVAL);

        FTBQuestsManager.init();
        if (!FTBQuestsManager.isFullyInitialized()) return;

        ModConfig cfg = ModConfig.getInstance();

        if (cfg.questSkillRewards != null && !cfg.questSkillRewards.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : cfg.questSkillRewards.entrySet()) {
                String rewardId = entry.getKey();
                String key = uuid + ":skill:" + rewardId;
                if (processedRewards.contains(key)) continue;

                if (FTBQuestsManager.hasClaimedReward(player, rewardId)) {
                    LOGGER.info("[RPGTweaks/FTBQuests] Applying skill rewards for reward={} player={}",
                            rewardId, player.getName().getString());
                    boolean ok = applySkillRewards(player, entry.getValue());
                    if (ok) processedRewards.add(key);
                }
            }
        }

        if (cfg.dimensionQuestRequirements != null && !cfg.dimensionQuestRequirements.isEmpty()) {
            for (Map.Entry<String, String> entry : cfg.dimensionQuestRequirements.entrySet()) {
                String dimension = entry.getKey();
                String rewardId = entry.getValue();
                if (rewardId == null || rewardId.isBlank()) continue;

                if (PlayerDimensionData.isAllowed(uuid, dimension)) continue;
                if (PlayerDimensionData.isBlocked(uuid, dimension)) continue;

                String key = uuid + ":dim:" + rewardId + ":" + dimension;
                if (processedRewards.contains(key)) continue;

                if (FTBQuestsManager.hasClaimedReward(player, rewardId)) {
                    LOGGER.info("[RPGTweaks/FTBQuests] Granting dimension={} to player={} via reward={}",
                            dimension, player.getName().getString(), rewardId);
                    PlayerDimensionData.setDimension(uuid, player.getName().getString(), dimension, true, "");
                    processedRewards.add(key);
                }
            }
        }
    }

    public static void clearPlayer(UUID uuid) {
        cooldowns.remove(uuid);
        processedRewards.removeIf(k -> k.startsWith(uuid.toString()));
    }

    private static boolean applySkillRewards(ServerPlayer player, List<String> requirements) {
        if (!ReskillableConfigManager.isReskillableInstalled()) {
            LOGGER.warn("[RPGTweaks/FTBQuests] applySkillRewards: Reskillable not installed");
            return false;
        }
        if (player.getServer() == null) return false;

        boolean anyApplied = false;
        for (String req : requirements) {
            String[] parts = req.split(":");
            if (parts.length != 2) continue;
            String skill = parts[0].toLowerCase(java.util.Locale.ROOT);
            int level;
            try { level = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { continue; }

            String cmd = "skills add " + player.getName().getString() + " " + skill + " " + level;
            try {
                player.getServer().getCommands().performPrefixedCommand(
                        player.getServer().createCommandSourceStack()
                                .withSuppressedOutput()
                                .withPermission(4),
                        cmd
                );
                LOGGER.info("[RPGTweaks/FTBQuests] Executed '{}'", cmd);
                anyApplied = true;
            } catch (Exception e) {
                LOGGER.warn("[RPGTweaks/FTBQuests] Command failed '{}': {}", cmd, e.getMessage());
            }
        }

        return anyApplied;
    }
}