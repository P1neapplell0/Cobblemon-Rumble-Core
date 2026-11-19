package com.p1nero.cobblemon.rumblecore.prologue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** The client prompt and the server interaction use the same short aim ray. */
public final class DialogueTarget {
    private DialogueTarget() {}

    public static boolean aimedAt(Player player, Entity actor) {
        if (actor == null || !actor.isAlive() || actor.level() != player.level()
                || !player.isAlive() || player.isSpectator() || !PrologueWorld.GYMS.equals(player.level().dimension())
                || player.distanceToSqr(actor) > ProfessorEntity.TALK_DISTANCE * ProfessorEntity.TALK_DISTANCE) return false;

        var eye = player.getEyePosition();
        var end = eye.add(Vec3.directionFromRotation(player.getXRot(), player.getYRot()).scale(ProfessorEntity.TALK_DISTANCE + 0.25));
        var bounds = actor.getBoundingBox().inflate(0.12);
        if (bounds.contains(eye)) return false;
        var hit = bounds.clip(eye, end);
        return hit.isPresent() && player.hasLineOfSight(actor)
                && player.level().clip(new ClipContext(eye, hit.get(), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }
}
