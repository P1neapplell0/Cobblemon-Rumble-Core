package com.p1nero.cobblemon.rumblecore.client.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.lang.ref.WeakReference;
import net.minecraft.Util;
import net.minecraft.util.Mth;

/** The server HP stays authoritative; these fractions only affect the width of the painted bars. */
public final class RealtimeHealthBar {
    public record Frame(float fill, float trail) {}
    // Only populated while the RTC tile is drawing, never during Cobblemon's turn-based HUD.
    public static Frame painting;

    private WeakReference<PokemonEntity> tracked = new WeakReference<>(null);
    private float target, from, trailFrom;
    private long changedAt;
    private boolean damage;

    public Frame sample(PokemonEntity pokemon, float health, float max) {
        float next = Mth.clamp(health / Math.max(1, max), 0, 1);
        long now = Util.getMillis();
        if (tracked.get() != pokemon) {
            tracked = new WeakReference<>(pokemon);
            from = target = trailFrom = next;
            changedAt = now;
            damage = false;
        } else if (next != target) {
            Frame old = frame(now);
            damage = next < target;
            from = old.fill();
            trailFrom = damage ? Math.max(old.trail(), old.fill()) : next;
            target = next;
            changedAt = now;
        }
        return frame(now);
    }

    public void clear() { tracked.clear(); }

    private Frame frame(long now) {
        long age = now - changedAt;
        float filled = Mth.lerp(ease(age / 130f), from, target);
        float trail = damage ? Mth.lerp(ease((age - 280) / 190f), trailFrom, target) : filled;
        return new Frame(filled, Math.max(filled, trail));
    }

    private static float ease(float progress) {
        float t = 1 - Mth.clamp(progress, 0, 1);
        return 1 - t * t * t;
    }
}
