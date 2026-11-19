package com.p1nero.cobblemon.rumblecore.mixin.rct;

import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.p1nero.cobblemon.rumblecore.integration.rct.RoadTrainerArena;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RCTMod.class, remap = false)
abstract class RoadTrainerBattle {
    @Inject(method = "makeBattle", at = @At("HEAD"), cancellable = true)
    private void rumble$arena(TrainerMob mob, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer serverPlayer) cir.setReturnValue(RoadTrainerArena.challenge(serverPlayer, mob));
    }
}
