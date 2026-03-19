package org.mateof24.rpg_tweaks.integration;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public class FTBQuestsManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DETECTION_CLASS = "dev.ftb.mods.ftbquests.FTBQuests";

    private static volatile Boolean installed = null;
    private static volatile Method getTeamDataMethod = null;
    private static volatile Method isQuestCompleteMethod = null;
    private static volatile Method isChapterCompleteMethod = null;
    private static volatile Object questFileInstance = null;
    private static volatile boolean initAttempted = false;

    public static boolean isInstalled() {
        if (installed == null) {
            try {
                Class.forName(DETECTION_CLASS);
                installed = true;
            } catch (ClassNotFoundException e) {
                installed = false;
            }
        }
        return installed;
    }

    public static synchronized void init() {
        if (initAttempted || !isInstalled()) return;
        initAttempted = true;
        try {
            Class<?> serverQuestFileClass = Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile");

            for (java.lang.reflect.Field f : serverQuestFileClass.getDeclaredFields()) {
                if (f.getType().isAssignableFrom(serverQuestFileClass)) {
                    f.setAccessible(true);
                    questFileInstance = f.get(null);
                    break;
                }
            }

            Class<?> teamDataClass = null;
            for (Method m : serverQuestFileClass.getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == ServerPlayer.class) {
                    teamDataClass = m.getReturnType();
                    getTeamDataMethod = m;
                    break;
                }
            }

            if (teamDataClass != null) {
                for (Method m : teamDataClass.getMethods()) {
                    if (m.getParameterCount() == 1 && m.getReturnType() == boolean.class) {
                        String name = m.getName().toLowerCase();
                        if (name.contains("complete") && name.contains("quest")) {
                            isQuestCompleteMethod = m;
                        } else if (name.contains("complete") && name.contains("chapter")) {
                            isChapterCompleteMethod = m;
                        }
                    }
                }
            }

            LOGGER.info("[RPGTweaks/FTBQuests] Integration initialized. TeamData methods: quest={}, chapter={}",
                    isQuestCompleteMethod != null, isChapterCompleteMethod != null);

        } catch (Exception e) {
            LOGGER.warn("[RPGTweaks/FTBQuests] Init failed: {}", e.getMessage());
        }
    }

    public static boolean hasCompletedQuest(ServerPlayer player, String questId) {
        if (!isInstalled() || questId == null || questId.isBlank()) return true;
        try {
            if (getTeamDataMethod == null || isQuestCompleteMethod == null) return true;
            Object teamData = getTeamDataMethod.invoke(questFileInstance, player);
            if (teamData == null) return false;
            Object questFile = Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile")
                    .getMethod("get").invoke(null);
            Object quest = findObjectById(questFile, questId, "getQuest");
            if (quest == null) {
                LOGGER.warn("[RPGTweaks/FTBQuests] Quest not found: {}", questId);
                return false;
            }
            return (boolean) isQuestCompleteMethod.invoke(teamData, quest);
        } catch (Exception e) {
            LOGGER.warn("[RPGTweaks/FTBQuests] Quest check failed for {}: {}", questId, e.getMessage());
            return false;
        }
    }

    public static boolean hasCompletedChapter(ServerPlayer player, String chapterId) {
        if (!isInstalled() || chapterId == null || chapterId.isBlank()) return true;
        try {
            if (getTeamDataMethod == null) return true;
            Object teamData = getTeamDataMethod.invoke(questFileInstance, player);
            if (teamData == null) return false;
            Object questFile = Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile")
                    .getMethod("get").invoke(null);
            Object chapter = findObjectById(questFile, chapterId, "getChapter");
            if (chapter == null) {
                LOGGER.warn("[RPGTweaks/FTBQuests] Chapter not found: {}", chapterId);
                return false;
            }
            Method method = isChapterCompleteMethod != null ? isChapterCompleteMethod : isQuestCompleteMethod;
            if (method == null) return false;
            return (boolean) method.invoke(teamData, chapter);
        } catch (Exception e) {
            LOGGER.warn("[RPGTweaks/FTBQuests] Chapter check failed for {}: {}", chapterId, e.getMessage());
            return false;
        }
    }

    private static Object findObjectById(Object questFile, String id, String methodName) {
        try {
            for (Method m : questFile.getClass().getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == 1) {
                    Object result = m.invoke(questFile, id);
                    if (result != null) return result;
                }
            }
            for (Method m : questFile.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("byid") || m.getName().toLowerCase().contains("find")) {
                    if (m.getParameterCount() == 1) {
                        try {
                            Object result = m.invoke(questFile, id);
                            if (result != null) return result;
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[RPGTweaks/FTBQuests] findObjectById failed: {}", e.getMessage());
        }
        return null;
    }

    public static void invalidate() {
        installed = null;
        initAttempted = false;
        getTeamDataMethod = null;
        isQuestCompleteMethod = null;
        isChapterCompleteMethod = null;
        questFileInstance = null;
    }
}