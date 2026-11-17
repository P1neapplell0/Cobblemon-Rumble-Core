package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.client.BattleCamera;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.p1nero.cobblemon.rumblecore.client.combat.ReticleProjection;
import com.p1nero.cobblemon.rumblecore.client.combat.SubpixelReticleGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.johnn.realtimecobblemon.client.SmashHud", remap = false)
abstract class SubpixelBattleReticle {
    @WrapMethod(method = "crosshair")
    private static void rumble$fractionalCenter(GuiGraphics graphics, int width, int height, float time, Operation<Void> original) {
        boolean firstPerson = Minecraft.getInstance().options.getCameraType().isFirstPerson();
        graphics.pose().pushPose();
        try {
            if (!firstPerson) {
                // Upstream rounds to an entire GUI unit (several screen pixels at larger GUI scales).
                // Retain its shape and hit feedback, restoring the discarded fraction through the local pose.
                // The central square spans [cx,cx+1], so subtract 0.5 to center it on the projected point.
                var window = Minecraft.getInstance().getWindow();
                // GameRenderer's GUI projection uses these continuous extents, not ceil(guiWidth).
                // That distinction also prevents scale-dependent drift on non-divisible window sizes.
                double guiWidth = window.getWidth() / window.getGuiScale();
                double guiHeight = window.getHeight() / window.getGuiScale();
                double dx = guiWidth * ReticleProjection.x() - Math.round(width * BattleCamera.aimX()) - 0.5;
                double dy = guiHeight * ReticleProjection.y() - Math.round(height * BattleCamera.aimY()) - 0.5;
                graphics.pose().translate(dx, dy, 0);
            }
            original.call(graphics, width, height, time);
            graphics.flush();
        } finally {
            graphics.pose().popPose();
        }
    }

    @Redirect(method = "crosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private static void rumble$subpixelEdges(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        SubpixelReticleGraphics.fill(graphics, x1, y1, x2, y2, color);
    }
}
