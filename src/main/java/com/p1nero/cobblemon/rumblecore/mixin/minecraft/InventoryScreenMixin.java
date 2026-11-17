package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import fr.harmex.cobbledollars.common.client.utils.Context;
import fr.harmex.cobbledollars.common.client.utils.GuiUtilsKt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.math.BigInteger;

/** 在玩家背包面板下方居中绘制 CobbleDollars 余额。 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    private static final int PANEL_GAP = 4;
    private static final int SCREEN_MARGIN = 4;

    @Inject(method = "render", at = @At("TAIL"))
    private void rumble$renderMoneyBelowInventory(
            GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        int x = screen.getGuiLeft() + (screen.getXSize() - GuiUtilsKt.TEXTURE_WIDTH) / 2;
        int preferredY = screen.getGuiTop() + screen.getYSize() + PANEL_GAP;
        int y = Math.min(preferredY,
                guiGraphics.guiHeight() - GuiUtilsKt.TEXTURE_HEIGHT - SCREEN_MARGIN);
        boolean animateUp = y + GuiUtilsKt.TEXTURE_HEIGHT + 12 > guiGraphics.guiHeight();

        GuiUtilsKt.renderCobbleDollarsElement(
                guiGraphics, x, y, true, BigInteger.ZERO, animateUp, Context.PLAYER, null);
    }
}
