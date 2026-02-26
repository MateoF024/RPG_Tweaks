package org.mateof24.rpg_tweaks.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;


@EventBusSubscriber(modid = org.mateof24.rpg_tweaks.RPG_Tweaks.MODID)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AdvancementXPHandler.clearPlayerSnapshot(player);
        }
    }
}