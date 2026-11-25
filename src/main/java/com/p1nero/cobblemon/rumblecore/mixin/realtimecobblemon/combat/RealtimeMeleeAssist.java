package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Brawl;
import com.johnn.realtimecobblemon.moves.Combo;
import com.johnn.realtimecobblemon.moves.Fighting;
import com.johnn.realtimecobblemon.moves.MoveEngine;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMelee;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = MoveEngine.class, remap = false)
abstract class RealtimeMeleeAssist {
    private static final String LOOK = "Lnet/minecraft/server/level/ServerPlayer;getLookAngle()Lnet/minecraft/world/phys/Vec3;";
    private static final String PUSH = "Lcom/johnn/realtimecobblemon/moves/Motion;push(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/phys/Vec3;I)V";

    @WrapOperation(method = "melee", at = @At(value = "INVOKE", target = LOOK))
    private static Vec3 rumble$aimMelee(ServerPlayer player, Operation<Vec3> original, @Share("aim") LocalRef<RealtimeMelee.Aim> shared) {
        var session = Smash.session(player);
        Vec3 look = original.call(player);
        if (session == null) return look;
        var aim = RealtimeMelee.aim(session, look, 8.0);
        shared.set(aim);
        return aim.direction();
    }

    @WrapOperation(method = "melee", at = @At(value = "INVOKE", target = PUSH))
    private static void rumble$stepMelee(ServerPlayer player, Vec3 motion, int ticks, Operation<Void> original,
            @Share("aim") LocalRef<RealtimeMelee.Aim> shared) {
        Vec3 adjusted = RealtimeMelee.guidedStep(motion, shared.get());
        // A zero-distance lunge still brakes RTC locomotion; don't enqueue it for an in-range attack.
        if (adjusted.lengthSqr() > 0) original.call(player, adjusted, ticks);
    }

