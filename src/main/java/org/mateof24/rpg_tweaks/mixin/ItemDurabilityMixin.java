package org.mateof24.rpg_tweaks.mixin;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public class ItemDurabilityMixin {

    @ModifyVariable(
            method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private int modifyDurabilityDamage(int amount) {
        float multiplier = ModConfig.getInstance().durabilityMultiplier;
        if (multiplier == -1f) return 0;
        if (multiplier == 0f) return amount;
        return Math.max(0, Math.round(amount * multiplier));
    }
}