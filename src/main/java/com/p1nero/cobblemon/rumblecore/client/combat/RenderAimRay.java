package com.p1nero.cobblemon.rumblecore.client.combat;

import java.util.function.Predicate;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Visual-only picking; server hit detection and block occlusion remain unchanged. */
public final class RenderAimRay {
    private RenderAimRay() {}

    public static AABB renderBounds(Entity entity, float partial) {
        return entity.getBoundingBox().move(renderPosition(entity, partial).subtract(entity.position()));
    }

    /** LevelRenderer draws entities from xOld/yOld/zOld, which need not match the physics xo/yo/zo. */
    public static Vec3 renderPosition(Entity entity, float partial) {
        return new Vec3(Mth.lerp((double) partial, entity.xOld, entity.getX()),
                Mth.lerp((double) partial, entity.yOld, entity.getY()),
                Mth.lerp((double) partial, entity.zOld, entity.getZ()));
    }

    public static @Nullable EntityHitResult pick(Entity viewer, Vec3 eye, Vec3 end, AABB search,
            Predicate<Entity> canHit, double rangeSquared, float partial) {
        double closest = rangeSquared;
        Entity target = null;
        Vec3 hit = null;
        // Broad phase uses tick positions; leave extra room for the otherwise identical render-space ray.
        for (Entity candidate : viewer.level().getEntities(viewer, search.inflate(1.0), canHit)) {
            AABB box = renderBounds(candidate, partial).inflate(candidate.getPickRadius());
            if (box.contains(eye)) {
                target = candidate;
                hit = eye;
                closest = 0;
                continue;
            }
            if (closest == 0 || candidate.getRootVehicle() == viewer.getRootVehicle()) continue;
            Vec3 point = box.clip(eye, end).orElse(null);
            if (point == null) continue;
            double distance = eye.distanceToSqr(point);
            if (distance < closest) {
                target = candidate;
                hit = point;
                closest = distance;
            }
        }
        return target == null ? null : new EntityHitResult(target, hit);
    }
}
