package com.p1nero.cobblemon.rumblecore.mixin.quark;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides this mod's menu shortcut without disabling its configuration or other widgets. */
@Mixin(targets = "org.violetmoon.quark.base.client.config.QButtonHandler", remap = false)
abstract class HideConfigButton {
    @Inject(method = "onGuiInit", at = @At("HEAD"), cancellable = true)
    private static void rumble$hideShortcut(CallbackInfo ci) {
        ci.cancel();
    }
}
