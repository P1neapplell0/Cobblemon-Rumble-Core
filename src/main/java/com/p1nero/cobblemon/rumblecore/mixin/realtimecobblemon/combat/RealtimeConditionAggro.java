package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Brawl;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** RTC 1.0.3alpha.1: hook successful primary conditions, not the damage path or periodic ticks. */
@Mixin(targets = "com.johnn.realtimecobblemon.moves.MoveEngine", remap = false)
abstract class RealtimeConditionAggro {
    @WrapOperation(method = "statusOn", at = {
        @At(value = "INVOKE", target = "Lcom/johnn/realtimecobblemon/moves/Conditions;applyStatus(Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;Lcom/johnn/realtimecobblemon/Smash$Session;)Z"),
        @At(value = "INVOKE", target = "Lcom/johnn/realtimecobblemon/moves/Conditions;applyVolatile(Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;Lcom/johnn/realtimecobblemon/Smash$Session;)Z")
    }, require = 2)
    private static boolean rumble$provoke(LivingEntity target, String effect, Smash.Session source, Operation<Boolean> original) {
        boolean applied = original.call(target, effect, source);
        if (applied && target != source.pokemon && target != source.player && Smash.session(source.player) == source)
            Brawl.provoke(source, target);
        return applied;
    }
}

/** RTC 1.0.3alpha.1: a missed seed can sprout and attach via this separate ground job. */
@Mixin(targets = "com.johnn.realtimecobblemon.moves.Status$13", remap = false)
abstract class RealtimeSeedAggro {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/Conditions;applyVolatile(Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;Lcom/johnn/realtimecobblemon/Smash$Session;)Z"))
    private boolean rumble$provokeFromSprout(LivingEntity target, String effect, Smash.Session source, Operation<Boolean> original) {
        boolean applied = original.call(target, effect, source);
        if (applied && target != source.pokemon && target != source.player && Smash.session(source.player) == source)
            Brawl.provoke(source, target);
        return applied;
    }
}
