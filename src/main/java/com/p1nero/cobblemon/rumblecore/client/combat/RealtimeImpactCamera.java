package com.p1nero.cobblemon.rumblecore.client.combat;

import com.p1nero.cobblemon.rumblecore.RumbleConfig;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class RealtimeImpactCamera {
    private static ClientLevel level;
    private static LocalPlayer player;
    private static long start, phaseStart;
    private static float amplitude, duration;
    private static boolean cinematic;

    private RealtimeImpactCamera() {}

    public static void hit(float fraction, boolean received) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !Float.isFinite(fraction) || fraction <= 0) return;
        long now = Util.getMillis();
        float residual = level == mc.level && player == mc.player ? amplitude * envelope(now) : 0;
        // Keep small hits visible without scaling large hits linearly.
        float strength = (float) Math.sqrt(Mth.clamp(fraction, 0, 1));
        float wanted = RumbleConfig.impactShakeMin() + strength * RumbleConfig.impactShakeDamage();
        if (!received) wanted *= .72F;
        if (residual < .02F) phaseStart = now;
        amplitude = Math.min(RumbleConfig.impactShakeMax(), Math.max(wanted, residual) + .25F * Math.min(wanted, residual));
        duration = RumbleConfig.impactShakeDuration();
        level = mc.level;
        player = mc.player;
        start = now;
    }

    private static float envelope(long now) {
        if (duration <= 0) return 0;
        float t = Mth.clamp((now - start) / duration, 0, 1);
        return (1 - t) * (1 - t);
    }

    public static void cinematic(boolean active) {
        cinematic = active;
        if (active) amplitude = 0;
    }

    public static boolean cinematic() { return cinematic; }

    public record Offset(float yaw, float pitch, float roll) {}

    public static Offset offset(Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (cinematic || level == null || level != mc.level || player != mc.player || camera.getEntity() != player) return null;
        long now = Util.getMillis();
        float strength = amplitude * envelope(now);
        if (strength < .01F) return null;
        float phase = (now - phaseStart) * RumbleConfig.impactShakeFrequency();
        return new Offset(.70F * strength * Mth.cos(phase * .90F),
                strength * .60F * Mth.sin(phase * 1.10F),
                .40F * strength * Mth.sin(phase * 1.30F + 2F));
    }
}
