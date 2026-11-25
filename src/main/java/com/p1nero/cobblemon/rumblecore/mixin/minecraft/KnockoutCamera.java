package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.client.combat.FrontierKnockoutOverlay;
import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeBeamClashCamera;
import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeImpactCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Apply the temporary pose after RTC's shoulder camera; the player's view and aim are not mutated.
@Mixin(Camera.class)
abstract class KnockoutCamera {
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Mixin(Camera.class)
    interface RotationAccess {
        @Invoker("setRotation")
        void rumble$setRenderRotation(float yaw, float pitch, float roll);
    }

    // Both mods inject at TAIL. Priority alone is not a guarantee of callback order.
    @Inject(method = "setup", at = @At("TAIL"), order = 2000)
    private void rumble$frameKnockout(BlockGetter level, Entity view, boolean thirdPerson, boolean reverse,
                                     float partialTick, CallbackInfo ci) {
        var pose = FrontierKnockoutOverlay.camera((Camera) (Object) this, level, partialTick);
        if (pose == null) pose = RealtimeBeamClashCamera.camera((Camera) (Object) this, level, view, partialTick);
        RealtimeImpactCamera.cinematic(pose != null);
        if (pose == null) return;
        detached = true;
        setPosition(pose.pos());
        setRotation(pose.yaw(), pose.pitch());
    }
}
