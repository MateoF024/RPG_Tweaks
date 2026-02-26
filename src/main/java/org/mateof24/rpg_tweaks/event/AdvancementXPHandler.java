package org.mateof24.rpg_tweaks.event;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.slf4j.Logger;

@EventBusSubscriber(modid = org.mateof24.rpg_tweaks.RPG_Tweaks.MODID)
public class AdvancementXPHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final java.util.Map<java.util.UUID, XPSnapshot> xpSnapshots =
            new java.util.concurrent.ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onAdvancementProgress(AdvancementEvent.AdvancementProgressEvent event) {
        if (!ModConfig.getInstance().blockAdvancementXP) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            if (event.getProgressType() == AdvancementEvent.AdvancementProgressEvent.ProgressType.GRANT) {
                captureXPSnapshot(player);
            }
        }
    }

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!ModConfig.getInstance().blockAdvancementXP) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            restoreXPSnapshot(player, event.getAdvancement().id().toString());
        }
    }

    private static void captureXPSnapshot(ServerPlayer player) {
        XPSnapshot snapshot = new XPSnapshot(
                player.experienceLevel,
                player.experienceProgress,
                player.totalExperience
        );

        xpSnapshots.put(player.getUUID(), snapshot);

        if (ModConfig.getInstance().logXPBlocking) {
            LOGGER.debug("Captured XP snapshot for {}: Level={}, Progress={}, Total={}",
                    player.getName().getString(),
                    snapshot.level,
                    snapshot.progress,
                    snapshot.totalXP
            );
        }
    }

    private static void restoreXPSnapshot(ServerPlayer player, String advancementId) {
        XPSnapshot snapshot = xpSnapshots.remove(player.getUUID());

        if (snapshot == null) {
            return;
        }

        player.experienceLevel = snapshot.level;
        player.experienceProgress = snapshot.progress;
        player.totalExperience = snapshot.totalXP;

        player.connection.send(
                new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(
                        player.experienceProgress,
                        player.totalExperience,
                        player.experienceLevel
                )
        );

        if (ModConfig.getInstance().logXPBlocking) {
            LOGGER.info("XP locked from advancement '{}' for {}. XP restored to: Level={}, Progress={:.2f}%, Total={}",
                    advancementId,
                    player.getName().getString(),
                    snapshot.level,
                    snapshot.progress * 100,
                    snapshot.totalXP
            );
        }
    }

    public static void clearPlayerSnapshot(ServerPlayer player) {
        xpSnapshots.remove(player.getUUID());
    }

    private static class XPSnapshot {
        final int level;
        final float progress;
        final int totalXP;

        XPSnapshot(int level, float progress, int totalXP) {
            this.level = level;
            this.progress = progress;
            this.totalXP = totalXP;
        }
    }
}