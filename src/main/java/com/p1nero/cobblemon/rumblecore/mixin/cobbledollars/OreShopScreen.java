package com.p1nero.cobblemon.rumblecore.mixin.cobbledollars;

import com.p1nero.cobblemon.rumblecore.client.ui.ore.NativePanels;
import fr.harmex.cobbledollars.common.client.gui.screen.ShopScreen;
import fr.harmex.cobbledollars.common.world.inventory.ShopMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShopScreen.class, remap = false)
abstract class OreShopScreen extends AbstractContainerScreen<ShopMenu> {
    protected OreShopScreen(ShopMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }

    @Redirect(method = "renderBg", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
    private void rumble$panel(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v, int w, int h, int tw, int th) {
        NativePanels.shop(g, font, x, y, w, h, menu.slots);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void rumble$searchHints(CallbackInfo ci) {
        var shop = (ShopScreen) (Object) this;
        shop.getSearchCategoryBox().setHint(Component.translatable("rumble.bank_prices.categories"));
        shop.getSearchOfferBox().setHint(Component.translatable("rumble.bank_prices.offers"));
        shop.getBuyAmountBox().setHint(Component.translatable("rumble.bank_prices.qty"));
    }
}
