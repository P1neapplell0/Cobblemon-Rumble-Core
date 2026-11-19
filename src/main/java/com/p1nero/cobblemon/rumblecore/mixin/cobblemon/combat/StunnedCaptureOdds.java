package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.p1nero.cobblemon.rumblecore.combat.reward.WildCapture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Give Cobblemon's HP ratio terms exactly 1/100 without modifying its rolls or the Pokémon's stats. */
@Mixin(targets = {
        "com.cobblemon.mod.common.pokeball.catching.calculators.CobblemonCaptureCalculator",
        "com.cobblemon.mod.common.pokeball.catching.calculators.Gen1CaptureCalculator",
        "com.cobblemon.mod.common.pokeball.catching.calculators.Gen2CaptureCalculator",
        "com.cobblemon.mod.common.pokeball.catching.calculators.Gen3And4CaptureCalculator",
        "com.cobblemon.mod.common.pokeball.catching.calculators.Gen5CaptureCalculator",
        "com.cobblemon.mod.common.pokeball.catching.calculators.Gen6CaptureCalculator",
        "com.cobblemon.mod.common.pokeball.catching.calculators.Gen7CaptureCalculator",
        "com.cobblemon.mod.common.pokeball.catching.calculators.Gen8CaptureCalculator",
        "com.cobblemon.mod.common.pokeball.catching.calculators.Gen9CaptureCalculator"
}, remap = false)
abstract class StunnedCaptureOdds {
    @ModifyExpressionValue(method = "processCapture", at = @At(value = "INVOKE",
            target = "Lcom/cobblemon/mod/common/pokemon/Pokemon;getCurrentHealth()I"))
    private int rumble$virtualHp(int original, LivingEntity thrower, EmptyPokeBallEntity ball, PokemonEntity target) {
        return WildCapture.stunned(target) ? 1 : original;
    }

    @ModifyExpressionValue(method = "processCapture", at = @At(value = "INVOKE",
            target = "Lcom/cobblemon/mod/common/pokemon/Pokemon;getMaxHealth()I"))
    private int rumble$virtualMaxHp(int original, LivingEntity thrower, EmptyPokeBallEntity ball, PokemonEntity target) {
        return WildCapture.stunned(target) ? 100 : original;
    }
}
