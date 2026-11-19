package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.p1nero.cobblemon.rumblecore.combat.reward.WildCapture;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PokemonEntity.class, remap = false)
abstract class WildCaptureState implements WildCapture.Synced {
    @Unique private static final EntityDataAccessor<Integer> RUMBLE_CAPTURE_SECONDS =
            SynchedEntityData.defineId(PokemonEntity.class, EntityDataSerializers.INT);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void rumble$defineCountdown(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(RUMBLE_CAPTURE_SECONDS, WildCapture.INACTIVE);
    }

    @Override public int rumble$captureSeconds() {
        return ((PokemonEntity) (Object) this).getEntityData().get(RUMBLE_CAPTURE_SECONDS);
    }

    @Override public void rumble$setCaptureSeconds(int seconds) {
        ((PokemonEntity) (Object) this).getEntityData().set(RUMBLE_CAPTURE_SECONDS, seconds);
    }

    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void rumble$keepUntilDefeat(CallbackInfo ci) {
        if (WildCapture.stunned((PokemonEntity) (Object) this)) ci.cancel();
    }

    @Inject(method = "canBattle", at = @At("HEAD"), cancellable = true)
    private void rumble$cannotChallengeStunned(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (WildCapture.stunned((PokemonEntity) (Object) this)) cir.setReturnValue(false);
    }
}
