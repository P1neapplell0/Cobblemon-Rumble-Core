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
    private static Vec3 lastPosition, anchor;
    private static double shoulder, armFraction = 1, duelYaw, duelPan, duelDistance;
    private static Vec3 framingOffset;
    private static LivingEntity previousTarget;
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
        lastPosition = anchor = null;
        armFraction = 1;
        previousMode = -1;
        previousTarget = null;
        lastOffset = transitionOffset = null;
        transition = 0;
        framingOffset = null;
    }

    public static BattleCamera.Pose place(BlockGetter level, Entity entity, LivingEntity target, float partial,
                                           float desiredShoulder, float sideView) {
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

        Entity model = id == null || Minecraft.getInstance().level == null ? entity
                : Minecraft.getInstance().level.getEntity(id);
        if (model == null) model = entity;
        double height = Math.max(0.3, model.getBbHeight());
        double big = Mth.clamp((height - 0.5) / 2.5, 0, 1);
        Vec3 body = position.add(0, height * 0.62, 0);
        if (cut) {
            anchor = body;
            shoulder = desiredShoulder;
            armFraction = 1;
            duelYaw = BattleOrbit.yaw(entity);
            duelPan = duelDistance = 0;
            framingOffset = null;
            previousMode = -1;
            transition = 0;
        } else {
            // Physics already interpolates the rendered body. Horizontal re-filtering adds acceleration lag
            // and lets the body swim under the reticle; keep only a small vertical step filter.
            anchor = new Vec3(body.x, damp(anchor.y, body.y, dt, 0.075), body.z);
            shoulder = damp(shoulder, desiredShoulder, dt, 0.10);
        }
        double yaw = BattleOrbit.yaw(entity), pitch = BattleOrbit.pitch(entity);
        double freeTilt = Mth.lerp(big, 5, 12);
        double cameraYaw = yaw, cameraPitch = Mth.clamp(pitch + freeTilt, -89, 89);
        Vec3 wanted;
        Vec3 opponent = target != null && sideView <= 0
                ? RenderAimRay.renderBounds(target, partial).getCenter() : null;
        int currentMode = sideView > 0 ? 2 : opponent != null ? 1 : 0;
        if (previousMode >= 0 && (previousMode != currentMode || currentMode == 1 && previousTarget != target)) {
            transitionOffset = lastOffset;
            transitionYaw = lastYaw;
            transitionPitch = lastPitch;
            transition = 1;
        }
        if (sideView > 0) {
            // Preserve authored move close-ups, but use render-frame rotation and the stable body anchor.
            double facing = entity.getYRot();
            cameraYaw = facing + 90;
            cameraPitch = 4;
            double radians = Math.toRadians(cameraYaw);
            wanted = anchor.add(direction(0, facing).scale(sideView * 0.45))
                    .add(Math.sin(radians) * sideView, 0.2, -Math.cos(radians) * sideView);
        } else if (opponent != null) {
            Vec3 separation = opponent.subtract(anchor);
            double gap = separation.horizontalDistance();
            double contact = Math.max(1.15, (model.getBbWidth() + target.getBbWidth()) * 0.55);
            double tracking = smoothstep((gap - contact * 0.55) / (1.6 + contact * 0.45));
            if (previousMode != 1) {
                duelYaw = previousMode == 0 ? lastYaw : yaw;
                duelPan = 0;
                duelDistance = Mth.clamp(2.1 + 2.1 * height, 2.4, 16);
                framingOffset = Vec3.ZERO;
            }
            // During a crossover the bearing stops being useful. Keep the shoulder through contact,
            // then gradually follow again as the fighters separate. Mouse orbit stays available.
            if (tracking > 0) {
                duelYaw = steer(duelYaw, yawOf(separation), dt * tracking, .22, 115);
            }
            duelPan = damp(duelPan, BattleOrbit.pan(), dt, .10);
            double orbitYaw = duelYaw + duelPan;
            double size = Math.max(height, Math.min(4, target.getBbHeight()) * .65);
            double close = 1 - tracking;
            double distance = Mth.clamp(3.1 + 1.5 * size + gap * .17 + close * .65, 3.5, 15);
            duelDistance = damp(duelDistance, distance, dt, .21);

            // Lift and widen the same shoulder pose during close contact. Never jump to another
            // camera position just because the sight line grazes the controlled model's hitbox.
            double elevation = Mth.clamp(12 + BattleOrbit.tilt() + close * 9 + Math.max(0, -separation.y) * 1.2, 3, 37);
            double radians = Math.toRadians(orbitYaw);
            Vec3 right = new Vec3(-Math.cos(radians), 0, -Math.sin(radians));
            double side = Math.max(model.getBbWidth() * .85 + .68, height * .68) + close * .46;
            wanted = anchor.add(orientedOffset(separation, .18, .08, 2.4, 1))
                    .add(direction(elevation, orbitYaw).scale(-duelDistance)).add(0, .24, 0)
                    .add(right.scale(side * shoulder));

            // Keep both fighters in the composition. A slight bias toward the opponent helps read attacks.
            Vec3 framing = orientedOffset(separation, .58, .50, 16, 5);
            framingOffset = framingOffset.lerp(framing, response(dt, .16));
        } else {
            double distance = Mth.clamp(2.1 + 2.1 * height, 2.4, 16);
            double radians = Math.toRadians(cameraYaw);
            Vec3 right = new Vec3(-Math.cos(radians), 0, -Math.sin(radians));
            wanted = anchor.add(direction(cameraPitch, cameraYaw).scale(-distance))
                    .add(0, Mth.lerp(big, 0.12, 0.42) * height + 0.2, 0)
                    .add(right.scale(Mth.clamp(0.56 + 0.52 * Math.max(height, model.getBbWidth()), 0.8, 3.4) * shoulder));
        }
        if (transition > .001 && transitionOffset != null) {
            transition *= 1 - response(dt, .13);
            // Blend relative to the moving body. The look angle is blended independently below.
            wanted = anchor.add(transitionOffset.lerp(wanted.subtract(anchor), 1 - transition));
        }
        Vec3 pivot = position.add(0, entity.getEyeHeight(), 0);
        Vec3 camera = collide(level, entity, pivot, wanted, dt);
        if (opponent != null) {
            Vec3 look = anchor.add(framingOffset).subtract(camera);
            cameraYaw = look.horizontalDistanceSqr() > .0025 ? yawOf(look) : duelYaw + duelPan;
            if (look.lengthSqr() > .0025) cameraPitch = pitchOf(look);
        }
        if (transition > .001) {
            cameraYaw = angle(transitionYaw, cameraYaw, 1 - transition);
            cameraPitch = Mth.lerp(1 - transition, transitionPitch, cameraPitch);
        }
        if (currentMode == 1 && previousMode == 1) {
            double deltaYaw = Math.IEEEremainder(cameraYaw - lastYaw, 360);
            cameraYaw = lastYaw + Mth.clamp(deltaYaw, -175 * dt, 175 * dt);
            cameraPitch = lastPitch + Mth.clamp(cameraPitch - lastPitch, -125 * dt, 125 * dt);
        }
        previousMode = currentMode;
        previousTarget = currentMode == 1 ? target : null;
        lastOffset = wanted.subtract(anchor);
        lastYaw = cameraYaw;
        lastPitch = cameraPitch;
        float finalPitch = (float) Mth.clamp(cameraPitch, -89, 89);
        if (sideView <= 0) BattleOrbit.rendered(entity, (float) cameraYaw, finalPitch, (float) freeTilt);
        return new BattleCamera.Pose(camera, (float) cameraYaw, finalPitch);
    }

    private static Vec3 orientedOffset(Vec3 separation, double horizontalWeight, double verticalWeight,
                                       double maxHorizontal, double maxVertical) {
        double scale = Math.min(horizontalWeight, maxHorizontal / Math.max(separation.horizontalDistance(), 1));
        return new Vec3(separation.x * scale, Mth.clamp(separation.y * verticalWeight, -maxVertical, maxVertical),
                separation.z * scale);
    }

    private static double smoothstep(double value) {
        double t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static double steer(double from, double to, double dt, double seconds, double maxSpeed) {
        double delta = Math.IEEEremainder(to - from, 360) * response(dt, seconds);
        return from + Mth.clamp(delta, -maxSpeed * dt, maxSpeed * dt);
    }

    private static Vec3 collide(BlockGetter level, Entity entity, Vec3 pivot, Vec3 wanted, double dt) {
        double allowed = collisionFraction(BattleCamera.seeThrough(level), entity, pivot, wanted);
        armFraction = allowed < armFraction ? allowed : damp(armFraction, allowed, dt, 0.18);
        return pivot.add(wanted.subtract(pivot).scale(armFraction));
    }

    private static double collisionFraction(BlockGetter collision, Entity entity, Vec3 pivot, Vec3 wanted) {
        Vec3 direction = wanted.subtract(pivot);
        double length = direction.length();
        if (length < 1.0e-6) return 1;
        double safe = length;
        // Probe the camera volume rather than one ray. Retract before applying the pose, never through walls.
        for (int i = 0; i < 8; i++) {
            Vec3 offset = new Vec3(((i & 1) * 2 - 1) * 0.12, ((i >> 1 & 1) * 2 - 1) * 0.12,
                    ((i >> 2 & 1) * 2 - 1) * 0.12);
            var hit = collision.clip(new ClipContext(pivot.add(offset), wanted.add(offset),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
            if (hit.getType() != HitResult.Type.MISS) safe = Math.min(safe, Math.max(0, hit.getLocation().distanceTo(pivot.add(offset)) - 0.18));
        }
        return safe / length;
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
