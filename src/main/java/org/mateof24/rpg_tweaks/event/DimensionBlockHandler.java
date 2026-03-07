package org.mateof24.rpg_tweaks.event;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.mateof24.rpg_tweaks.data.PlayerDimensionData;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = org.mateof24.rpg_tweaks.RPG_Tweaks.MODID)
public class DimensionBlockHandler {

    private static final Map<UUID, double[]> lastSafePosition = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onEntityTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String dest = event.getDimension().location().toString();
        if (isBlocked(player, dest)) {
            event.setCanceled(true);
            sendBlockedMessage(player, dest);
        } else {
            lastSafePosition.put(player.getUUID(), new double[]{
                    player.getX(), player.getY(), player.getZ()
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String dest = event.getTo().location().toString();
        if (!isBlocked(player, dest)) return;
        ResourceKey<Level> fromKey = event.getFrom();
        ServerLevel fromLevel = player.getServer().getLevel(fromKey);
        if (fromLevel != null) {
            double[] pos = lastSafePosition.getOrDefault(player.getUUID(), new double[]{
                    fromLevel.getSharedSpawnPos().getX(),
                    fromLevel.getSharedSpawnPos().getY(),
                    fromLevel.getSharedSpawnPos().getZ()
            });
            player.teleportTo(fromLevel, pos[0], pos[1], pos[2], player.getYRot(), player.getXRot());
        }
        sendBlockedMessage(player, dest);
    }

    public static boolean isBlocked(ServerPlayer player, String dimension) {
        if (PlayerDimensionData.isAllowed(player.getUUID(), dimension)) return false;
        if (PlayerDimensionData.isBlocked(player.getUUID(), dimension)) return true;
        return ModConfig.getInstance().blockedDimensions.containsKey(dimension);
    }

    private static void sendBlockedMessage(ServerPlayer player, String dimension) {
        String perPlayerMsg = PlayerDimensionData.getBlockedMessage(player.getUUID(), dimension);
        if (perPlayerMsg != null && !perPlayerMsg.isBlank()) {
            player.displayClientMessage(Component.literal(parseColors(perPlayerMsg)), true);
            return;
        }
        String global = ModConfig.getInstance().blockedDimensions.get(dimension);
        if (global != null && !global.isBlank()) {
            player.displayClientMessage(Component.literal(parseColors(global)), true);
        } else {
            player.displayClientMessage(Component.translatable("rpg_tweaks.dimension.blocked", dimension), true);
        }
    }

    private static String parseColors(String text) {
        return text.replace("&", "§");
    }

    public static void clearPlayer(ServerPlayer player) {
        lastSafePosition.remove(player.getUUID());
    }
}