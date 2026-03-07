package org.mateof24.rpg_tweaks.event;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.mateof24.rpg_tweaks.RPG_Tweaks;
import org.mateof24.rpg_tweaks.config.MobLootConfig;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.mateof24.rpg_tweaks.item.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;


import java.util.Iterator;

@EventBusSubscriber(modid = RPG_Tweaks.MODID)
public class MobLootHandler {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
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
        if (event.getSource().getEntity() instanceof LivingEntity killer) {
            try {
                lootingLevel = killer.getMainHandItem().getEnchantmentLevel(
                        entity.level().holderLookup(net.minecraft.core.registries.Registries.ENCHANTMENT)
                                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING));
            } catch (Exception ignored) {}
        }

        float bonus = lootingLevel * ModConfig.getInstance().lootSackLootingBonus;

        ItemStack winner = rollTier(entity, dropConfig.legendaryChance, bonus, ModItems.LOOT_SACK_LEGENDARY.get());
        if (winner == null) winner = rollTier(entity, dropConfig.epicChance, bonus, ModItems.LOOT_SACK_EPIC.get());
        if (winner == null) winner = rollTier(entity, dropConfig.rareChance, bonus, ModItems.LOOT_SACK_RARE.get());
        if (winner == null) winner = rollTier(entity, dropConfig.uncommonChance, bonus, ModItems.LOOT_SACK_UNCOMMON.get());
        if (winner == null) winner = rollTier(entity, dropConfig.commonChance, bonus, ModItems.LOOT_SACK_COMMON.get());
        if (winner == null) return;

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

    private static ItemStack rollTier(LivingEntity entity, float chance, float bonus, net.minecraft.world.item.Item item) {
        if (chance <= 0f) return null;
        float effective = Math.min(100f, chance + bonus);
        return entity.level().random.nextFloat() * 100f < effective ? new ItemStack(item) : null;
    }
}