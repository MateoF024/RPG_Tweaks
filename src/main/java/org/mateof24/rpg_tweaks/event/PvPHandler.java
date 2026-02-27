package org.mateof24.rpg_tweaks.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.mateof24.rpg_tweaks.config.ModConfig;

@EventBusSubscriber(modid = org.mateof24.rpg_tweaks.RPG_Tweaks.MODID)
public class PvPHandler {

    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (ModConfig.getInstance().pvpEnabled) return;
        if (!(event.getEntity() instanceof ServerPlayer)) return;

        if (event.getSource().getEntity() instanceof Player) {
            event.setCanceled(true);
        }
    }
}