package com.p1nero.cobblemon.rumblecore.client.combat;

import com.johnn.realtimecobblemon.client.BattleCamera;
import com.johnn.realtimecobblemon.client.ClientSmash;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/** Mouse heading, independent of the heading RTC sends to the server for attacks. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class BattleOrbit {
    private static LocalPlayer owner;
    private static float renderedYaw, renderedPitch, freePitch;
    private static boolean locked, hasRendered;
    private static float pan, tilt, switchIntent;

    private BattleOrbit() {}

    public static void reset() {
        owner = null;
        locked = hasRendered = false;
        pan = tilt = switchIntent = 0;
    }

    public static void turn(LocalPlayer player, double dx, double dy) {
        if (ClientSmash.active() && BattleCamera.target() != null) {
            updateLock(player, true);
            float x = (float) dx * .15F;
            pan = Mth.clamp(pan + x, -22, 22);
            tilt = Mth.clamp(tilt + (float) dy * .15F, -10, 15);
            // A short, deliberate mouse sweep can request a new lock without dragging the lens off target.
            switchIntent = Mth.clamp(switchIntent + x, -105, 105);
        } else {
            updateLock(player, false);
            player.turn(dx, dy);
        }
    }

    public static void updateLock(LocalPlayer player, boolean lock) {
        if (owner != player || !ClientSmash.active()) {
            reset();
            if (!ClientSmash.active()) return;
            owner = player;
        }
        if (locked && !lock && hasRendered) {
            player.setYRot(renderedYaw);
            player.setXRot(Mth.clamp(freePitch, -90, 90));
            player.setYHeadRot(renderedYaw);
            player.yRotO = player.getYRot();
            player.xRotO = player.getXRot();
        }
        if (locked != lock) pan = tilt = switchIntent = 0;
        locked = lock;
    }

    public static void rendered(Entity entity, float yaw, float pitch, float freeTilt) {
        if (!(entity instanceof LocalPlayer player)) return;
        if (owner != player) {
            reset();
            owner = player;
        }
        renderedYaw = yaw;
        renderedPitch = pitch;
        freePitch = pitch - freeTilt;
        hasRendered = true;
    }

    public static float yaw(Entity entity) { return entity.getYRot(); }
    public static float pitch(Entity entity) { return entity.getXRot(); }
    public static float movementYaw(LocalPlayer player) {
        return owner == player && locked && hasRendered ? renderedYaw : player.getYRot();
    }
    public static float pan() { return pan; }
    public static float tilt() { return tilt; }
    public static boolean wantsSwitch() { return Math.abs(switchIntent) >= 48; }
    public static void tickIntent() { switchIntent *= .80F; }
    public static void switched() { pan = tilt = switchIntent = 0; }

    public static double angleTo(LocalPlayer player, Entity entity) {
        return angleTo(player, entity, false);
    }

    public static double angleTo(LocalPlayer player, Entity entity, boolean sweep) {
        Vec3 toward = entity.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (toward.lengthSqr() < .01) return 0;
        float viewYaw = movementYaw(player) + (sweep ? switchIntent : 0);
        float viewPitch = owner == player && locked && hasRendered ? renderedPitch : player.getXRot();
        return Math.toDegrees(Math.acos(Mth.clamp(Vec3.directionFromRotation(viewPitch, viewYaw)
                .dot(toward.normalize()), -1, 1)));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void movement(MovementInputUpdateEvent event) {
        if (!ClientSmash.active() || !(event.getEntity() instanceof LocalPlayer player)
                || player != Minecraft.getInstance().player) return;
        var input = event.getInput();
        double angle = Math.toRadians(Mth.wrapDegrees(movementYaw(player) - player.getYRot()));
        float left = input.leftImpulse, forward = input.forwardImpulse;
        float magnitude = Math.max(Math.abs(left), Math.abs(forward));
        double length = Math.hypot(left, forward);
        if (length < 1.0e-6) return;
        // Keep consistent speed for diagonals and rotate into RTC's current facing basis.
        double cos = Math.cos(angle) * magnitude / length, sin = Math.sin(angle) * magnitude / length;
        input.leftImpulse = (float) (left * cos - forward * sin);
        input.forwardImpulse = (float) (forward * cos + left * sin);
    }
}
