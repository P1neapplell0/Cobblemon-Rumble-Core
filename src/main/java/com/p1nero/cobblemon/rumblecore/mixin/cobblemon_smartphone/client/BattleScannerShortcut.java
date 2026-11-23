package com.p1nero.cobblemon.rumblecore.mixin.cobblemon_smartphone.client;

import com.nbp.neoforge.keybind.NeoForgeSmartphoneKeybindHandler;
import com.p1nero.cobblemon.rumblecore.client.combat.BattleUseGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NeoForgeSmartphoneKeybindHandler.class, remap = false)
abstract class BattleScannerShortcut {
    @Inject(method = "handleScannerKeybind", at = @At("HEAD"), cancellable = true)
    private void rumble$ignoreScannerDuringBattle(CallbackInfo ci) {
        if (BattleUseGuard.blocksVanillaUse()) ci.cancel();
    }
}
