package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaKnockoutVisual;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PokemonEntity.class)
abstract class ArenaKnockoutState implements ArenaKnockoutVisual {
    @Unique private static final EntityDataAccessor<Boolean> RUMBLE_KO =
            SynchedEntityData.defineId(PokemonEntity.class, EntityDataSerializers.BOOLEAN);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void rumble$defineKnockout(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(RUMBLE_KO, false);
    }

    @Override public void rumble$markKnockedOut() {
        ((PokemonEntity) (Object) this).getEntityData().set(RUMBLE_KO, true);
    }

    @Override public boolean rumble$isKnockedOut() {
        return ((PokemonEntity) (Object) this).getEntityData().get(RUMBLE_KO);
    }
}
