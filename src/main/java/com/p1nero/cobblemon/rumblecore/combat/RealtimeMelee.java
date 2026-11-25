package com.p1nero.cobblemon.rumblecore.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.MoveCombat;
import com.johnn.realtimecobblemon.Smash;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaKnockoutVisual;
import com.p1nero.cobblemon.rumblecore.combat.reward.WildCapture;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RealtimeMelee {
    public static final double REACH = 2.0;
    public static final double ASSIST_SEARCH_REACH = 6.0;
    private static final double DASH_SEARCH_REACH = 8.0;
    private static final double ASSIST_STEP = 1.0;
    private static final double CONE = Math.cos(Math.toRadians(55));

    public record Aim(LivingEntity target, Vec3 direction, Vec3 step, double contactDistance) {}

    private RealtimeMelee() {}

    public static Aim aim(Smash.Session session, Vec3 look, double searchReach) {
        AABB body = session.pokemon.getBoundingBox();
        Vec3 preferred = RealtimeReleaseAim.direction(session, body.getCenter(), look);
        var lock = RealtimeReleaseAim.lockedTarget(session);
        var target = targets(session, body, preferred, searchReach).stream()
                .min(Comparator.<LivingEntity, Boolean>comparing(candidate -> candidate != lock)
                        .thenComparingDouble(candidate -> gap(body, candidate.getBoundingBox())
                                + (1 - horizontal(preferred).dot(horizontal(candidate.getBoundingBox().getCenter().subtract(body.getCenter())))) * 2))
                .orElse(null);
        if (target == null) return new Aim(null, look, Vec3.ZERO, 0);
        Vec3 direction = direction(session, target, look);
        // When aim assist acquired an unlocked target, rotate the actual controlled
        // Pokemon as well as the attack vector. Otherwise the hit lands toward the
        // target while the model keeps facing the player's cursor.
        if (lock == null && direction.horizontalDistanceSqr() > 1.0e-5) {
            faceControlledPokemon(session, direction);
        }
        // The acquisition window deliberately extends beyond reach + step: a pursuit can still whiff.
        double reach = effectiveReach(body, target.getBoundingBox());
        boolean outOfReach = gap(body, target.getBoundingBox()) > reach;
        double step = Math.min(ASSIST_STEP, Math.max(0, gap(body, target.getBoundingBox()) - reach * .72));
        Vec3 movement = outOfReach && session.player.onGround() ? horizontal(direction).scale(step) : Vec3.ZERO;
        if (movement.lengthSqr() > 0 && (!session.player.level().noCollision(session.pokemon, body.expandTowards(movement).deflate(.001))
                || !session.player.level().noCollision(session.player, session.player.getBoundingBox().expandTowards(movement).deflate(.001)))) {
            movement = Vec3.ZERO;
        }
        return new Aim(target, direction, movement, Math.max(0, gap(body, target.getBoundingBox()) - .18));
    }

    private static void faceControlledPokemon(Smash.Session session, Vec3 direction) {
        float yaw = (float) (Mth.atan2(-direction.x, direction.z) * (180.0 / Math.PI));
        var pokemon = session.pokemon;
        pokemon.setYRot(yaw);
        pokemon.setYHeadRot(yaw);
        pokemon.setYBodyRot(yaw);
        // Keep interpolation from showing a one-frame snap back to the old aim.
        pokemon.yRotO = yaw;
        pokemon.yHeadRotO = yaw;
        pokemon.yBodyRotO = yaw;
    }

    public static Vec3 direction(Smash.Session session, LivingEntity target, Vec3 fallback) {
        if (session == null || target == null) return fallback;
        Vec3 delta = target.getBoundingBox().getCenter().subtract(session.pokemon.getBoundingBox().getCenter());
        return delta.horizontalDistanceSqr() > 1e-5 ? delta.normalize() : fallback;
    }

    public static List<LivingEntity> meleeTargets(Smash.Session session, Aim aim) {
        // RTC lunges are client-simulated; evaluate the short collision-checked assist step before the motion packet arrives.
        var targets = targets(session, session.pokemon.getBoundingBox().move(aim.step()), aim.direction(), ASSIST_SEARCH_REACH);
        return targets.stream().filter(target -> clearAfterStep(session, target, aim.step())).sorted(Comparator.comparing(target -> target != aim.target())).limit(3).toList();
    }

    private static boolean clearAfterStep(Smash.Session session, LivingEntity target, Vec3 step) {
        if (step.lengthSqr() == 0) return true;
        return session.player.level().clip(new ClipContext(session.pokemon.getEyePosition().add(step),
                target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, session.pokemon))
                .getType() == HitResult.Type.MISS;
    }

    private static List<LivingEntity> targets(Smash.Session session, AABB body, Vec3 direction, double searchReach) {
        Vec3 forward = horizontal(direction);
        if (forward.lengthSqr() < 1e-5) forward = Vec3.directionFromRotation(0, session.player.getYRot());
        Vec3 facing = forward;
        // Search is deliberately generous. The final distance is calculated from both hitboxes below.
        return session.player.level().getEntitiesOfClass(LivingEntity.class,
                body.inflate(searchReach + 1.0, 2.25, searchReach + 1.0), target -> {
            if (!MoveCombat.canHit(session, target) || !target.isAlive()
                    || target instanceof PokemonEntity pokemon && WildCapture.stunned(pokemon)
                    || target instanceof ArenaKnockoutVisual visual && visual.rumble$isKnockedOut()
                    || !session.pokemon.hasLineOfSight(target)) return false;
            AABB box = target.getBoundingBox();
            double distance = threeDimensionalGap(body, box);
            double reach = effectiveReach(body, box);
            Vec3 delta = horizontal(box.getCenter().subtract(body.getCenter()));
            // At close range, side/behind/overhead targets are valid. Only distant targets need facing.
            return distance <= Math.min(searchReach, reach + ASSIST_STEP + 2.0)
                    && (distance <= reach * 1.35 || delta.lengthSqr() < 1e-5 || facing.dot(delta) >= -0.35);
        }).stream().sorted(Comparator.comparingDouble(target -> threeDimensionalGap(body, target.getBoundingBox()))).toList();
    }

    /**
     * Approximation of RTC's visible M1/heavy effect envelope: the effect starts at the
     * attacker's front, grows with the two hitboxes, and gets a generous 20 cm tolerance.
     * This is used only for server acquisition; actual MoveEngine hit filtering remains authoritative.
     */
    private static double effectiveReach(AABB attacker, AABB target) {
        double widths = (attacker.getXsize() + target.getXsize()) * .5;
        double heights = (attacker.getYsize() + target.getYsize()) * .18;
        return Math.max(1.25, 1.10 + widths + heights);
    }

    private static double threeDimensionalGap(AABB a, AABB b) {
        double x = Math.max(0, Math.max(a.minX - b.maxX, b.minX - a.maxX));
        double y = Math.max(0, Math.max(a.minY - b.maxY, b.minY - a.maxY));
        double z = Math.max(0, Math.max(a.minZ - b.maxZ, b.minZ - a.maxZ));
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static Vec3 guidedStep(Vec3 motion, Aim aim) {
        if (aim == null || aim.target() == null) return motion;
        Vec3 horizontal = motion.multiply(1, 0, 1);
        double maxContact = aim.contactDistance();
        if (horizontal.lengthSqr() > maxContact * maxContact && maxContact > 0) {
            horizontal = horizontal.normalize().scale(maxContact);
        }
        return aim.step().add(horizontal).add(0, motion.y, 0);
    }

    private static Vec3 horizontal(Vec3 direction) { return direction.multiply(1, 0, 1).normalize(); }

    private static double gap(AABB a, AABB b) {
        double x = Math.max(0, Math.max(a.minX - b.maxX, b.minX - a.maxX));
        double z = Math.max(0, Math.max(a.minZ - b.maxZ, b.minZ - a.maxZ));
        return Math.hypot(x, z);
    }
}
