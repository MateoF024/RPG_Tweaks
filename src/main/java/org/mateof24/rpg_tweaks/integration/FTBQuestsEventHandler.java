package org.mateof24.rpg_tweaks.integration;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import org.mateof24.rpg_tweaks.RPG_Tweaks;

@EventBusSubscriber(modid = RPG_Tweaks.MODID)
public class FTBQuestsEventHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (FTBQuestsManager.isInstalled() && event.getEntity() instanceof ServerPlayer) {
            FTBQuestsManager.init();
        }
    }
}