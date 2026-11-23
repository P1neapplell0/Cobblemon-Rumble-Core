package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.client.combat.HallOfFameScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastComponent.class)
abstract class ChampionCeremonyToasts {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void rumble$waitForCeremony(GuiGraphics graphics, CallbackInfo ci) {
        if (((ToastComponent) (Object) this).getMinecraft().screen instanceof HallOfFameScreen screen && screen.isCeremony()) {
            ci.cancel();
        }
    }
}
