package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.prologue.client.PrologueClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastComponent.class)
abstract class PrologueTutorialHud {
    // Render world-anchored prompts and lesson cues in HUD space, on top of the scene and toasts.
    @Inject(method = "render", at = @At("RETURN"))
    private void rumble$renderTutorial(GuiGraphics graphics, CallbackInfo ci) {
        PrologueClient.renderOverlay(graphics);
    }
}
