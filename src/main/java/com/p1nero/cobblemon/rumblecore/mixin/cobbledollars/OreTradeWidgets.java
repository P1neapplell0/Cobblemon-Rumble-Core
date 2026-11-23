package com.p1nero.cobblemon.rumblecore.mixin.cobbledollars;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.NativePanels;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import fr.harmex.cobbledollars.common.client.gui.screen.widget.BankButton;
import fr.harmex.cobbledollars.common.client.gui.screen.widget.BuyButton;
import fr.harmex.cobbledollars.common.client.gui.screen.widget.CategoryListWidget;
import fr.harmex.cobbledollars.common.client.gui.screen.widget.OfferListWidget;
import fr.harmex.cobbledollars.common.client.gui.screen.widget.SellButton;
import fr.harmex.cobbledollars.common.client.gui.screen.widget.ShopButton;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

final class OreTradeWidgets {
    @Mixin(value = CategoryListWidget.class, remap = false)
    static abstract class Search {
        @ModifyExpressionValue(method = "search", at = @At(value = "INVOKE", target = "Lfr/harmex/cobbledollars/common/api/shop/Category;getName()Ljava/lang/String;"))
        private String rumble$localizedName(String name) { return NativePanels.categoryLabel(name).getString(); }
    }

    @Mixin(value = {BuyButton.class, SellButton.class, BankButton.class, ShopButton.class}, remap = false)
    static abstract class Actions {
        @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
        private void rumble$render(GuiGraphics g, int mx, int my, float tick, CallbackInfo ci) {
            Button self = (Button) (Object) this;
            boolean primary = self instanceof BuyButton || self instanceof SellButton;
            boolean enabled = self.active && (!(self instanceof BuyButton buy) || buy.getShopScreen().canBuy())
                    && (!(self instanceof SellButton sell) || sell.getBankScreen().canSell());
            boolean sell = self instanceof SellButton || self instanceof BankButton;
            OreTheme.button(g, self, Component.translatable("rumble.bank_prices." + (sell ? "sell" : "buy")), primary, enabled);
            ci.cancel();
        }
    }

    @Mixin(value = CategoryListWidget.CategoryEntry.class, remap = false)
    static abstract class Categories {
        @Shadow @Final CategoryListWidget this$0;

        @Inject(method = "renderBack", at = @At("HEAD"), cancellable = true)
        private void rumble$row(GuiGraphics g, int index, int top, int left, int w, int h, int mx, int my, boolean hover, float tick, CallbackInfo ci) {
            OreTheme.row(g, left - 1, top - 1, w, h + 1, this$0.getSelected() == (Object) this, hover);
            ci.cancel();
        }

        @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)I"))
        private int rumble$label(GuiGraphics g, Font font, String name, int x, int y, int color) {
            var label = NativePanels.categoryLabel(name);
            float scale = Math.min(1f, 60f / Math.max(1, font.width(label)));
            g.pose().pushPose();
            g.pose().translate(x + 1, y + (1 - scale) * 4, 0);
            g.pose().scale(scale, scale, 1);
            int end = g.drawString(font, label, 0, 0, OreTheme.TEXT, false);
            g.pose().popPose();
            return end;
        }

        @ModifyArg(method = "getNarration()Lnet/minecraft/network/chat/MutableComponent;", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
        private String rumble$narration(String name) {
            return NativePanels.categoryLabel(name).getString();
        }
    }

    @Mixin(value = OfferListWidget.OfferEntry.class, remap = false)
    static abstract class Offers {
        @Shadow @Final OfferListWidget this$0;

        @Inject(method = "renderBack", at = @At("HEAD"), cancellable = true)
        private void rumble$row(GuiGraphics g, int index, int top, int left, int w, int h, int mx, int my, boolean hover, float tick, CallbackInfo ci) {
            OreTheme.row(g, left, top, w, h + 1, this$0.getSelected() == (Object) this, hover);
            ci.cancel();
        }
    }
}
