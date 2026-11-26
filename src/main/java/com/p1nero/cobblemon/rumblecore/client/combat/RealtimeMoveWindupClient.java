package com.p1nero.cobblemon.rumblecore.client.combat;

import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindupPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class RealtimeMoveWindupClient {
    private static LocalPlayer caster;
    private static ClientLevel level;
    private static long until;

    private RealtimeMoveWindupClient() {}

    public static void onWarning(int ticks, boolean melee) {
        Minecraft mc = Minecraft.getInstance();
        if (ticks == RealtimeMoveWindupPacket.RELEASE) {
            finish();
            return;
        }
        if (mc.level == null || mc.player == null) return;
        boolean friendly = ticks > 0 || ticks == RealtimeMoveWindupPacket.ALLY;
        // Melee is higher-pitched than ranged; reserve the louder anvil cue for threats.
        // UI playback keeps both cues audible regardless of camera distance.
        float pitch = friendly ? (melee ? 1.75F : 1.30F) : (melee ? 1.35F : 1.00F);
        mc.getSoundManager().play(SimpleSoundInstance.forUI(
                friendly ? SoundEvents.NOTE_BLOCK_BELL.value() : SoundEvents.ANVIL_LAND,
                pitch, friendly ? 0.9F : 0.72F));
        if (ticks > 0) {
            caster = mc.player;
            level = mc.level;
            until = level.getGameTime() + ticks;
        }
    }

    public static boolean active() {
        if (caster == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == caster && mc.level == level && level.getGameTime() < until) return true;
        finish();
        return false;
    }

    private static void finish() {
        caster = null;
        level = null;
    }
}
