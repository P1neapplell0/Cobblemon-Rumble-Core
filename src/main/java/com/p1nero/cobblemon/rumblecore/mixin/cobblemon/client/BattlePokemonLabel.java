package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.client;

import com.cobblemon.mod.common.client.render.pokemon.PokemonRenderer;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.p1nero.cobblemon.rumblecore.client.combat.BattleNameplates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PokemonRenderer.class)
abstract class BattlePokemonLabel {
    // Cobblemon's own name/level draw bypasses NeoForge's RenderNameTagEvent.
    @Inject(method = "shouldRenderLabel", at = @At("HEAD"), cancellable = true, remap = false)
    private void rumble$hideOverheadLabel(PokemonEntity pokemon, CallbackInfoReturnable<Boolean> ci) {
        if (BattleNameplates.hidden(pokemon)) ci.setReturnValue(false);
    }
}
