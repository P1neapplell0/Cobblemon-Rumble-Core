package com.p1nero.cobblemon.rumblecore.mixin.cobbledollars;

import fr.harmex.cobbledollars.common.api.shop.Category;
import fr.harmex.cobbledollars.common.api.shop.Offer;
import fr.harmex.cobbledollars.common.client.gui.screen.widget.OfferListWidget;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores filtered offer data lost by CobbleDollars 2.0.0+Beta-6.1's search() implementation.
 * The upstream method creates filtered entries with the Kotlin default price (zero) and stock,
 * which makes ShopScreen's canBuy() calculate a zero price. Remove this compatibility mixin after
 * the upstream fix lands.
 */
@Mixin(value = OfferListWidget.class, remap = false)
public abstract class OfferListWidgetMixin {

    @Inject(method = "search", at = @At("TAIL"))
    private void rumble$restoreFilteredOfferData(String search, CallbackInfo ci) {
        OfferListWidget widget = (OfferListWidget) (Object) this;
        Category category = widget.getCategory();
        if (category == null) return;

        List<Offer> offers = category.getOffers();
        for (OfferListWidget.OfferEntry entry : widget.children()) {
            Integer offerIndex = entry.getOfferIndex();
            Integer ingredientIndex = entry.getIngredientIndex();
            if (offerIndex == null || ingredientIndex == null
                    || offerIndex < 0 || offerIndex >= offers.size()) {
                continue;
            }

            Offer offer = offers.get(offerIndex);
            if (ingredientIndex < 0 || ingredientIndex >= offer.getItems().size()) continue;
            entry.setOffer(offer);
            entry.setPrice(offer.getPrice());
            entry.setStock(offer.getStock());
        }
    }
}
