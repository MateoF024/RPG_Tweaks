package org.mateof24.rpg_tweaks.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.food.FoodData;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.util.Unit;

@Mixin(Player.class)
public class SleepMixin {

    @Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
    private void checkSleepAllowed(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        int requiredNight = ModConfig.getInstance().sleepFromNight;
        if (requiredNight <= 0) return;

        Player player = (Player)(Object)this;
        if (player.level().isClientSide()) return;

        long currentDay = player.level().getDayTime() / 24000L;

        if (currentDay < requiredNight) {
            cir.setReturnValue(Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_HERE));
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(
                        Component.translatable("rpg_tweaks.sleep.blocked", requiredNight),
                        true
                );
            }
        }
    }

    @Inject(method = "stopSleepInBed", at = @At("TAIL"))
    private void onWakeUp(boolean wakeImmediately, boolean updateLevel, CallbackInfo ci) {
        Player player = (Player)(Object)this;
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer)) return;

        float healPercent = ModConfig.getInstance().sleepHealPercent;
        if (healPercent > 0f) {
            player.heal(player.getMaxHealth() * (healPercent / 100f));
        }

        int points = ModConfig.getInstance().sleepHungerPoints;
        float chance = ModConfig.getInstance().sleepHungerChance;
        if (points > 0 && chance > 0f && player.level().random.nextFloat() * 100f < chance) {
            FoodData food = player.getFoodData();
            food.setFoodLevel(Math.max(0, food.getFoodLevel() - points));
        }
    }
}