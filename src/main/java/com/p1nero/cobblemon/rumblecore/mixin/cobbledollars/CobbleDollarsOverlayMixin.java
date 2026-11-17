package com.p1nero.cobblemon.rumblecore.mixin.cobbledollars;

import fr.harmex.cobbledollars.common.client.gui.CobbleDollarsOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 关闭 CobbleDollars 的常驻 HUD；余额改由玩家背包界面绘制。 */
@Mixin(value = CobbleDollarsOverlay.class, remap = false)
public abstract class CobbleDollarsOverlayMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void rumble$hidePersistentMoneyOverlay(
            GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ci.cancel();
    }
}
