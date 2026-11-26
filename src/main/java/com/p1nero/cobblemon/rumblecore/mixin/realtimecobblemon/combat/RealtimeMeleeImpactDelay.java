package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Looks;
import com.johnn.realtimecobblemon.moves.MoveEngine;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMelee;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeWorldInteractions;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/** RTC 1.0.3alpha.1: melee schedules a lambda with two visual branches and a single target iterator. */
@Mixin(value = MoveEngine.class, remap = false)
abstract class RealtimeMeleeImpactDelay {
    @Unique private static final ThreadLocal<Vec3> rumble$meleePass = new ThreadLocal<>();

    @WrapOperation(method = "melee", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/MoveEngine;later(Lnet/minecraft/server/level/ServerLevel;ILcom/johnn/realtimecobblemon/Smash$Session;Ljava/lang/Runnable;)V"))
    private static void rumble$schedule(ServerLevel level, int ticks, Smash.Session session, Runnable hit, Operation<Void> original,
                                       @Local(ordinal = 0) List<LivingEntity> selected) {
        Vec3 facing = RealtimeMelee.direction(session, selected.isEmpty() ? null : selected.getFirst(), session.player.getLookAngle());
        original.call(level, ticks, session, (Runnable) () -> rumble$runPass(Vec3.ZERO, hit));
        original.call(level, ticks + RealtimeMoveWindup.MELEE_IMPACT_TICKS, session, (Runnable) () -> {
            if (Smash.session(session.player) == session && session.player.level() == level && session.pokemon.isAlive())
                rumble$runPass(facing, hit);
        });
    }

    @Unique private static void rumble$runPass(Vec3 pass, Runnable hit) {
        var previous = rumble$meleePass.get();
        rumble$meleePass.set(pass);
        try { hit.run(); }
        finally {
            if (previous == null) rumble$meleePass.remove(); else rumble$meleePass.set(previous);
        }
    }

    @WrapOperation(method = "lambda$melee$14", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private static Iterator<?> rumble$targets(List<?> initial, Operation<Iterator<?>> original,
                                             @Coerce Object cast, Smash.Session session, double range, List<?> ignored, int hitIndex) {
        var facing = rumble$meleePass.get();
        if (facing == null) return original.call(initial);
        var access = (RealtimeMeleeCastAccess) cast;
        if (facing == Vec3.ZERO || !access.rumble$alive()) return Collections.emptyIterator();
        var targets = RealtimeMelee.meleeTargets(session, new RealtimeMelee.Aim(null, facing, Vec3.ZERO, 0));
        if (hitIndex == 0) RealtimeWorldInteractions.melee(session, access.rumble$info(), facing);
        return access.rumble$single() && targets.size() > 1 ? targets.subList(0, 1).iterator() : targets.iterator();
    }

    @WrapOperation(method = "lambda$melee$14", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/MoveEngine;swipe(Lcom/johnn/realtimecobblemon/moves/MoveEngine$Cast;D)V"))
    private static void rumble$swipeOnce(@Coerce Object cast, double range, Operation<Void> original) {
        var pass = rumble$meleePass.get();
        if (pass == null || pass == Vec3.ZERO) original.call(cast, range);
    }

    @WrapOperation(method = "lambda$melee$14", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/Looks;send(Lnet/minecraft/server/level/ServerLevel;Lcom/johnn/realtimecobblemon/moves/Looks$Def;ILnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;FILnet/minecraft/world/entity/Entity;I)V"))
    private static void rumble$looksOnce(ServerLevel level, Looks.Def look, int kind, Vec3 from, Vec3 direction,
                                         float size, int ticks, Entity body, int seed, Operation<Void> original) {
        var pass = rumble$meleePass.get();
        if (pass == null || pass == Vec3.ZERO) original.call(level, look, kind, from, direction, size, ticks, body, seed);
    }
}
