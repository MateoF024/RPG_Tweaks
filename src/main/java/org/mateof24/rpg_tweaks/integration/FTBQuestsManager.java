package org.mateof24.rpg_tweaks.integration;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

public class FTBQuestsManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile Boolean installed = null;
    private static volatile boolean initialized = false;

    private static Method apiMethod;
    private static Method getQuestFileMethod;
    private static Method getRewardMethod;
    private static Method getTeamDataMethod;
    private static Method isRewardClaimedMethod;
    private static Method isCompletedMethod;
    private static Method getQuestFromRewardMethod;
    private static Method getQuestObjectMethod;

    public static boolean isInstalled() {
        if (installed == null) {
            try {
                Class.forName("dev.ftb.mods.ftbquests.api.FTBQuestsAPI");
                installed = true;
                LOGGER.info("[RPGTweaks/FTBQuests] FTB Quests detected");
            } catch (ClassNotFoundException e) {
                installed = false;
            }
        }
        return installed;
    }

    public static synchronized void init() {
        if (initialized || !isInstalled()) return;
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbquests.api.FTBQuestsAPI");
            Class<?> teamDataClass = Class.forName("dev.ftb.mods.ftbquests.quest.TeamData");
            Class<?> rewardClass = Class.forName("dev.ftb.mods.ftbquests.quest.reward.Reward");
            Class<?> questObjectClass = Class.forName("dev.ftb.mods.ftbquests.quest.QuestObject");
            Class<?> questFileClass = Class.forName("dev.ftb.mods.ftbquests.quest.BaseQuestFile");

            apiMethod = apiClass.getMethod("api");
            Object apiObj = apiMethod.invoke(null);
            if (apiObj == null) throw new Exception("FTBQuestsAPI.api() returned null");

            getQuestFileMethod = apiObj.getClass().getMethod("getQuestFile", boolean.class);
            getRewardMethod = questFileClass.getMethod("getReward", long.class);
            getTeamDataMethod = questFileClass.getMethod("getTeamData", Player.class);
            isRewardClaimedMethod = teamDataClass.getMethod("isRewardClaimed", UUID.class, rewardClass);
            isCompletedMethod = teamDataClass.getMethod("isCompleted", questObjectClass);
            getQuestFromRewardMethod = rewardClass.getMethod("getQuest");
            getQuestObjectMethod = questFileClass.getMethod("get", long.class);

            Object testFile = getQuestFileMethod.invoke(apiObj, false);
            if (testFile == null) throw new Exception("getQuestFile(false) returned null — server not ready yet");

            initialized = true;
            LOGGER.info("[RPGTweaks/FTBQuests] Reflection OK via FTBQuestsAPI: api={} getQuestFile={} getReward={} getTeamData={} isRewardClaimed={} isCompleted={} getQuest={}",
                    apiMethod != null, getQuestFileMethod != null, getRewardMethod != null,
                    getTeamDataMethod != null, isRewardClaimedMethod != null,
                    isCompletedMethod != null, getQuestFromRewardMethod != null);
        } catch (Exception e) {
            initialized = false;
            apiMethod = null;
            getQuestFileMethod = null;
            getRewardMethod = null;
            getTeamDataMethod = null;
            isRewardClaimedMethod = null;
            isCompletedMethod = null;
            getQuestFromRewardMethod = null;
            LOGGER.debug("[RPGTweaks/FTBQuests] Init not ready yet: {}", e.getMessage());
        }
    }

    public static boolean isFullyInitialized() {
        return initialized
                && apiMethod != null && getQuestFileMethod != null
                && getRewardMethod != null && getTeamDataMethod != null
                && isRewardClaimedMethod != null && isCompletedMethod != null
                && getQuestFromRewardMethod != null;
    }

    private static Object getServerFile() {
        if (!isFullyInitialized()) return null;
        try {
            Object apiObj = apiMethod.invoke(null);
            if (apiObj == null) return null;
            return getQuestFileMethod.invoke(apiObj, false);
        } catch (Exception e) {
            LOGGER.debug("[RPGTweaks/FTBQuests] getServerFile error: {}", e.getMessage());
            return null;
        }
    }

    public static boolean hasClaimedReward(ServerPlayer player, String hexId) {
        if (hexId == null || hexId.isBlank()) return true;
        if (!isFullyInitialized()) return false;
        try {
            long id = Long.parseUnsignedLong(hexId.trim(), 16);
            Object sqf = getServerFile();
            if (sqf == null) return false;

            Object reward = getRewardMethod.invoke(sqf, id);
            if (reward == null) {
                LOGGER.warn("[RPGTweaks/FTBQuests] hasClaimedReward: reward not found id={}", hexId);
                return false;
            }

            @SuppressWarnings("unchecked")
            Optional<Object> optTeam = (Optional<Object>) getTeamDataMethod.invoke(sqf, player);
            if (optTeam == null || optTeam.isEmpty()) return false;

            boolean claimed = (boolean) isRewardClaimedMethod.invoke(optTeam.get(), player.getUUID(), reward);
            LOGGER.info("[RPGTweaks/FTBQuests] hasClaimedReward id={} player={} -> {}", hexId, player.getName().getString(), claimed);
            return claimed;
        } catch (NumberFormatException e) {
            LOGGER.warn("[RPGTweaks/FTBQuests] Invalid reward ID (hex): {}", hexId);
            return false;
        } catch (Exception e) {
            LOGGER.warn("[RPGTweaks/FTBQuests] hasClaimedReward error id={}: {} {}", hexId, e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    public static boolean hasCompletedQuestForReward(ServerPlayer player, String hexId) {
        if (hexId == null || hexId.isBlank()) return true;
        if (!isFullyInitialized()) return false;
        try {
            long id = Long.parseUnsignedLong(hexId.trim(), 16);
            Object sqf = getServerFile();
            if (sqf == null) return false;

            Object reward = getRewardMethod.invoke(sqf, id);
            if (reward == null) {
                LOGGER.warn("[RPGTweaks/FTBQuests] hasCompletedQuestForReward: reward not found id={}", hexId);
                return false;
            }

            Object quest = getQuestFromRewardMethod.invoke(reward);
            if (quest == null) {
                LOGGER.warn("[RPGTweaks/FTBQuests] hasCompletedQuestForReward: quest not found for reward id={}", hexId);
                return false;
            }

            @SuppressWarnings("unchecked")
            Optional<Object> optTeam = (Optional<Object>) getTeamDataMethod.invoke(sqf, player);
            if (optTeam == null || optTeam.isEmpty()) return false;

            boolean completed = (boolean) isCompletedMethod.invoke(optTeam.get(), quest);
            LOGGER.info("[RPGTweaks/FTBQuests] hasCompletedQuestForReward id={} player={} -> {}", hexId, player.getName().getString(), completed);
            return completed;
        } catch (NumberFormatException e) {
            LOGGER.warn("[RPGTweaks/FTBQuests] Invalid reward ID (hex): {}", hexId);
            return false;
        } catch (Exception e) {
            LOGGER.warn("[RPGTweaks/FTBQuests] hasCompletedQuestForReward error id={}: {} {}", hexId, e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    public static boolean hasCompletedQuest(ServerPlayer player, String hexId) {
        if (hexId == null || hexId.isBlank()) return true;
        if (!isFullyInitialized()) return false;
        try {
            long id = Long.parseUnsignedLong(hexId.trim(), 16);
            Object sqf = getServerFile();
            if (sqf == null) return false;

            Object quest = getQuestObjectMethod.invoke(sqf, id);
            if (quest == null) {
                LOGGER.warn("[RPGTweaks/FTBQuests] hasCompletedQuest: quest not found id={}", hexId);
                return false;
            }

            @SuppressWarnings("unchecked")
            Optional<Object> optTeam = (Optional<Object>) getTeamDataMethod.invoke(sqf, player);
            if (optTeam == null || optTeam.isEmpty()) return false;

            boolean completed = (boolean) isCompletedMethod.invoke(optTeam.get(), quest);
            LOGGER.info("[RPGTweaks/FTBQuests] hasCompletedQuest id={} player={} -> {}", hexId, player.getName().getString(), completed);
            return completed;
        } catch (NumberFormatException e) {
            LOGGER.warn("[RPGTweaks/FTBQuests] Invalid quest ID (hex): {}", hexId);
            return false;
        } catch (Exception e) {
            LOGGER.warn("[RPGTweaks/FTBQuests] hasCompletedQuest error id={}: {} {}", hexId, e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    public static void invalidate() {
        installed = null;
        initialized = false;
        apiMethod = null;
        getQuestFileMethod = null;
        getRewardMethod = null;
        getTeamDataMethod = null;
        isRewardClaimedMethod = null;
        isCompletedMethod = null;
        getQuestFromRewardMethod = null;
        getQuestObjectMethod = null;
    }
}