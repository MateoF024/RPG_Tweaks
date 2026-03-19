package org.mateof24.rpg_tweaks.integration;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public class FTBQuestsQuestListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean registered = false;

    public static void tryRegister() {
        if (registered || !FTBQuestsManager.isInstalled()) return;
        try {
            Class<?> eventClass = Class.forName("dev.ftb.mods.ftbquests.events.ObjectCompletedEvent");
            registerDynamic(eventClass);
            registered = true;
            LOGGER.info("[RPGTweaks/FTBQuests] Quest completion listener registered.");
        } catch (ClassNotFoundException e1) {
            try {
                Class<?> eventClass = Class.forName("dev.ftb.mods.ftbquests.api.event.QuestCompletedEvent");
                registerDynamic(eventClass);
                registered = true;
                LOGGER.info("[RPGTweaks/FTBQuests] Quest completion listener registered (alt class).");
            } catch (ClassNotFoundException e2) {
                LOGGER.warn("[RPGTweaks/FTBQuests] Could not find quest completion event class. Bridge disabled.");
            }
        }
    }

    private static void registerDynamic(Class<?> eventClass) {
        try {
            java.lang.reflect.Method addListenerMethod = null;
            for (java.lang.reflect.Method m : NeoForge.EVENT_BUS.getClass().getMethods()) {
                if (m.getName().equals("addListener") && m.getParameterCount() == 2) {
                    java.lang.reflect.Parameter[] params = m.getParameters();
                    if (params[0].getType() == Class.class &&
                            java.util.function.Consumer.class.isAssignableFrom(params[1].getType())) {
                        addListenerMethod = m;
                        break;
                    }
                }
            }
            if (addListenerMethod == null) {
                LOGGER.warn("[RPGTweaks/FTBQuests] Could not find addListener(Class, Consumer) method.");
                return;
            }

            java.util.function.Consumer<Object> consumer = event -> {
                try {
                    ServerPlayer player = extractPlayer(event);
                    String questId = extractQuestId(event);
                    if (player != null && questId != null) {
                        FTBQuestsEventHandler.onQuestCompleted(player, questId);
                    }
                } catch (Exception e) {
                    LOGGER.warn("[RPGTweaks/FTBQuests] Error handling quest event: {}", e.getMessage());
                }
            };

            addListenerMethod.invoke(NeoForge.EVENT_BUS, eventClass, consumer);
        } catch (Exception e) {
            LOGGER.warn("[RPGTweaks/FTBQuests] registerDynamic failed: {}", e.getMessage());
        }
    }

    private static ServerPlayer extractPlayer(Object event) {
        try {
            for (Method m : event.getClass().getMethods()) {
                if (m.getParameterCount() == 0) {
                    Object result = m.invoke(event);
                    if (result instanceof ServerPlayer p) return p;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String extractQuestId(Object event) {
        try {
            for (Method m : event.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == String.class) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("id") || name.contains("quest")) {
                        return (String) m.invoke(event);
                    }
                }
                if (m.getParameterCount() == 0) {
                    Object result = m.invoke(event);
                    if (result != null && !(result instanceof ServerPlayer)) {
                        for (Method qm : result.getClass().getMethods()) {
                            if (qm.getParameterCount() == 0 && qm.getReturnType() == String.class) {
                                String val = (String) qm.invoke(result);
                                if (val != null) return val;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}