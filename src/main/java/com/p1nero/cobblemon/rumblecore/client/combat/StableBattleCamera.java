package com.p1nero.cobblemon.rumblecore.client.combat;

import com.johnn.realtimecobblemon.client.BattleCamera;
import com.johnn.realtimecobblemon.client.ClientSmash;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Render-thread third-person rig: interpolated anchors, time-based damping, then a collision arm.
 * No walk-cycle displacement or tick-velocity look-ahead: both can kick the camera at tick boundaries.
 * Free mouse look stays immediate; horizontal tracking shares the rendered body position.
 * Camera angles and orbit vectors stay double until Minecraft accepts the final float rotation.
 */
public final class StableBattleCamera {
    private static Entity focus;
    private static BlockGetter world;
    private static Integer controlled;
    private static long lastFrame;
    private static Vec3 lastPosition, anchor, targetAnchor, duelOffset;
    private static double yaw, pitch;
    private static double shoulder, armFraction = 1, duelBearing;
    private static boolean wasDuel;
    private static int previousMode = -1;
    private static Vec3 lastOffset, transitionOffset;
    private static double lastYaw, lastPitch, transitionYaw, transitionPitch;
    private static double transition;

    private StableBattleCamera() {}

    public static void reset() {
        focus = null;
        world = null;
        controlled = null;
        lastFrame = 0;
        lastPosition = anchor = targetAnchor = duelOffset = null;
        armFraction = 1;
        previousMode = -1;
        lastOffset = transitionOffset = null;
        transition = 0;
        wasDuel = false;
    }

    public static BattleCamera.Pose place(BlockGetter level, Entity entity, LivingEntity target, float partial,
                                           boolean duel, float desiredShoulder, float sideView) {
        long now = System.nanoTime();
        Vec3 position = RenderAimRay.renderPosition(entity, partial);
        Integer id = ClientSmash.controlledEntity();
        double elapsed = lastFrame == 0 ? 0 : (now - lastFrame) * 1.0e-9;
        boolean cut = focus != entity || world != level || !java.util.Objects.equals(controlled, id)
                || lastPosition == null || position.distanceToSqr(lastPosition) > 64 || elapsed > 1.0;
        double dt = Minecraft.getInstance().isPaused() ? 0 : Math.min(elapsed, 0.1);
        lastFrame = now;
        focus = entity;
        world = level;
        controlled = id;
        lastPosition = position;

        double height = Math.max(0.3, entity.getBbHeight());
        double big = Mth.clamp((height - 0.5) / 2.5, 0, 1);
        Vec3 body = position.add(0, height * 0.62, 0);
        if (cut) {
            anchor = body;
            yaw = entity.getYRot();
            pitch = entity.getXRot();
            shoulder = desiredShoulder;
            armFraction = 1;
            duelOffset = null;
            targetAnchor = null;
            previousMode = -1;
            transition = 0;
        } else {
            // Physics already interpolates the rendered body. Horizontal re-filtering adds acceleration lag
            // and lets the body swim under the reticle; keep only a small vertical step filter.
            anchor = new Vec3(body.x, damp(anchor.y, body.y, dt, 0.075), body.z);
            shoulder = damp(shoulder, desiredShoulder, dt, 0.10);
        }
        if (target != null) {
            Vec3 at = RenderAimRay.renderBounds(target, partial).getCenter();
            // No second target-position filter before the orientation filter: it compounds tracking delay.
            targetAnchor = at;
        } else targetAnchor = null;

        double bias = Mth.lerp(big, 5, 12);
        if (targetAnchor == null) {
            // Mouse movement is already sampled every render frame. Don't re-interpolate it from a tick.
            yaw = entity.getYRot();
            pitch = entity.getXRot();
        } else {
            Vec3 look = targetAnchor.subtract(anchor.add(0, entity.getEyeHeight() - height * 0.62, 0));
            double wantedYaw = yawOf(look);
            double wantedPitch = pitchOf(look);
            yaw = angle(yaw, wantedYaw, cut ? 1 : response(dt, 0.045));
            pitch = damp(pitch, Mth.clamp(wantedPitch, -60, 60), dt, 0.045);
        }

        Vec3 wanted;
        double cameraYaw = yaw;
        double cameraPitch = Mth.clamp(pitch + bias, -89, 89);
        if (sideView > 0) {
            // Preserve authored move close-ups, but use render-frame rotation and the stable body anchor.
            cameraYaw = yaw + 90;
            cameraPitch = 4;
            double radians = Math.toRadians(cameraYaw);
            wanted = anchor.add(direction(0, yaw).scale(sideView * 0.45))
                    .add(Math.sin(radians) * sideView, 0.2, -Math.cos(radians) * sideView);
        } else if (duel && targetAnchor != null) {
            Vec3 delta = targetAnchor.subtract(anchor).multiply(1, 0, 1);
            double distance = delta.length();
            double bearing = distance < 0.6 ? duelBearing : yawOf(delta);
            duelBearing = cut || !wasDuel ? bearing : angle(duelBearing, bearing, response(dt, 0.10));
            Vec3 along = direction(0, duelBearing);
            Vec3 normal = new Vec3(-along.z, 0, along.x);
            double h = Math.max(height, target.getBbHeight() * 0.8);
            Vec3 mid = anchor.add(targetAnchor).scale(0.5);
            double gap = Mth.clamp((distance - 1.5 * h) / (10 * h), 0, 1);
            double back = Math.max(Mth.lerp(gap, 2.75 * h, 5.5 * h), 0.62 * (distance + 1.5 * h));
            Vec3 offset = normal.scale(back).add(0, 0.4 * back, 0);
            duelOffset = duelOffset == null || cut ? offset : duelOffset.lerp(offset, response(dt, 0.10));
            wanted = mid.add(duelOffset);
            Vec3 look = mid.subtract(wanted);
            cameraYaw = yawOf(look);
            cameraPitch = pitchOf(look);
        } else {
            double distance = Mth.clamp(1.9 + 2.1 * height, 2.4, 16);
            double radians = Math.toRadians(yaw);
            Vec3 right = new Vec3(-Math.cos(radians), 0, -Math.sin(radians));
            wanted = anchor.add(direction(cameraPitch, yaw).scale(-distance))
                    .add(0, Mth.lerp(big, 0.12, 0.42) * height + 0.2, 0)
                    .add(right.scale(Mth.clamp(0.3 + 0.4 * height, 0.45, 2.5) * shoulder));
        }
        wasDuel = duel;
        int currentMode = sideView > 0 ? 2 : duel ? 1 : 0;
        if (previousMode >= 0 && previousMode != currentMode) {
            transitionOffset = lastOffset;
            transitionYaw = lastYaw;
            transitionPitch = lastPitch;
            transition = 1;
        }
        if (transition > 0.001 && transitionOffset != null) {
            transition *= 1 - response(dt, 0.10);
            // Blend relative to the moving anchor so mode changes don't pin the camera in world space.
            wanted = anchor.add(transitionOffset.lerp(wanted.subtract(anchor), 1 - transition));
            cameraYaw = angle(transitionYaw, cameraYaw, 1 - transition);
            cameraPitch = Mth.lerp(1 - transition, transitionPitch, cameraPitch);
        }
        previousMode = currentMode;
        lastOffset = wanted.subtract(anchor);
        lastYaw = cameraYaw;
        lastPitch = cameraPitch;
        return new BattleCamera.Pose(collide(level, entity, position.add(0, entity.getEyeHeight(), 0), wanted, dt), (float) cameraYaw, (float) cameraPitch);
    }

