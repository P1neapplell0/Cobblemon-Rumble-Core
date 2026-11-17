package com.p1nero.cobblemon.rumblecore.mixin.cobbledollars;

import com.p1nero.cobblemon.rumblecore.client.bank.BankPriceCatalogue;
import fr.harmex.cobbledollars.common.client.gui.screen.BankScreen;
import fr.harmex.cobbledollars.common.world.inventory.BankMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a catalogue button to the bottom edge of the CobbleDollars bank. */
@Mixin(value = BankScreen.class, remap = false)
public abstract class BankScreenMixin extends AbstractContainerScreen<BankMenu> {

    protected BankScreenMixin(BankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void rumble$addPriceCatalogueButton(CallbackInfo ci) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        addRenderableWidget(Button.builder(
                        Component.translatable("rumble.bank_prices.button"),
                        button -> minecraft.setScreen(new BankPriceCatalogue(
                                (BankScreen) (Object) this, menu.getBankRef().getBank())))
                .bounds(x + 112, y + 189, 53, 14)
                .build());
    }
}
