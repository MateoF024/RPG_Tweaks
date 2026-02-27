package org.mateof24.rpg_tweaks.mixin;

import net.minecraft.world.entity.player.Player;
import org.mateof24.rpg_tweaks.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public class PlayerXPCapMixin {

    @Shadow public int experienceLevel;
    @Shadow public float experienceProgress;

    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int capXPAmount(int amount) {
        int cap = ModConfig.getInstance().maxStorableXP;
        if (cap <= 0) return amount;
        int currentXP = calculateActualXP(experienceLevel, experienceProgress);
        if (currentXP >= cap) return 0;
        return Math.min(amount, cap - currentXP);
    }

    private static int calculateActualXP(int level, float progress) {
        int totalAtLevel;
        if (level <= 16) {
            totalAtLevel = level * level + 6 * level;
        } else if (level <= 31) {
            totalAtLevel = (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            totalAtLevel = (int) (4.5 * level * level - 162.5 * level + 2220);
        }
        int xpForNext;
        if (level <= 15) {
            xpForNext = 2 * level + 7;
        } else if (level <= 30) {
            xpForNext = 5 * level - 38;
        } else {
            xpForNext = 9 * level - 158;
        }
        return totalAtLevel + Math.round(progress * xpForNext);
    }
}