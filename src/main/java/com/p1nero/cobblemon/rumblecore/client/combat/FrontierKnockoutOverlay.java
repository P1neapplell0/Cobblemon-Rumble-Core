package com.p1nero.cobblemon.rumblecore.client.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.johnn.realtimecobblemon.client.BattleCamera;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.FrontierKnockout;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class FrontierKnockoutOverlay {
    private static final ResourceLocation ICON = PrologueWorld.id("textures/gui/arena_ko.png");
    private static final double ENTER_MS = 220;
    private static final double EXIT_MS = 340;
    private static FrontierKnockout.Cue active;
    private static ClientLevel cueLevel;
    private static long start;
    private static Shot shot;

    private static final class Shot {
        final BattleCamera.Pose entry;
        Vec3 side, anchor, center;
        FramingSubject victim, attacker;
        long lastFrame;
        double distance, obstruction;

        Shot(Camera base, Vec3 center, long now) {
            entry = new BattleCamera.Pose(base.getPosition(), base.getYRot(), base.getXRot());
            this.center = anchor = center;
            lastFrame = now;
        }
    }

    private record FramingSubject(Vec3 center, double radius) {
        static FramingSubject of(Entity entity, float partial) {
            double height = entity.getBbHeight();
            return new FramingSubject(RenderAimRay.renderPosition(entity, partial).add(0, height * .5, 0),
                    Math.max(height, entity.getBbWidth()) * .6);
        }
    }

    private FrontierKnockoutOverlay() {}

    public static void start(FrontierKnockout.Cue cue) {
        ArenaIntroOverlay.finishForKnockout(cue.dimension());
        active = cue;
        cueLevel = Minecraft.getInstance().level;
        start = Util.getMillis();
        shot = null;
        ArenaCinematicAudio.play("arena.knockout", .94f);
    }

    public static boolean inCue(Entity entity) {
        var cue = active;
        var level = Minecraft.getInstance().level;
        return entity != null && level != null && entity.level() == level && cue != null
                && cueIsLive(level, Util.getMillis()) && cue.actors().contains(entity.getId());
    }

    public static FrontierKnockout.Cue slowCue(Entity entity) {
        return inCue(entity) && Util.getMillis() - start < FrontierKnockout.SLOW_TICKS * 50L ? active : null;
    }

    private static boolean cueIsLive(ClientLevel level, long now) {
        return active != null && level != null && level == cueLevel
                && level.dimension().location().equals(active.dimension()) && now - start < active.ticks() * 50L;
    }

    public static Entity focus() {
        var level = Minecraft.getInstance().level;
        return cueIsLive(level, Util.getMillis()) ? level.getEntity(active.focus()) : null;
    }

    public static boolean isFramed(Entity entity) {
        var victim = focus();
        return victim != null && (entity == victim || entity.level() == victim.level() && entity.getId() == active.attacker());
    }

    public static BattleCamera.Pose camera(Camera base, BlockGetter level, float partial) {
        var mc = Minecraft.getInstance();
        long now = Util.getMillis();
        if (!cueIsLive(mc.level, now) || base.getEntity() != mc.getCameraEntity()) return null;
        Entity focus = mc.level.getEntity(active.focus());
        boolean choosingSide = shot == null;
        boolean hasVictim = focus != null && !focus.isRemoved();
        if (choosingSide && !hasVictim) return null;

        double age = (now - start) / (active.ticks() * 50.0);
        var victim = hasVictim ? FramingSubject.of(focus, partial) : shot.victim;
        Entity attacker = mc.level.getEntity(active.attacker());
        var winner = attacker != null && attacker.getId() != active.focus() && !attacker.isRemoved()
                ? FramingSubject.of(attacker, partial) : choosingSide ? null : shot.attacker;
        // Keep both subjects in the shot, giving the knockback slightly more room as it progresses.
        Vec3 target = winner == null ? victim.center() : winner.center().lerp(victim.center(), .5 + .08 * age);
        if (choosingSide) {
            shot = new Shot(base, target, now);
            Vec3 fromBase = base.getPosition().subtract(target).multiply(1, 0, 1).normalize();
            Vec3 flight = winner == null ? focus.getDeltaMovement() : victim.center().subtract(winner.center());
            flight = flight.multiply(1, 0, 1).normalize();
            if (flight.lengthSqr() > 1e-6) {
                Vec3 side = new Vec3(-flight.z, 0, flight.x);
                shot.side = (side.dot(fromBase) < 0 ? side.reverse() : side)
                        .subtract(flight.scale(winner == null ? .24 : .12)).normalize();
            } else shot.side = fromBase.lengthSqr() > 1e-6 ? fromBase
                    : Vec3.directionFromRotation(0, base.getYRot() + 35).reverse();
        }
        shot.victim = victim;
        shot.attacker = winner;
        double dt = Math.min(100, now - shot.lastFrame);
        shot.lastFrame = now;
        // Slow-motion physics alternates movement and holds. Two light filters smooth the change in speed too.
        double follow = damping(dt, 55);
        shot.anchor = shot.anchor.lerp(target, follow);
        Vec3 center = shot.center = shot.center.lerp(shot.anchor, follow);

        double aspect = mc.getWindow().getWidth() / (double) Math.max(1, mc.getWindow().getHeight());
        double verticalSlope = .70 * Math.tan(Math.toRadians(mc.options.fov().get() * .5));
        double horizontalSlope = verticalSlope * Math.max(.25, aspect);
        Vec3 outward = shot.side.add(0, .20, 0).normalize();
        double wanted = framingDistance(victim, winner, center, outward, horizontalSlope, verticalSlope);
        Entity clipSource = hasVictim ? focus : base.getEntity();
        if (choosingSide) {
            double clear = clearDistance(level, clipSource, center, outward, wanted);
            if (clear < wanted) {
                Vec3 reverse = shot.side.reverse().add(0, .20, 0).normalize();
                double reverseWanted = framingDistance(victim, winner, center, reverse, horizontalSlope, verticalSlope);
                double reverseClear = clearDistance(level, clipSource, center, reverse, reverseWanted);
                if (reverseClear == reverseWanted || reverseClear > clear + .75) {
                    shot.side = shot.side.reverse();
                    outward = reverse;
                    wanted = reverseWanted;
                }
            }
            shot.distance = wanted;
        } else {
            // Back away promptly as they separate, but don't zoom back in at every pause in the flight.
            shot.distance = Mth.lerp(damping(dt, wanted > shot.distance ? 65 : 175), shot.distance, wanted);
        }
        double obstruction = shot.distance - clearDistance(level, clipSource, center, outward, shot.distance);
        // Retract immediately at walls, then ease back out when a wall edge clears the shot.
        shot.obstruction = Math.max(obstruction, shot.obstruction * Math.exp(-dt / 130));
        Vec3 desired = center.add(outward.scale(Math.max(.08, shot.distance - shot.obstruction)));

        float mix = shotWeight(now);
        var origin = now - start < ENTER_MS ? shot.entry : new BattleCamera.Pose(base.getPosition(), base.getYRot(), base.getXRot());
        Vec3 look = center.subtract(desired);
        float yaw = (float) (Math.atan2(-look.x, look.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (-Math.atan2(look.y, look.horizontalDistance()) * Mth.RAD_TO_DEG);
        return new BattleCamera.Pose(origin.pos().lerp(desired, mix), Mth.rotLerp(mix, origin.yaw(), yaw),
                Mth.lerp(mix, origin.pitch(), pitch));
    }

    public static float battleShakeScale() {
        long now = Util.getMillis();
        return shot != null && cueIsLive(Minecraft.getInstance().level, now) ? 1 - .9f * shotWeight(now) : 1;
    }

    private static float shotWeight(long now) {
        double elapsed = now - start;
        double t = Mth.clamp(Math.min(elapsed / ENTER_MS, (active.ticks() * 50.0 - elapsed) / EXIT_MS), 0, 1);
        return (float) (t * t * t * (t * (t * 6 - 15) + 10));
    }

    private static double damping(double dt, double timeConstant) {
        return -Math.expm1(-dt / timeConstant);
    }

    private static double clearDistance(BlockGetter level, Entity source, Vec3 center, Vec3 outward, double distance) {
        var hit = level.clip(new ClipContext(center, center.add(outward.scale(distance)), ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, source));
        return hit.getType() == HitResult.Type.MISS ? distance : Math.max(.08, hit.getLocation().distanceTo(center) - .18);
    }

    private static double framingDistance(FramingSubject victim, FramingSubject attacker, Vec3 center, Vec3 outward,
                                          double horizontalSlope, double verticalSlope) {
        Vec3 right = new Vec3(-outward.z, 0, outward.x).normalize();
        Vec3 up = outward.cross(right).normalize();
        double distance = Math.max(2.7, framingDistance(victim, center, outward, right, up, horizontalSlope, verticalSlope));
        return attacker == null ? distance : Math.max(distance,
                framingDistance(attacker, center, outward, right, up, horizontalSlope, verticalSlope));
    }

    private static double framingDistance(FramingSubject subject, Vec3 center, Vec3 outward, Vec3 right, Vec3 up,
                                          double horizontalSlope, double verticalSlope) {
        Vec3 offset = subject.center().subtract(center);
        return offset.dot(outward) + subject.radius() + Math.max(
                (Math.abs(offset.dot(right)) + subject.radius()) / horizontalSlope,
                (Math.abs(offset.dot(up)) + subject.radius()) / verticalSlope);
    }

    private static void clear() {
        active = null;
        cueLevel = null;
        shot = null;
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    @SubscribeEvent public static void hideHand(RenderHandEvent event) {
        if (focus() != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void draw(RenderGuiEvent.Post event) {
        if (active == null) return;
        var level = Minecraft.getInstance().level;
        long now = Util.getMillis();
        if (!cueIsLive(level, now)) {
            clear();
            return;
        }
        float age = (now - start) / (50f * active.ticks());
        var g = event.getGuiGraphics();
        int w = (int) (Math.min(244, g.guiWidth() * 0.47f) * (1 + .16f * Math.max(0, 1 - age * 9)));
        int h = w * 261 / 640;
        int alpha = Math.round(255 * Mth.clamp((1 - age) * 5, 0, 1));
        g.flush();
        RenderSystem.enableBlend();
        g.setColor(1, 1, 1, alpha / 255f);
        g.blit(ICON, (g.guiWidth() - w) / 2, Math.max(6, g.guiHeight() / 5 - h / 2), w, h, 0, 0, 640, 261, 640, 261);
        g.flush();
        g.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }
}
