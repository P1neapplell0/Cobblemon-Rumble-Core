package com.p1nero.cobblemon.rumblecore.mixin.jade;

import com.p1nero.cobblemon.rumblecore.prologue.client.PrologueClient;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "snownee.jade.util.ClientProxy", remap = false)
abstract class PrologueTooltip {
    @Inject(method = "onRenderTick", at = @At("HEAD"), cancellable = true)
    private static void rumble$avoidDuplicatePrompt(GuiGraphics graphics, float delta, CallbackInfo ci) {
        if (PrologueClient.hideInspectionHud()) ci.cancel();
    }
}
