package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.client;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.client.gui.summary.widgets.screens.moves.MoveSlotWidget;
import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeMoveShapeTooltip;
import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeMoveShapeUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Cobblemon 1.8.4 renders equipped moves separately from the swap list.
 * On upgrades, check the name draw (the first scaled draw here is PP),
 * the space reserved for the reorder controls, and renderWidget's hover logic.
 */
@Mixin(value = MoveSlotWidget.class, remap = false)
abstract class MoveSlotWidgetRealtimeShape {
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lcom/cobblemon/mod/common/api/moves/Move;getDisplayName()Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent rumble$appendRealtimeShape(Move move) {
        return RealtimeMoveShapeUi.displayNameWithShape(move.getTemplate());
    }

    @ModifyArgs(method = "renderWidget", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lcom/cobblemon/mod/common/client/render/RenderHelperKt;drawScaledText$default(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/chat/MutableComponent;Ljava/lang/Number;Ljava/lang/Number;FLjava/lang/Number;IIZZLjava/lang/Integer;Ljava/lang/Integer;ILjava/lang/Object;)V"))
    private void rumble$fitMoveName(Args args) {
        // Name starts at x+28 in the 108px row. Leave 10px for controls.
        args.set(7, MoveSlotWidget.MOVE_WIDTH - 28 - 10);
        args.set(13, (int) args.get(13) & ~128); // Override Kotlin's default max width.
    }

    @Inject(method = "renderWidget", at = @At("TAIL"))
    private void rumble$captureShapeTooltip(GuiGraphics graphics, int mouseX, int mouseY,
                                            float partialTick, CallbackInfo ci) {
        MoveSlotWidget slot = (MoveSlotWidget) (Object) this;
        Move move = slot.getMove();
        if (move != null && slot.isHovered()) {
            RealtimeMoveShapeTooltip.capture(Minecraft.getInstance().screen, move.getTemplate(), mouseX, mouseY);
        }
    }
}
