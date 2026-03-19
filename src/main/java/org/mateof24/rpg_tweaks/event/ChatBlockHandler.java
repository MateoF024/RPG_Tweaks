package org.mateof24.rpg_tweaks.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import org.mateof24.rpg_tweaks.RPG_Tweaks;
import org.mateof24.rpg_tweaks.config.ModConfig;

import java.util.Locale;

@EventBusSubscriber(modid = RPG_Tweaks.MODID)
public class ChatBlockHandler {

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        ModConfig config = ModConfig.getInstance();
        if (!config.chatEnabled) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.translatable("rpg_tweaks.chat.disabled_global"));
            return;
        }
        String name = player.getName().getString().toLowerCase(Locale.ROOT);
        if (config.chatBlockedPlayers.contains(name)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.translatable("rpg_tweaks.chat.disabled_player"));
        }
    }
}