package org.mateof24.rpg_tweaks.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.mateof24.rpg_tweaks.util.FoodTickTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class PlayerNaturalRegenMixin {

    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float modifyNaturalRegen(float amount) {
        if (!((Object) this instanceof Player)) return amount;
        if (!FoodTickTracker.INSIDE_FOOD_TICK.get()) return amount;
        return amount * ModConfig.getInstance().naturalRegenRateMultiplier;
    }
}