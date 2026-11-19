package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.p1nero.cobblemon.rumblecore.combat.reward.WildCapture;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmptyPokeBallEntity.class, remap = false)
abstract class StunnedCaptureWindow {
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "beginCapture", at = @At(value = "INVOKE",
            target = "Lcom/cobblemon/mod/common/api/pokeball/catching/calculators/CaptureCalculator;processCapture(Lnet/minecraft/world/entity/LivingEntity;Lcom/cobblemon/mod/common/entity/pokeball/EmptyPokeBallEntity;Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)Lcom/cobblemon/mod/common/api/pokeball/catching/CaptureContext;"))
    private com.cobblemon.mod.common.api.pokeball.catching.CaptureContext rumble$ankletCapture(
            com.cobblemon.mod.common.api.pokeball.catching.calculators.CaptureCalculator calculator,
            net.minecraft.world.entity.LivingEntity thrower, EmptyPokeBallEntity ball, PokemonEntity target,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<com.cobblemon.mod.common.api.pokeball.catching.CaptureContext> original) {
        var result = original.call(calculator, thrower, ball, target);
        return com.p1nero.cobblemon.rumblecore.prologue.MayAnklet.guaranteesCapture(target)
                ? new com.cobblemon.mod.common.api.pokeball.catching.CaptureContext(4, true, false) : result;
    }

    @Shadow private void drop() { throw new AssertionError(); }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void rumble$respectStunWindow(EntityHitResult hit, CallbackInfo ci) {
        EmptyPokeBallEntity self = (EmptyPokeBallEntity) (Object) this;
        if (self.level().isClientSide() || !(hit.getEntity() instanceof PokemonEntity target)
                || self.getCaptureState() != EmptyPokeBallEntity.CaptureState.NOT
                || !WildCapture.stunned(target) || WildCapture.canThrowAt(target)) return;
        drop();
        ci.cancel();
    }

    @Inject(method = "attemptCatch", at = @At("RETURN"))
    private void rumble$pauseCountdown(PokemonEntity target, CallbackInfo ci) {
        if (WildCapture.stunned(target)) WildCapture.attemptStarted(target);
    }

    @Inject(method = "breakFree", at = @At("RETURN"))
    private void rumble$refreshCountdown(CallbackInfo ci) {
        var ball = (EmptyPokeBallEntity) (Object) this;
        var target = ball.getCapturingPokemon();
        if (target != null && target.getBusyLocks().contains(ball)) WildCapture.attemptFailed(target);
    }
}
