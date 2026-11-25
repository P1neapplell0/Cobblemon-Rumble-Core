package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.client.ClashHud;
import com.johnn.realtimecobblemon.network.SmashPayloads;
import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeBeamClashCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClashHud.class, remap = false)
abstract class RealtimeBeamClashHud {
    @Inject(method = "on", at = @At("HEAD"))
    private static void rumble$trackBeamClash(SmashPayloads.ClashState state, CallbackInfo ci) {
        RealtimeBeamClashCamera.update(state.on());
    }

    // Replace RTC's generic “Mash!” in its own HUD, so the hint cannot outlive the clash.
    @Inject(method = "render", at = @At(value = "INVOKE", target =
            "Lcom/cobblemon/mod/common/client/render/RenderHelperKt;drawScaledText(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/chat/MutableComponent;Ljava/lang/Number;Ljava/lang/Number;FLjava/lang/Number;IIZZLjava/lang/Integer;Ljava/lang/Integer;)V"), cancellable = true)
    private static void rumble$drawClashInput(GuiGraphics g, float partialTick, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        var jump = mc.options.keyJump;
        var attack = mc.options.keyAttack;
        var hint = jump.isUnbound() && attack.isUnbound()
                ? Component.translatable("rumble.realtime.clash.hotbar")
                : jump.isUnbound() || attack.isUnbound() || jump.same(attack)
                    ? Component.translatable("rumble.realtime.clash.one", (jump.isUnbound() ? attack : jump).getTranslatedKeyMessage())
                    : Component.translatable("rumble.realtime.clash.two", jump.getTranslatedKeyMessage(), attack.getTranslatedKeyMessage());
        int cx = g.guiWidth() / 2;
        int y = Math.round(Math.max(52F, g.guiHeight() * 0.2F)) + 23;
        int half = (mc.font.width(hint) + 1) / 2 + 7;
        g.fill(cx - half, y - 3, cx + half, y + 12, 0xD8151924);
        g.fill(cx - half, y + 11, cx + half, y + 12, 0xFF78D6FA);
        g.drawCenteredString(mc.font, hint, cx, y, 0xFFF4FBFF);
        ci.cancel();
    }
}
