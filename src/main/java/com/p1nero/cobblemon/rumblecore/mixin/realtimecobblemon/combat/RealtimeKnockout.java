package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Experience;
import com.p1nero.cobblemon.rumblecore.combat.reward.KnockoutRewards;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Experience.class, remap = false)
abstract class RealtimeKnockout {
    // This hook carries the authoritative attacker even when a faint is not a vanilla LivingDeathEvent.
    @Inject(method = "knockedOut", at = @At("HEAD"))
    private static void rumble$awardKnockout(Smash.Session session, PokemonEntity defeated, CallbackInfo ci) {
        if (PrologueTraining.target(defeated)) PrologueTraining.knockout(session, defeated);
        else if (com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch.group(session.player) == null) KnockoutRewards.settle(session, defeated);
    }
}
