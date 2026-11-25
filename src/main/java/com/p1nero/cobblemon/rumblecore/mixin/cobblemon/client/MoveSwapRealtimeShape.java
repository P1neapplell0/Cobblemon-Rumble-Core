package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.client;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.client.gui.summary.widgets.screens.moves.MoveSwapScreen;
import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeMoveShapeTooltip;
import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeMoveShapeUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Limit the RTC name suffix to Cobblemon 1.8.4's move-swap candidate rows.
 * On Cobblemon upgrades, check the name draw call and the render hover parameter.
 */
@Mixin(value = MoveSwapScreen.MoveSlot.class, remap = false)
abstract class MoveSwapRealtimeShape extends ObjectSelectionList.Entry<MoveSwapScreen.MoveSlot> {
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/cobblemon/mod/common/api/moves/MoveTemplate;getDisplayName()Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent rumble$appendRealtimeShape(MoveTemplate move) {
        // The original call is only reached for populated rows.
        return RealtimeMoveShapeUi.displayNameWithShape(move);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void rumble$captureShapeTooltip(GuiGraphics graphics, int index,
                                            int rowTop, int rowLeft, int rowWidth, int rowHeight,
                                            int mouseX, int mouseY, boolean hovering, float partialTick,
                                            CallbackInfo ci) {
        if (hovering) {
            RealtimeMoveShapeTooltip.capture(
                    Minecraft.getInstance().screen,
                    ((MoveSwapScreen.MoveSlot) (Object) this).getMove(), mouseX, mouseY);
        }
    }
}
