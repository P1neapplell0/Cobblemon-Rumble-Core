package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.p1nero.cobblemon.rumblecore.combat.arena.CinemaHealth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Pokemon.class, remap = false)
abstract class CinemaPokemonHealth implements CinemaHealth {
    @Unique private double rumble$cinemaHealthScale = 1;

    @Override public void rumble$cinemaHealthScale(double scale) { rumble$cinemaHealthScale = scale; }

    @Inject(method = "getMaxHealth", at = @At("RETURN"), cancellable = true)
    private void rumble$scaleMovieHealth(CallbackInfoReturnable<Integer> cir) {
        if (rumble$cinemaHealthScale > 1)
            cir.setReturnValue((int) Math.round(cir.getReturnValueI() * rumble$cinemaHealthScale));
    }
}
