package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.Smash;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.misc.custom_instances.api.InstanceApi;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.arena.RealtimeProtection;
import com.p1nero.cobblemon.rumblecore.prologue.FixedStoryActors;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Smash.class, remap = false)
abstract class InstanceDirectDamage {
    @Inject(method = "hurtPokemon", at = @At("HEAD"), cancellable = true)
    private static void rumble$protectStoryActors(Smash.Session battle, float amount, DamageSource source, CallbackInfo ci) {
        if (FixedStoryActors.protectedFromCombat(battle.pokemon) || RealtimeProtection.megaProtected(battle.pokemon)) ci.cancel();
    }

    @WrapOperation(method = "hurtPokemon", at = @At(value = "INVOKE",
            target = "Lcom/cobblemon/mod/common/pokemon/Pokemon;setCurrentHealth(I)V"))
    private static void rumble$recordDirectDamage(Pokemon mon, int health, Operation<Void> original,
            Smash.Session battle, float amount, DamageSource source) {
        int previous = mon.getCurrentHealth();
        original.call(mon, health);
        int lost = previous - mon.getCurrentHealth();
        ArenaMatch.cinemaPlayerHit(battle.pokemon, source, lost);
        InstanceApi.recordCustomDamage(battle.pokemon, source, lost);
        RealtimeProtection.hit(battle.pokemon, source, lost);
    }
}
