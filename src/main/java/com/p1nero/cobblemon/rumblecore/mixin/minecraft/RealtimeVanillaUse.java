package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.client.combat.BattleUseGuard;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class RealtimeVanillaUse {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void rumble$keepVanillaUseOutOfCombat(CallbackInfo ci) {
        // RTC polls the bound key separately to send Block packets.
        if (BattleUseGuard.blocksVanillaUse()) ci.cancel();
    }
}
