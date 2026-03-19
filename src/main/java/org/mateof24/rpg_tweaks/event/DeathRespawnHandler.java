package org.mateof24.rpg_tweaks.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.mateof24.rpg_tweaks.RPG_Tweaks;
import org.mateof24.rpg_tweaks.config.ModConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = RPG_Tweaks.MODID)
public class DeathRespawnHandler {

    private static final Map<UUID, BlockPos> safePositions = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModConfig.getInstance().deathRespawnEnabled) return;
        if (!player.level().dimension().equals(Level.OVERWORLD)) return;
        if (player.getRespawnPosition() != null) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        ModConfig config = ModConfig.getInstance();
        RandomSource random = serverLevel.getRandom();
        double angle = random.nextDouble() * 2 * Math.PI;
        double dist = config.deathRespawnMinDistance
                + random.nextDouble() * (config.deathRespawnMaxDistance - config.deathRespawnMinDistance);
        int tx = (int) (player.getX() + Math.cos(angle) * dist);
        int tz = (int) (player.getZ() + Math.sin(angle) * dist);

        if (!serverLevel.hasChunk(tx >> 4, tz >> 4)) return;

        BlockPos safePos = findSafePosition(serverLevel, tx, tz);
        if (safePos != null) {
            safePositions.put(player.getUUID(), safePos);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlockPos target = safePositions.remove(player.getUUID());
        if (target == null) return;
        player.connection.teleport(
                target.getX() + 0.5,
                target.getY(),
                target.getZ() + 0.5,
                player.getYRot(),
                player.getXRot());
    }

    private static BlockPos findSafePosition(ServerLevel level, int x, int z) {
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        if (surfaceY <= level.getMinBuildHeight()) return null;

        BlockPos feet = new BlockPos(x, surfaceY, z);
        if (!level.getBlockState(feet).isAir()) return null;
        if (!level.getBlockState(feet.above()).isAir()) return null;
        if (level.getBlockState(feet.below()).liquid()) return null;
        if (!level.getBlockState(feet.below()).isSolid()) return null;

        return feet;
    }

    public static void clearPlayer(UUID uuid) {
        safePositions.remove(uuid);
    }
}