    @WrapOperation(method = "melee", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/MoveCombat;meleeTargets(Lcom/johnn/realtimecobblemon/Smash$Session;D)Ljava/util/List;"))
    private static List<LivingEntity> rumble$reachFromHitbox(Smash.Session session, double range,
            Operation<List<LivingEntity>> original, @Share("aim") LocalRef<RealtimeMelee.Aim> shared) {
        var aim = shared.get();
        return aim == null ? original.call(session, range) : RealtimeMelee.meleeTargets(session, aim);
    }

    @WrapOperation(method = "lambda$melee$14", at = @At(value = "INVOKE", target = LOOK))
    private static Vec3 rumble$aimMeleeEffects(ServerPlayer player, Operation<Vec3> original, @Local(argsOnly = true) List<LivingEntity> targets) {
        return RealtimeMelee.direction(Smash.session(player), targets.isEmpty() ? null : targets.getFirst(), original.call(player));
    }

    @WrapOperation(method = "dash", at = @At(value = "INVOKE", target = LOOK))
    private static Vec3 rumble$aimDash(ServerPlayer player, Operation<Vec3> original, @Share("dashAim") LocalRef<RealtimeMelee.Aim> shared) {
        var session = Smash.session(player);
        Vec3 look = original.call(player);
        if (session == null) return look;
        var aim = RealtimeMelee.aim(session, look, 8.0);
        shared.set(aim);
        return aim.direction();
    }

    @WrapOperation(method = "dash", at = @At(value = "INVOKE", target = PUSH))
    private static void rumble$stepDash(ServerPlayer player, Vec3 motion, int ticks, Operation<Void> original,
            @Share("dashAim") LocalRef<RealtimeMelee.Aim> shared) {
        var aim = shared.get();
        original.call(player, aim == null ? motion : motion.add(aim.step()), ticks);
    }

    @WrapOperation(method = "lambda$dash$18", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/MoveCombat;canHit(Lcom/johnn/realtimecobblemon/Smash$Session;Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private static boolean rumble$dashCannotHitThroughWalls(Smash.Session session, LivingEntity target, Operation<Boolean> original) {
        return original.call(session, target) && session.player.hasLineOfSight(target);
    }

}

@Mixin(value = Combo.class, remap = false)
abstract class RealtimeComboAssist {
    private static final String LOOK = "Lnet/minecraft/server/level/ServerPlayer;getLookAngle()Lnet/minecraft/world/phys/Vec3;";
    private static final String PUSH = "Lcom/johnn/realtimecobblemon/moves/Motion;push(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/phys/Vec3;I)V";
    private static final String PICK = "Lcom/johnn/realtimecobblemon/moves/Combo;pick(Lcom/johnn/realtimecobblemon/Smash$Session;D)Lnet/minecraft/world/entity/LivingEntity;";

    @WrapOperation(method = "hit", at = @At(value = "INVOKE", target = PICK))
    private static LivingEntity rumble$pickCombo(Smash.Session session, double range, Operation<LivingEntity> original,
            @Share("aim") LocalRef<RealtimeMelee.Aim> shared) {
        var aim = RealtimeMelee.aim(session, session.player.getLookAngle(), RealtimeMelee.ASSIST_SEARCH_REACH);
        shared.set(aim);
        return RealtimeMelee.meleeTargets(session, aim).stream().findFirst().orElse(null);
    }

    @WrapOperation(method = "hit", at = @At(value = "INVOKE", target = LOOK))
    private static Vec3 rumble$aimCombo(ServerPlayer player, Operation<Vec3> original, @Share("aim") LocalRef<RealtimeMelee.Aim> shared) {
        return shared.get() == null ? original.call(player) : shared.get().direction();
    }

    @WrapOperation(method = "hit", at = @At(value = "INVOKE", target = PUSH))
    private static void rumble$stepCombo(ServerPlayer player, Vec3 motion, int ticks, Operation<Void> original,
            @Share("aim") LocalRef<RealtimeMelee.Aim> shared) {
        Vec3 adjusted = RealtimeMelee.guidedStep(motion, shared.get());
        // A zero-distance lunge still brakes RTC locomotion; don't enqueue it for an in-range attack.
        if (adjusted.lengthSqr() > 0) original.call(player, adjusted, ticks);
    }

    @WrapOperation(method = "strike", at = @At(value = "INVOKE", target = LOOK))
    private static Vec3 rumble$aimKnockback(ServerPlayer player, Operation<Vec3> original, @Local(argsOnly = true) LivingEntity target) {
        return RealtimeMelee.direction(Smash.session(player), target, original.call(player));
    }
}

@Mixin(value = Fighting.class, remap = false)
abstract class RealtimeHeavyAssist {
    private static final String LOOK = "Lnet/minecraft/server/level/ServerPlayer;getLookAngle()Lnet/minecraft/world/phys/Vec3;";
    private static final String PUSH = "Lcom/johnn/realtimecobblemon/moves/Motion;push(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/phys/Vec3;I)V";
    private static final String PICK = "Lcom/johnn/realtimecobblemon/moves/Combo;pick(Lcom/johnn/realtimecobblemon/Smash$Session;D)Lnet/minecraft/world/entity/LivingEntity;";

    @WrapOperation(method = "fireHeavy", at = @At(value = "INVOKE", target = PICK))
    private static LivingEntity rumble$pickHeavy(Smash.Session session, double range, Operation<LivingEntity> original,
            @Share("aim") LocalRef<RealtimeMelee.Aim> shared) {
        var aim = RealtimeMelee.aim(session, session.player.getLookAngle(), RealtimeMelee.ASSIST_SEARCH_REACH);
        shared.set(aim);
        return RealtimeMelee.meleeTargets(session, aim).stream().findFirst().orElse(null);
    }

    @WrapOperation(method = "fireHeavy", at = @At(value = "INVOKE", target = LOOK))
    private static Vec3 rumble$aimHeavy(ServerPlayer player, Operation<Vec3> original, @Share("aim") LocalRef<RealtimeMelee.Aim> shared) {
        return shared.get() == null ? original.call(player) : shared.get().direction();
    }

    @WrapOperation(method = "fireHeavy", at = @At(value = "INVOKE", target = PUSH))
    private static void rumble$stepHeavy(ServerPlayer player, Vec3 motion, int ticks, Operation<Void> original,
            @Share("aim") LocalRef<RealtimeMelee.Aim> shared) {
        Vec3 adjusted = RealtimeMelee.guidedStep(motion, shared.get());
        // A zero-distance lunge still brakes RTC locomotion; don't enqueue it for an in-range attack.
        if (adjusted.lengthSqr() > 0) original.call(player, adjusted, ticks);
    }
}

@Mixin(value = Brawl.class, remap = false)
abstract class RealtimeAiMeleeReach {
    @ModifyConstant(method = "reach", constant = @Constant(doubleValue = 1.4))
    private static double rumble$reachFromHitbox(double previous) { return RealtimeMelee.REACH; }
}
