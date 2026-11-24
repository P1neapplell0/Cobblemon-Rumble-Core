package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaPokemonHealth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Pokemon.class, remap = false)
abstract class ArenaPokemonHealthMixin implements ArenaPokemonHealth {
    @Unique private double rumble$arenaHealthScale = 1;

    @Override public void rumble$setArenaHealthScale(double scale) { rumble$arenaHealthScale = scale; }

    @Inject(method = "getMaxHealth", at = @At("RETURN"), cancellable = true)
    private void rumble$scaleArenaHealth(CallbackInfoReturnable<Integer> cir) {
        if (rumble$arenaHealthScale > 1)
            cir.setReturnValue((int) Math.round(cir.getReturnValueI() * rumble$arenaHealthScale));
    }
}
