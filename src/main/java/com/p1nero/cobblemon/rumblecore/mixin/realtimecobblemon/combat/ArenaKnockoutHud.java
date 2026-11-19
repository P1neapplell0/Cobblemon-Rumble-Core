package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaKnockoutVisual;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.johnn.realtimecobblemon.client.SmashHud", remap = false)
abstract class ArenaKnockoutHud {
    @Redirect(method = "targetTile", at = @At(value = "INVOKE",
            target = "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;getHealth()F", remap = false))
    private static float rumble$showKnockedOut(PokemonEntity pokemon) {
        return ((ArenaKnockoutVisual) pokemon).rumble$isKnockedOut() ? 0 : pokemon.getHealth();
    }
}
