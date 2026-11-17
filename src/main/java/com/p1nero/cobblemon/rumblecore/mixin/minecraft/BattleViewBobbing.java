package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.johnn.realtimecobblemon.client.ClientSmash;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class BattleViewBobbing {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void rumble$stableCombatView(PoseStack pose, float partial, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        // Vanilla bob is a second view-matrix transform, independent of the third-person Camera pose.
        // Suppress it for RTC only; leave the user's normal walking camera option untouched.
        if (ClientSmash.active() && mc.getCameraEntity() == mc.player
                && !mc.options.getCameraType().isFirstPerson()) ci.cancel();
    }
}
