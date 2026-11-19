package com.p1nero.cobblemon.rumblecore.mixin.cobbledollars;

import com.p1nero.cobblemon.rumblecore.client.ui.ore.NativePanels;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.RecyclingScreen;
import com.p1nero.cobblemon.rumblecore.prologue.ProfessorEntity;
import fr.harmex.cobbledollars.common.client.gui.screen.BankScreen;
import fr.harmex.cobbledollars.common.world.inventory.BankMenu;
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
        addRenderableWidget(OreTheme.action(x + 112, y + 188, 53, 14,
                Component.translatable("rumble.bank_prices.button"), false, button -> {
                    if (!menu.getCarried().isEmpty() || minecraft.player == null) return;
                    boolean professor = menu.getHasMerchant() && !minecraft.player.level().getEntitiesOfClass(ProfessorEntity.class,
                            minecraft.player.getBoundingBox().inflate(6), doctor -> doctor.getUUID().equals(menu.getCobbleMerchant().getMerchantUUID())).isEmpty();
                    minecraft.setScreen(new RecyclingScreen((BankScreen) (Object) this, professor));
                }));
    }

    @Redirect(method = "renderBg", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
    private void rumble$panel(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v, int w, int h, int tw, int th) {
        NativePanels.bank(g, font, x, y, w, h, menu.slots);
    }
}
