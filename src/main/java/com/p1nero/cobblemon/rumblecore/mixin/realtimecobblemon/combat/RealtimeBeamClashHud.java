package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.client.ClashHud;
import com.johnn.realtimecobblemon.network.SmashPayloads;
import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeBeamClashCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClashHud.class, remap = false)
abstract class RealtimeBeamClashHud {
    @Inject(method = "on", at = @At("HEAD"))
    private static void rumble$trackBeamClash(SmashPayloads.ClashState state, CallbackInfo ci) {
        RealtimeBeamClashCamera.update(state.on());
    }
}
