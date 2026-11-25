package com.p1nero.cobblemon.rumblecore.client.combat;

import com.johnn.realtimecobblemon.client.BattleCamera;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeBeamClashPacket;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RealtimeBeamClashCamera {
    private static final long FADE_IN = 280, FADE_OUT = 320, PACKET_TIMEOUT = 1400;
    private static Shot active;

    private static final class Shot {
        final ClientLevel level = Minecraft.getInstance().level;
        final RealtimeBeamClashPacket packet;
        final long starts = Util.getMillis();
        long heartbeat = starts, ends;
        double side;
        Vec3 center;
        Vec3 offset;
        long frame;
        BattleCamera.Pose last;

        Shot(RealtimeBeamClashPacket packet) { this.packet = packet; }
    }

    private RealtimeBeamClashCamera() {}

    public static void begin(RealtimeBeamClashPacket packet) { active = new Shot(packet); }

    public static void update(boolean fighting) {
        if (active == null) return;
        long now = Util.getMillis();
        if (fighting) active.heartbeat = now;
        else if (active.ends == 0) active.ends = now;
    }

    public static BattleCamera.Pose camera(Camera base, BlockGetter level, Entity view, float partial) {
        Shot shot = active;
        if (shot == null) return null;
        long now = Util.getMillis();
        if (shot.level == null || shot.level != level || shot.ends != 0 && now - shot.ends >= FADE_OUT) {
            active = null;
            return null;
        }
        var a = shot.level.getEntity(shot.packet.localBody());
        var b = shot.level.getEntity(shot.packet.opposingBody());
        if (shot.ends == 0 && (now - shot.heartbeat > PACKET_TIMEOUT || (a == null || b == null) && shot.last != null)) shot.ends = now;
        if (shot.ends == 0 && a != null && b != null) {
            Vec3 p = center(a, partial), q = center(b, partial);
            Vec3 line = q.subtract(p);
            Vec3 lateral = new Vec3(-line.z, 0, line.x).normalize();
            if (lateral.lengthSqr() < .01) return null;
            Vec3 midpoint = p.lerp(q, .5);
            if (shot.side == 0) shot.side = base.getPosition().subtract(midpoint).dot(lateral) >= 0 ? 1 : -1;
            var window = Minecraft.getInstance().getWindow();
            double aspect = (double) window.getWidth() / Math.max(1, window.getHeight());
            double distance = Math.max(3.4, (line.length() * .58 + Math.max(a.getBbWidth(), b.getBbWidth())) / Math.max(.45, aspect * .53));
            Vec3 offset = lateral.scale(distance * shot.side).add(0, Math.max(.8, distance * .20), 0);
            double alpha = shot.frame == 0 ? 1 : 1 - Math.exp(-Math.clamp(now - shot.frame, 0L, 50L) / 75.0);
            shot.frame = now;
            shot.center = shot.center == null ? midpoint : shot.center.lerp(midpoint, alpha);
            shot.offset = shot.offset == null ? offset : shot.offset.lerp(offset, alpha);
            Vec3 pos = shot.center.add(shot.offset);
            var hit = level.clip(new ClipContext(shot.center, pos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, view));
            if (hit.getType() != HitResult.Type.MISS) {
                double usable = Math.max(0, hit.getLocation().distanceTo(shot.center) - .22);
                if (usable < .8) return null;
                pos = shot.center.add(shot.offset.normalize().scale(usable));
            }
            Vec3 aim = shot.center.subtract(pos);
            float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
            float pitch = (float) -Math.toDegrees(Math.atan2(aim.y, aim.horizontalDistance()));
            shot.last = new BattleCamera.Pose(pos, yaw, pitch);
        }
        if (shot.last == null) return null;
        float in = Math.clamp((now - shot.starts) / (float) FADE_IN, 0F, 1F);
        float out = shot.ends == 0 ? 1 : Math.clamp(1 - (now - shot.ends) / (float) FADE_OUT, 0F, 1F);
        float t = Math.min(in, out);
        float mix = t * t * (3 - 2 * t);
        return new BattleCamera.Pose(base.getPosition().lerp(shot.last.pos(), mix),
                Mth.rotLerp(mix, base.getYRot(), shot.last.yaw()), Mth.rotLerp(mix, base.getXRot(), shot.last.pitch()));
    }

    private static Vec3 center(Entity e, float partial) {
        return e.getPosition(partial).add(0, e.getBbHeight() * .63, 0);
    }
}
