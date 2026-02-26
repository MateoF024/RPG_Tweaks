package org.mateof24.rpg_tweaks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.mateof24.rpg_tweaks.event.OreXPHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Block.class)
public class BlockBreakXPMixin {

    @Inject(
            method = "popExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cancelVanillaXP(ServerLevel level, BlockPos pos, int amount, CallbackInfo ci) {
        Block thisBlock = (Block)(Object)this;
        if (OreXPHandler.isBlockConfigured(thisBlock)) {
            ci.cancel();
        }
    }
}