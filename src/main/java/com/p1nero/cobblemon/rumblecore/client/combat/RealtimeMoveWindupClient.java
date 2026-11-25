package com.p1nero.cobblemon.rumblecore.client.combat;

import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindupPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
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
        // Keep the cue non-positional for reliable audibility, while distinguishing our cast from threats.
        boolean ownCast = ticks > 0;
        SoundEvent sound = melee ? SoundEvents.IRON_GOLEM_ATTACK
                : (ownCast ? SoundEvents.NOTE_BLOCK_PLING.value() : SoundEvents.AMETHYST_BLOCK_CHIME);
        float pitch = melee ? (ownCast ? 1.65F : 1.15F) : 1.1F;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, ownCast ? 0.9F : 0.82F));
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
