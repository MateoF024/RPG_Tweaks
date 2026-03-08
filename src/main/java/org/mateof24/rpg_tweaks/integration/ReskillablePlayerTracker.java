package org.mateof24.rpg_tweaks.integration;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.mateof24.rpg_tweaks.RPG_Tweaks;
import org.mateof24.rpg_tweaks.network.S2CUnlockNotificationPacket;
import org.slf4j.Logger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = RPG_Tweaks.MODID)
public class ReskillablePlayerTracker {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CHECK_INTERVAL = 20;

    private static final Map<UUID, Map<String, Integer>> playerLevels = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> cooldowns = new ConcurrentHashMap<>();

    private static volatile boolean initialized = false;
    private static volatile Method getModelMethod = null;
    private static volatile Method getLevelMethod = null;
    private static volatile Object[] skillEnumConstants = null;
    private static volatile String[] skillNames = null;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ReskillableConfigManager.isReskillableInstalled()) return;

        UUID uuid = player.getUUID();
        int cd = cooldowns.merge(uuid, -1, Integer::sum);
        if (cd > 0) return;
        cooldowns.put(uuid, CHECK_INTERVAL);

        if (!initialized) init(player);
        if (getModelMethod == null || getLevelMethod == null) return;
        if (ReskillableSkillCache.isEmpty()) return;

        try {
            Object model = getModelMethod.invoke(null, player);
            if (model == null) return;

            Map<String, Integer> cached = playerLevels.computeIfAbsent(uuid, k -> new HashMap<>());

            for (int i = 0; i < skillEnumConstants.length; i++) {
                Object skillEnum = skillEnumConstants[i];
                String skillName = skillNames[i];

                int current;
                try {
                    Object result = getLevelMethod.invoke(model, skillEnum);
                    current = ((Number) result).intValue();
                } catch (Exception e) {
                    continue;
                }

                int previous = cached.getOrDefault(skillName, -1);
                if (previous < 0) {
                    cached.put(skillName, current);
                    continue;
                }
                if (current > previous) {
                    LOGGER.info("[RPGTweaks/Tracker] {} leveled {} {} → {}", player.getName().getString(), skillName, previous, current);
                    for (int lvl = previous + 1; lvl <= current; lvl++) {
                        List<String> unlocked = ReskillableSkillCache.getUnlockedItems(skillName, lvl);
                        if (!unlocked.isEmpty()) {
                            S2CUnlockNotificationPacket.send(player, skillName, lvl, unlocked);
                        }
                    }
                    cached.put(skillName, current);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("[RPGTweaks/Tracker] Tick error: {}", e.getMessage());
        }
    }

    public static void clearPlayer(UUID uuid) {
        playerLevels.remove(uuid);
        cooldowns.remove(uuid);
    }

    private static synchronized void init(Player player) {
        if (initialized) return;
        initialized = true;

        try {
            Class<?> skillModelClass = Class.forName("net.bandit.reskillable.common.capabilities.SkillModel");
            Class<?> skillEnumClass  = Class.forName("net.bandit.reskillable.common.commands.skills.Skill");

            Method gm = skillModelClass.getMethod("get", Player.class);
            Method gl = skillModelClass.getMethod("getSkillLevel", skillEnumClass);

            Object[] constants = skillEnumClass.getEnumConstants();
            String[] names = new String[constants.length];
            for (int i = 0; i < constants.length; i++) {
                names[i] = constants[i].toString().toLowerCase(Locale.ROOT);
            }

            Object testModel = gm.invoke(null, player);
            if (testModel == null) {
                LOGGER.warn("[RPGTweaks/Tracker] SkillModel.get() returned null for {}. Toasts DISABLED.", player.getName().getString());
                return;
            }

            getModelMethod    = gm;
            getLevelMethod    = gl;
            skillEnumConstants = constants;
            skillNames         = names;

            LOGGER.info("[RPGTweaks/Tracker] Initialized. Skills: {}", Arrays.toString(names));
            for (int i = 0; i < constants.length; i++) {
                int lvl = ((Number) gl.invoke(testModel, constants[i])).intValue();
                LOGGER.info("[RPGTweaks/Tracker]   {} = {}", names[i], lvl);
            }
        } catch (ClassNotFoundException e) {
            LOGGER.warn("[RPGTweaks/Tracker] Reskillable classes not found: {}. Toasts DISABLED.", e.getMessage());
        } catch (Exception e) {
            LOGGER.warn("[RPGTweaks/Tracker] Init failed: {}. Toasts DISABLED.", e.getMessage());
        }
    }
}