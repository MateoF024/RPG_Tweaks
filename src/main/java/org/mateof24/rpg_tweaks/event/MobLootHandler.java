package org.mateof24.rpg_tweaks.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import org.mateof24.rpg_tweaks.RPG_Tweaks;
import org.mateof24.rpg_tweaks.config.MobLootConfig;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.mateof24.rpg_tweaks.item.ModItems;

import java.util.Iterator;

@EventBusSubscriber(modid = RPG_Tweaks.MODID)
public class MobLootHandler {

    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        if (!ModConfig.getInstance().enableMobXP) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!(event.getAttackingPlayer() instanceof ServerPlayer)) return;
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityId == null) return;
        if (ModConfig.getInstance().mobLootConfig.mobDrops.containsKey(entityId.toString())) {
            event.setDroppedExperience(0);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityId == null) return;

        MobLootConfig.MobSackDrops dropConfig = ModConfig.getInstance().mobLootConfig.mobDrops.get(entityId.toString());
        if (dropConfig == null) return;

        Iterator<ItemEntity> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemEntity itemEntity = iterator.next();
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem());
            if (dropConfig.removedDrops.contains(itemId.toString())) iterator.remove();
        }

        int lootingLevel = 0;
        try {
            lootingLevel = player.getMainHandItem().getEnchantmentLevel(
                    entity.level().holderLookup(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.LOOTING));
        } catch (Exception ignored) {}

        ModConfig config = ModConfig.getInstance();
        float sackBonus = lootingLevel * config.lootSackLootingBonus;

        ItemStack winner = rollTier(entity, player, dropConfig.legendary, sackBonus, ModItems.LOOT_SACK_LEGENDARY.get());
        if (winner == null) winner = rollTier(entity, player, dropConfig.epic, sackBonus, ModItems.LOOT_SACK_EPIC.get());
        if (winner == null) winner = rollTier(entity, player, dropConfig.rare, sackBonus, ModItems.LOOT_SACK_RARE.get());
        if (winner == null) winner = rollTier(entity, player, dropConfig.uncommon, sackBonus, ModItems.LOOT_SACK_UNCOMMON.get());
        if (winner == null) winner = rollTier(entity, player, dropConfig.common, sackBonus, ModItems.LOOT_SACK_COMMON.get());

        if (winner != null) {
            boolean isHighTier = winner.getItem() == ModItems.LOOT_SACK_EPIC.get()
                    || winner.getItem() == ModItems.LOOT_SACK_LEGENDARY.get();
            ItemEntity drop = new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.5, entity.getZ(), winner);
            drop.setPickUpDelay(10);
            event.getDrops().add(drop);
            if (isHighTier) {
                entity.level().playSound(null, entity.blockPosition(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5f, 1.0f);
            }
        }

        if (config.enableMobXP && dropConfig.xpOnKill != null && dropConfig.xpOnKill.shouldDropXP()) {
            if (entity.level() instanceof ServerLevel serverLevel
                    && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL) {
                int baseXP = dropConfig.xpOnKill.getRandomXP();
                int finalXP = (lootingLevel > 0 && config.mobXPLootingBonus > 0f)
                        ? Math.round(baseXP * (1f + lootingLevel * config.mobXPLootingBonus))
                        : baseXP;
                if (finalXP > 0) {
                    serverLevel.addFreshEntity(new ExperienceOrb(serverLevel,
                            entity.getX(), entity.getY() + 0.5, entity.getZ(), finalXP));
                }
            }
        }
    }

    private static boolean meetsConditions(LivingEntity entity, ServerPlayer player, MobLootConfig.SackTierConfig tier) {
        if (!tier.requiredDimension.isEmpty()) {
            String dim = entity.level().dimension().location().toString();
            if (!dim.equals(tier.requiredDimension)) return false;
        }
        if (!tier.requiredBiome.isEmpty()) {
            String biome = entity.level().getBiome(entity.blockPosition())
                    .unwrapKey().map(k -> k.location().toString()).orElse("");
            if (!biome.equals(tier.requiredBiome)) return false;
        }
        if (tier.onlyAtNight && entity.level().isDay()) return false;
        if (!tier.moonPhases.isEmpty() && entity.level() instanceof ServerLevel sl) {
            int phase = (int) ((sl.getDayTime() / 24000L) % 8);
            if (!tier.moonPhases.contains(phase)) return false;
        }
        if (tier.minDistanceFromPlayer > 0f) {
            double dist = player.distanceTo(entity);
            if (dist < tier.minDistanceFromPlayer) return false;
        }
        if ((tier.onlySurface || tier.onlyCave) && entity.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            int surfaceY = entity.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    entity.getBlockX(), entity.getBlockZ());
            boolean isOnSurface = entity.getBlockY() >= surfaceY - 1;
            if (tier.onlySurface && !isOnSurface) return false;
            if (tier.onlyCave && isOnSurface) return false;
        }
        return true;
    }

    private static ItemStack rollTier(LivingEntity entity, ServerPlayer player,
                                      MobLootConfig.SackTierConfig tier,
                                      float bonus, net.minecraft.world.item.Item item) {
        if (tier.chance <= 0f) return null;
        if (!meetsConditions(entity, player, tier)) return null;
        float effective = Math.min(100f, tier.chance + bonus);
        return entity.level().random.nextFloat() * 100f < effective ? new ItemStack(item) : null;
    }
}