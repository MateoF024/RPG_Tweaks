package org.mateof24.rpg_tweaks.event;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.mateof24.rpg_tweaks.config.OreXPConfig;
import org.slf4j.Logger;

import java.util.*;

@EventBusSubscriber(modid = org.mateof24.rpg_tweaks.RPG_Tweaks.MODID)
public class OreXPHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        ModConfig config = ModConfig.getInstance();

        if (!config.enableCustomOreXP) {
            return;
        }

        Level level = (Level) event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (player.gameMode.getGameModeForPlayer() != net.minecraft.world.level.GameType.SURVIVAL) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BlockState blockState = event.getState();
        Block block = blockState.getBlock();
        BlockPos pos = event.getPos();

        String blockId = block.builtInRegistryHolder().key().location().toString();
        List<String> blockTags = getBlockTags(block);

        OreXPConfig.OreXPValues xpValues = config.oreXPConfig.getConfigForBlock(blockId, blockTags);

        if (xpValues == null) {
            return;
        }

        ItemStack tool = player.getMainHandItem();

        if (hasSilkTouch(tool, level)) {
            if (config.logOreXP) {
                LOGGER.debug("XP blocked by Silk Touch: {}", blockId);
            }
            return;
        }

        if (!blockState.canHarvestBlock(level, pos, player)) {
            if (config.logOreXP) {
                LOGGER.debug("Incorrect tool for {}", blockId);
            }
            return;
        }

        if (!xpValues.shouldDropXP()) {
            if (config.logOreXP) {
                LOGGER.debug("XP set to 0 for: {}", blockId);
            }
            return;
        }

        int customXP = xpValues.getRandomXP();

        if (customXP > 0) {
            serverLevel.getServer().execute(() -> {
                if (serverLevel.getBlockState(pos).isAir()) {
                    spawnXPOrb(serverLevel, pos, customXP);

                    if (config.logOreXP) {
                        LOGGER.info("XP spawned: {} points for {}", customXP, blockId);
                    }
                }
            });
        }
    }

    private static void spawnXPOrb(ServerLevel level, BlockPos pos, int xpValue) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        ExperienceOrb orb = new ExperienceOrb(level, x, y, z, xpValue);
        level.addFreshEntity(orb);
    }

    private static List<String> getBlockTags(Block block) {
        List<String> tags = new ArrayList<>();
        block.builtInRegistryHolder().tags().forEach(tagKey -> {
            tags.add(tagKey.location().toString());
        });
        return tags;
    }

    private static boolean hasSilkTouch(ItemStack tool, Level level) {
        try {
            return tool.getEnchantmentLevel(
                    level.holderLookup(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.SILK_TOUCH)
            ) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isBlockConfigured(Block block) {
        try {
            ModConfig config = ModConfig.getInstance();
            if (!config.enableCustomOreXP) return false;

            String blockId = block.builtInRegistryHolder().key().location().toString();
            List<String> blockTags = getBlockTags(block);

            return config.oreXPConfig.getConfigForBlock(blockId, blockTags) != null;
        } catch (Exception e) {
            return false;
        }
    }
}