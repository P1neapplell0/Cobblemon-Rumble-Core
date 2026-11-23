package com.p1nero.cobblemon.rumblecore.client.combat;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
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
        // A relative UI sound, so every selected listener hears it at the same volume.
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.35F, 0.9F));
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
