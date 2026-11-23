package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.p1nero.cobblemon.rumblecore.combat.reward.WildCapture;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EmptyPokeBallEntity.class, remap = false)
abstract class StunnedCaptureWindow {
    @Shadow private void drop() { throw new AssertionError(); }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void rumble$respectStunWindow(EntityHitResult hit, CallbackInfo ci) {
        EmptyPokeBallEntity self = (EmptyPokeBallEntity) (Object) this;
        if (self.level().isClientSide() || !(hit.getEntity() instanceof PokemonEntity target)
                || self.getCaptureState() != EmptyPokeBallEntity.CaptureState.NOT
                || (ArenaMatch.isLegendaryFoe(target) ? ArenaMatch.canThrowAtLegendary(target, self.getOwner())
                    : !WildCapture.stunned(target) || WildCapture.canThrowAt(target))) return;
        drop();
        ci.cancel();
    }

    @Inject(method = "attemptCatch", at = @At("RETURN"))
    private void rumble$pauseCountdown(PokemonEntity target, CallbackInfo ci) {
        var ball = (EmptyPokeBallEntity) (Object) this;
        if (!WildCapture.stunned(target) || !target.getBusyLocks().contains(ball)) return;
        WildCapture.attemptStarted(target);
        if (ArenaMatch.isLegendaryFoe(target)) ball.getCaptureFuture().thenAccept(success -> {
            // Cobblemon completes this future just before adding the Pokémon to party/PC.
            // The entity multiplier is transient; only species HP can be stored on the Pokémon.
            if (success) {
                var mon = target.getPokemon();
                mon.setCurrentHealth(Math.max(1, Math.round(mon.getMaxHealth() * .01f)));
            }
        });
    }

    @Inject(method = "breakFree", at = @At("RETURN"))
    private void rumble$refreshCountdown(CallbackInfo ci) {
        var ball = (EmptyPokeBallEntity) (Object) this;
        var target = ball.getCapturingPokemon();
        if (target != null && target.getBusyLocks().contains(ball)) WildCapture.attemptFailed(target);
    }
}
