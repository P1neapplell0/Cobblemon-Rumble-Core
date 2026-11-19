package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonServerDelegate;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cobblemon can roll its species loot after the fainting animation, outside vanilla's drop capture. */
@Mixin(PokemonEntity.class)
abstract class TrainingPikachuLoot {
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void rumble$noTrainingDeathLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
        if (PrologueTraining.target((PokemonEntity) (Object) this)) ci.cancel();
    }

    @Mixin(value = PokemonServerDelegate.class, remap = false)
    static abstract class SpeciesLoot {
        @Shadow public abstract PokemonEntity getEntity();

        @Inject(method = "doDeathDrops", at = @At("HEAD"), cancellable = true)
        private void rumble$noTrainingSpeciesLoot(CallbackInfo ci) {
            if (PrologueTraining.target(getEntity())) ci.cancel();
        }
    }
}
