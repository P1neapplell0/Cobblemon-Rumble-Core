package com.p1nero.cobblemon.rumblecore.client.combat;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

public final class RealtimeMoveWindupClient {
    private static LocalPlayer caster;
    private static ClientLevel level;
    private static long until;

    private RealtimeMoveWindupClient() {}

    public static void onWarning(int ticks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        // Keep the cue non-positional for reliable audibility, while distinguishing our cast from threats.
        SoundEvent sound = ticks > 0 ? SoundEvents.NOTE_BLOCK_PLING.value() : SoundEvents.AMETHYST_BLOCK_CHIME;
        float pitch = ticks > 0 ? 0.9F : 0.82F;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.1F, pitch));
        if (ticks > 0) {
            caster = mc.player;
            caster.setSprinting(false);
            caster.setDeltaMovement(Vec3.ZERO);
            level = mc.level;
            until = level.getGameTime() + ticks;
            KeyMapping.releaseAll();
        }
    }

    public static boolean active() {
        if (caster == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == caster && mc.level == level && level.getGameTime() < until) return true;
        caster = null;
        level = null;
        return false;
    }
}
