package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmptyPokeBallEntity.class, remap = false)
abstract class ThrownBallCaptureGate {
    @Shadow protected abstract void drop();

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void rumble$useKnockoutCapture(EntityHitResult hit, CallbackInfo ci) {
        EmptyPokeBallEntity self = (EmptyPokeBallEntity) (Object) this;
        if (!(self.getOwner() instanceof ServerPlayer player) || !(hit.getEntity() instanceof PokemonEntity)
                || self.getCaptureState() != EmptyPokeBallEntity.CaptureState.NOT) return;
        player.displayClientMessage(Component.translatable("rumble.combat.ball_disabled"), true);
        drop();
        ci.cancel();
    }
}