    private static Vec3 collide(BlockGetter level, Entity entity, Vec3 pivot, Vec3 wanted, double dt) {
        Vec3 direction = wanted.subtract(pivot);
        double length = direction.length();
        if (length < 1.0e-6) return pivot;
        double safe = length;
        BlockGetter collision = BattleCamera.seeThrough(level);
        // Probe the camera volume rather than one ray. Retract before applying the pose, never through walls.
        for (int i = 0; i < 8; i++) {
            Vec3 offset = new Vec3(((i & 1) * 2 - 1) * 0.12, ((i >> 1 & 1) * 2 - 1) * 0.12,
                    ((i >> 2 & 1) * 2 - 1) * 0.12);
            var hit = collision.clip(new ClipContext(pivot.add(offset), wanted.add(offset),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
            if (hit.getType() != HitResult.Type.MISS) safe = Math.min(safe, Math.max(0, hit.getLocation().distanceTo(pivot.add(offset)) - 0.18));
        }
        double allowed = safe / length;
        // Instant obstruction response, gradual recovery: a spring arm without edge oscillation.
        armFraction = allowed < armFraction ? allowed : damp(armFraction, allowed, dt, 0.18);
        return pivot.add(direction.scale(armFraction));
    }

    static double response(double dt, double seconds) { return -Math.expm1(-dt / seconds); }
    private static double damp(double from, double to, double dt, double seconds) { return Mth.lerp(response(dt, seconds), from, to); }
    private static double angle(double from, double to, double factor) { return from + Math.IEEEremainder(to - from, 360) * factor; }
    private static double yawOf(Vec3 v) { return Math.toDegrees(Math.atan2(v.z, v.x)) - 90; }
    private static double pitchOf(Vec3 v) { return -Math.toDegrees(Math.atan2(v.y, v.horizontalDistance())); }
    private static Vec3 direction(double pitch, double yaw) {
        // Entity.directionFromRotation uses Minecraft's float sine lookup table. An orbit camera needs
        // continuous trigonometry or its position steps between lookup entries even on a smooth mouse turn.
        double p = Math.toRadians(pitch), y = Math.toRadians(yaw), horizontal = Math.cos(p);
        return new Vec3(-Math.sin(y) * horizontal, -Math.sin(p), Math.cos(y) * horizontal);
    }
}
