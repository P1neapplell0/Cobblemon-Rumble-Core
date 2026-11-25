package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Conditions;
import com.johnn.realtimecobblemon.moves.React;
import com.johnn.realtimecobblemon.moves.Stages;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Conditions.class, remap = false)
abstract class RealtimeConditionLimits {
    @Unique private static final Map<Conditions.State, Integer> rumble$statusProcs = new WeakHashMap<>();
    @Unique private static final Map<Conditions.State, Integer> rumble$seedProcs = new WeakHashMap<>();
    @Unique private static final Map<Conditions.State, Integer> rumble$confusionProcs = new WeakHashMap<>();
    private static final String HURT = "Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z";

    @WrapOperation(method = "applyStatus", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;igniteForTicks(I)V"))
    private static void rumble$useOnlyPeriodicBurn(LivingEntity target, int ticks, Operation<Void> original) {
        // RTC's non-Pokemon burn used a second, unbudgeted vanilla fire timer.
    }

    @WrapOperation(method = {"applyStatus", "applyVolatile"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean rumble$limitNativeEffects(LivingEntity target, MobEffectInstance effect, Operation<Boolean> original) {
        // Non-Pokemon poison is settled by the same periodic budget as Pokemon poison.
        if (effect.is(MobEffects.POISON)) return false;
        int cap = effect.is(MobEffects.CONFUSION) ? 40 : 80;
        if (effect.getDuration() > cap) effect = new MobEffectInstance(effect.getEffect(), cap, effect.getAmplifier(),
                effect.isAmbient(), effect.isVisible(), effect.showIcon());
        return original.call(target, effect);
    }

    @WrapOperation(method = "applyStatus", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/Conditions;stun(Lnet/minecraft/world/entity/LivingEntity;I)V"))
    private static void rumble$capSleepFreezeStun(LivingEntity target, int ticks, Operation<Void> original) {
        original.call(target, Math.min(ticks, 80));
    }

    @WrapOperation(method = "applyVolatile", at = @At(value = "INVOKE", ordinal = 0, target =
            "Lcom/johnn/realtimecobblemon/moves/MoveEngine;later(Lnet/minecraft/server/level/ServerLevel;ILcom/johnn/realtimecobblemon/Smash$Session;Ljava/lang/Runnable;)V"))
    private static void rumble$capTrapDamage(ServerLevel level, int delay, Smash.Session session, Runnable action, Operation<Void> original) {
        if (delay <= 40) original.call(level, delay, session, action);
    }

    @Inject(method = "applyStatus", at = @At("RETURN"))
    private static void rumble$resetStatus(LivingEntity target, String effect, Smash.Session source, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        var state = Conditions.get(target);
        rumble$statusProcs.remove(state);
        var timers = (ConditionTimers) (Object) state;
        if ("par".equals(effect) || "slp".equals(effect) || "frz".equals(effect)) {
            long end = Math.min(timers.rumble$statusUntil(), target.level().getGameTime() + 80);
            timers.rumble$expireStatus(end);
            if ("frz".equals(effect)) target.setTicksFrozen(target.getTicksRequiredToFreeze() + (int) (end - target.level().getGameTime()));
        }
    }

    @Inject(method = "applyVolatile", at = @At("RETURN"))
    private static void rumble$resetVolatile(LivingEntity target, String effect, Smash.Session source, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        var state = Conditions.get(target);
        if ("leechseed".equals(effect)) rumble$seedProcs.remove(state);
        if ("confusion".equals(effect)) {
            rumble$confusionProcs.remove(state);
            var timers = (ConditionTimers) (Object) state;
            timers.rumble$clearConfusion(Math.min(timers.rumble$confusedUntil(), target.level().getGameTime() + 40));
        }
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = HURT, ordinal = 0))
    private static boolean rumble$limitPeriodicStatus(LivingEntity target, DamageSource source, float damage,
            Operation<Boolean> original, @Local(argsOnly = true) Conditions.State state) {
        boolean hit = original.call(target, source, damage);
        if (hit) rumble$countStatus(target, state);
        return hit;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = HURT, ordinal = 1))
    private static boolean rumble$limitLeech(LivingEntity target, DamageSource source, float damage, Operation<Boolean> original,
            @Local(argsOnly = true) Conditions.State state, @Share("leechHit") LocalBooleanRef leechHit) {
        boolean hit = original.call(target, source, damage);
        leechHit.set(hit);
        if (hit && rumble$seedProcs.merge(state, 1, Integer::sum) == 2) Conditions.clearLeech(target);
        return hit;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/MoveEngine;healPokemon(Lcom/johnn/realtimecobblemon/Smash$Session;F)V"))
    private static void rumble$healOnlyOnDrain(Smash.Session session, float amount, Operation<Void> original,
            @Share("leechHit") LocalBooleanRef leechHit) {
        if (leechHit.get()) original.call(session, amount);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/Conditions;stun(Lnet/minecraft/world/entity/LivingEntity;I)V"))
    private static void rumble$limitParalysis(LivingEntity target, int ticks, Operation<Void> original,
            @Local(argsOnly = true) Conditions.State state, @Share("paralysisExhausted") LocalBooleanRef exhausted) {
        long before = ((ConditionTimers) (Object) state).rumble$stunUntil();
        original.call(target, ticks);
        if (((ConditionTimers) (Object) state).rumble$stunUntil() > before) {
            exhausted.set(rumble$statusProcs.merge(state, 1, Integer::sum) >= 2);
        }
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;push(DDD)V"))
    private static void rumble$limitConfusion(Mob target, double x, double y, double z, Operation<Void> original) {
        original.call(target, x, y, z);
        rumble$countConfusion(target);
    }

    @WrapOperation(method = "onAttack", at = @At(value = "INVOKE", target = HURT))
    private static boolean rumble$countConfusedAttack(LivingEntity attacker, DamageSource source, float damage, Operation<Boolean> original) {
        // The attack was already cancelled by confusion even if the self-damage is blocked.
        rumble$countConfusion(attacker);
        return original.call(attacker, source, damage);
    }

    @Unique private static void rumble$countConfusion(LivingEntity target) {
        var state = Conditions.get(target);
        if (state != null && rumble$confusionProcs.merge(state, 1, Integer::sum) >= 2) {
            ((ConditionTimers) (Object) state).rumble$clearConfusion(0);
            target.removeEffect(MobEffects.CONFUSION);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private static void rumble$endParalysis(ServerLevel level, LivingEntity target, Conditions.State state, long now, CallbackInfo ci,
            @Share("paralysisExhausted") LocalBooleanRef exhausted) {
        if (exhausted.get()) rumble$clearStatus(target, state);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private static void rumble$expireOwnedStatus(ServerLevel level, LivingEntity target, Conditions.State state, long now, CallbackInfo ci) {
        var timers = (ConditionTimers) (Object) state;
        String effect = timers.rumble$status();
        if (effect == null) return;
        if (now >= timers.rumble$statusUntil()) {
            rumble$clearStatus(target, state);
            return;
        }
        if (target instanceof PokemonEntity || now < timers.rumble$nextTick()) return;
        float ratio = switch (effect) {
            case "brn" -> 1F / 16;
            case "psn" -> 1F / 8;
            case "tox" -> (1 + rumble$statusProcs.getOrDefault(state, 0)) / 16F;
            default -> 0;
        };
        if (ratio > 0 && target.hurt(level.damageSources().magic(), Math.max(1, target.getMaxHealth() * ratio))) {
            rumble$countStatus(target, state);
        }
    }

    @Unique private static void rumble$countStatus(LivingEntity target, Conditions.State state) {
        if (rumble$statusProcs.merge(state, 1, Integer::sum) >= 2) rumble$clearStatus(target, state);
    }

    @Unique private static void rumble$clearStatus(LivingEntity target, Conditions.State state) {
        var timers = (ConditionTimers) (Object) state;
        String effect = timers.rumble$status();
        if (effect == null) return;
        if (target instanceof PokemonEntity pokemon && effect.equals(Conditions.status(target))) pokemon.getPokemon().setStatus(null);
        timers.rumble$setStatus(null);
        timers.rumble$expireStatus(0);
        var slow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if ("par".equals(effect) && slow != null && slow.getAmplifier() == 1) target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if ("frz".equals(effect)) target.setTicksFrozen(0);
        React.clear(target, effect);
    }
}

@Mixin(value = Stages.class, remap = false)
abstract class RealtimeNegativeStageLimits {
    @ModifyConstant(method = "change", constant = @Constant(longValue = 300))
    private long rumble$shorterStatDebuff(long duration, String stat, int delta, long now) {
        return delta < 0 ? 80 : duration;
    }
}
