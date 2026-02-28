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
import org.mateof24.rpg_tweaks.item.ModItems;

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
            if (dropConfig.removedDrops.contains(itemId.toString())) {
                iterator.remove();
            }
        }

        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        rollAndAdd(event, entity, dropConfig.commonChance,    new ItemStack(ModItems.LOOT_SACK_COMMON.get()),    x, y, z, false);
        rollAndAdd(event, entity, dropConfig.uncommonChance,  new ItemStack(ModItems.LOOT_SACK_UNCOMMON.get()),  x, y, z, false);
        rollAndAdd(event, entity, dropConfig.rareChance,      new ItemStack(ModItems.LOOT_SACK_RARE.get()),      x, y, z, false);
        rollAndAdd(event, entity, dropConfig.epicChance,      new ItemStack(ModItems.LOOT_SACK_EPIC.get()),      x, y, z, true);
        rollAndAdd(event, entity, dropConfig.legendaryChance, new ItemStack(ModItems.LOOT_SACK_LEGENDARY.get()), x, y, z, true);
    }

    private static void rollAndAdd(LivingDropsEvent event, LivingEntity entity,
                                   float chance, ItemStack stack, double x, double y, double z,
                                   boolean challengeSound) {
        if (chance <= 0f) return;
        if (entity.level().random.nextFloat() * 100f < chance) {
            ItemEntity itemEntity = new ItemEntity(entity.level(), x, y + 0.5, z, stack);
            itemEntity.setPickUpDelay(10);
            event.getDrops().add(itemEntity);
            if (challengeSound) {
                entity.level().playSound(null, entity.blockPosition(),
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5f, 1.0f);
            }
        }
    }
}