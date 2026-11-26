package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.MoveCombat;
import com.johnn.realtimecobblemon.moves.MoveEngine;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeWorldInteractions;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** RTC 1.0.3alpha.1: uses actual beam spans, shockwave rings and projectile wall contacts; review lambda indices on update. */
@Mixin(value = MoveEngine.class, remap = false)
abstract class RealtimeTerrainImpact {
    @WrapOperation(method = "deal(Lcom/johnn/realtimecobblemon/moves/MoveEngine$Cast;Lnet/minecraft/world/entity/LivingEntity;F)F",
            at = @At(value = "INVOKE", target =
                    "Lcom/johnn/realtimecobblemon/moves/MoveEngine;deal(Lcom/johnn/realtimecobblemon/Smash$Session;Lnet/minecraft/world/entity/LivingEntity;F)F"))
    private static float rumble$elementOnDamage(Smash.Session session, LivingEntity target, float amount,
                                                Operation<Float> original, @Coerce Object cast) {
        float before = target.getHealth() + target.getAbsorptionAmount();
        float result = original.call(session, target, amount);
        if (target.getHealth() + target.getAbsorptionAmount() < before)
            RealtimeWorldInteractions.onMoveHit(session, ((RealtimeMeleeCastAccess) cast).rumble$info(), target);
        return result;
    }

    @Inject(method = "lambda$shockwave$30", at = @At("HEAD"))
    private static void rumble$shockwave(double radius, double max, int rings, @Coerce Object cast, Vec3 center,
                                         boolean dust, boolean water, Smash.Session session, double previous, CallbackInfo ci) {
        var access = (RealtimeMeleeCastAccess) cast;
        if (radius != max / rings || !access.rumble$alive()) return;
        var start = center.add(0, .4, 0);
        var forward = session.player.getLookAngle().multiply(1, 0, 1).normalize().add(0, -.25, 0);
        RealtimeWorldInteractions.trace(session, access.rumble$info(), start, start.add(forward.scale(Math.min(5, max))), true);
    }

    @Inject(method = "lambda$beam$24", at = @At(value = "FIELD", target =
            "Lcom/johnn/realtimecobblemon/moves/MoveEngine$Cast;level:Lnet/minecraft/server/level/ServerLevel;", ordinal = 0))
    private static void rumble$beam(@Coerce Object cast, Smash.Session session, LivingEntity target, MoveCombat.Beam ray,
                                    boolean soundOnly, int pulse, float scale, CallbackInfo ci,
                                    @Local(index = 7) Vec3 from, @Local(index = 8) Vec3 to) {
        var access = (RealtimeMeleeCastAccess) cast;
        if (!soundOnly && pulse == 0 && access.rumble$alive()) RealtimeWorldInteractions.trace(session, access.rumble$info(), from, to.add(to.subtract(from).normalize().scale(.02)), false);
    }

    @Inject(method = "lambda$stream$26", at = @At(value = "FIELD", target =
            "Lcom/johnn/realtimecobblemon/moves/MoveEngine$Cast;look:Lcom/johnn/realtimecobblemon/moves/Looks$Def;", ordinal = 0))
    private static void rumble$stream(@Coerce Object cast, Smash.Session session, double range, int pulse, CallbackInfo ci,
                                      @Local(index = 5) Vec3 direction, @Local(index = 6) Vec3 from) {
        var access = (RealtimeMeleeCastAccess) cast;
        if (pulse == 0 && access.rumble$alive())
            RealtimeWorldInteractions.trace(session, access.rumble$info(), from, from.add(direction.scale(range)), false);
    }

}

/** RTC 1.0.3alpha.1: tick's land call is reached only after a wall collision, not on entity hits. */
@Mixin(targets = "com.johnn.realtimecobblemon.moves.MoveEngine$Projectile", remap = false)
abstract class RealtimeProjectileTerrainImpact {
    @Inject(method = "tick", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/MoveEngine$Projectile;land(Lnet/minecraft/world/phys/Vec3;)V"))
    private void rumble$contact(CallbackInfoReturnable<Boolean> ci, @Local(index = 5) Vec3 contact) {
        rumble$trace(contact, .03);
    }

    // RTC ignores fluids and non-solid fire. Check a traveled segment only if the shot did not hit an entity.
    @Inject(method = "tick", at = @At(value = "FIELD", target =
            "Lcom/johnn/realtimecobblemon/moves/MoveEngine$Projectile;pos:Lnet/minecraft/world/phys/Vec3;",
            opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    private void rumble$travel(CallbackInfoReturnable<Boolean> ci, @Local(index = 5) Vec3 end) {
        var ball = (com.johnn.realtimecobblemon.moves.Clash.Ball) (Object) this;
        if ("ice".equals(ball.move().type()) || "water".equals(ball.move().type())) rumble$trace(end, 0);
    }

    @org.spongepowered.asm.mixin.Unique
    private void rumble$trace(Vec3 contact, double extension) {
        var ball = (com.johnn.realtimecobblemon.moves.Clash.Ball) (Object) this;
        if (!(ball.owner() instanceof net.minecraft.server.level.ServerPlayer player)) return;
        var session = Smash.session(player);
        if (session != null) RealtimeWorldInteractions.trace(session, ball.move(), ball.pos(),
                contact.add(contact.subtract(ball.pos()).normalize().scale(extension)), false);
    }
}
