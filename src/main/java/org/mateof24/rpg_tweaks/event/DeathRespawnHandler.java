package org.mateof24.rpg_tweaks.event;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.mateof24.rpg_tweaks.RPG_Tweaks;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = RPG_Tweaks.MODID)
public class DeathRespawnHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, double[]> deathPositions = new ConcurrentHashMap<>();
    private static final Map<UUID, ServerLevel> deathLevels = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!ModConfig.getInstance().deathRespawnEnabled) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        deathPositions.put(player.getUUID(), new double[]{player.getX(), player.getY(), player.getZ()});
        deathLevels.put(player.getUUID(), (ServerLevel) player.level());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!ModConfig.getInstance().deathRespawnEnabled) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        double[] deathPos = deathPositions.remove(uuid);
        ServerLevel deathLevel = deathLevels.remove(uuid);

        if (deathPos == null || deathLevel == null) return;
        if ((ServerLevel) player.level() != deathLevel) return;

        int minDist = ModConfig.getInstance().deathRespawnMinDistance;
        int maxDist = ModConfig.getInstance().deathRespawnMaxDistance;

        BlockPos target = findSafePosition(deathLevel, deathPos[0], deathPos[2], minDist, maxDist);
        if (target == null) return;

        final double tx = target.getX() + 0.5;
        final double ty = target.getY();
        final double tz = target.getZ() + 0.5;

        player.connection.teleport(tx, ty, tz, player.getYRot(), player.getXRot());
        LOGGER.debug("[DeathRespawn] Teleported {} to ({}, {}, {})", player.getName().getString(), tx, ty, tz);
    }

    private static BlockPos findSafePosition(ServerLevel level, double originX, double originZ, int minDist, int maxDist) {
        int minY = level.dimensionType().minY();
        int height = level.dimensionType().height();
        int maxY = minY + height;
        java.util.Random rand = new java.util.Random();

        for (int attempt = 0; attempt < 50; attempt++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double dist = minDist + rand.nextDouble() * (maxDist - minDist);
            int x = (int) (originX + Math.cos(angle) * dist);
            int z = (int) (originZ + Math.sin(angle) * dist);

            for (int y = maxY - 2; y > minY; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!level.canSeeSky(pos)) continue;
                BlockPos below = pos.below();
                BlockState groundState = level.getBlockState(below);
                BlockState feetState = level.getBlockState(pos);
                BlockState headState = level.getBlockState(pos.above());

                if (groundState.isSolid()
                        && !groundState.liquid()
                        && feetState.isAir()
                        && headState.isAir()) {
                    return pos;
                }
            }
        }
        return null;
    }

    public static void clearPlayer(UUID uuid) {
        deathPositions.remove(uuid);
        deathLevels.remove(uuid);
    }
}