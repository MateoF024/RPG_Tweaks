package org.mateof24.rpg_tweaks.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.mateof24.rpg_tweaks.RPG_Tweaks;
import org.mateof24.rpg_tweaks.config.ModConfig;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.server.level.ServerLevel;


@EventBusSubscriber(modid = RPG_Tweaks.MODID)
public class DeathPenaltyHandler {

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ModConfig config = ModConfig.getInstance();
        if (config.deathXPLossPercent <= 0f) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int originalXP = player.totalExperience;
        int xpToKeep = Math.round(originalXP * (1f - config.deathXPLossPercent / 100f));

        player.totalExperience = 0;
        player.experienceLevel = 0;
        player.experienceProgress = 0f;

        if (xpToKeep > 0) {
            ExperienceOrb orb = new ExperienceOrb(serverLevel,
                    player.getX(), player.getY(), player.getZ(), xpToKeep);
            serverLevel.addFreshEntity(orb);
        }
    }
}