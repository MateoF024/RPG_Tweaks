package org.mateof24.rpg_tweaks.mixin;

import net.minecraft.world.food.FoodData;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.mateof24.rpg_tweaks.util.FoodTickTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public class FoodDataMixin {

    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float modifyExhaustion(float exhaustion) {
        return exhaustion * ModConfig.getInstance().exhaustionRateMultiplier;
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void onFoodTickStart(CallbackInfo ci) {
        FoodTickTracker.INSIDE_FOOD_TICK.set(true);
    }

    @Inject(method = "tick", at = @At("RETURN"), require = 0)
    private void onFoodTickEnd(CallbackInfo ci) {
        FoodTickTracker.INSIDE_FOOD_TICK.set(false);
    }
}