package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.moves.Conditions;
import com.johnn.realtimecobblemon.moves.Combo;
import com.johnn.realtimecobblemon.moves.Motion;
import com.johnn.realtimecobblemon.Smash;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaTenacity;
import com.p1nero.cobblemon.rumblecore.combat.arena.RealtimeProtection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Conditions.class, remap = false)
abstract class TenacityRTCConditions {
    @Inject(method = {"stun", "launch", "juggle"}, at = @At("HEAD"), cancellable = true)
    private static void rumble$ignoreStunDuringGetUp(LivingEntity entity, int ticks, CallbackInfo ci) {
        if (RealtimeProtection.megaProtected(entity) || entity instanceof PokemonEntity pokemon && ArenaTenacity.active(pokemon)) ci.cancel();
    }

    @Inject(method = {"stun", "launch", "juggle"}, at = @At("RETURN"))
    private static void rumble$recordStagger(LivingEntity entity, int ticks, CallbackInfo ci) {
        if (entity instanceof PokemonEntity pokemon) ArenaTenacity.staggered(pokemon);
    }
}

@Mixin(value = Motion.class, remap = false)
abstract class TenacityRTCImpulse {
    @Inject(method = "knock", at = @At("HEAD"), cancellable = true)
    private static void rumble$ignoreDirectImpulse(LivingEntity entity, Vec3 velocity, CallbackInfo ci) {
        if (RealtimeProtection.megaProtected(entity) || entity instanceof PokemonEntity pokemon && ArenaTenacity.active(pokemon)) ci.cancel();
    }
}

@Mixin(value = Combo.class, remap = false)
abstract class TenacityRTCLauncher {
    @Inject(method = "lambda$strike$3", at = @At("HEAD"), cancellable = true)
    private static void rumble$ignoreLauncher(LivingEntity entity, Vec3 direction, Smash.Session attacker,
                                             ServerLevel level, CallbackInfo ci) {
        // Suppress the entire launch reaction, including its later ground slam. Damage has already resolved.
        if (RealtimeProtection.megaProtected(entity) || entity instanceof PokemonEntity pokemon && ArenaTenacity.active(pokemon)) ci.cancel();
    }
}

@Mixin(LivingEntity.class)
abstract class TenacityKnockback {
    @Inject(method = "knockback(DDD)V", at = @At("HEAD"), cancellable = true)
    private void rumble$ignoreKnockbackDuringGetUp(double strength, double x, double z, CallbackInfo ci) {
        if (RealtimeProtection.megaProtected((LivingEntity) (Object) this)
                || (Object) this instanceof PokemonEntity pokemon && ArenaTenacity.active(pokemon)) ci.cancel();
    }
}
