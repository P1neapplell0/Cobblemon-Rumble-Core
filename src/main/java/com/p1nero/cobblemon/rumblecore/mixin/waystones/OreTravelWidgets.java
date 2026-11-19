package com.p1nero.cobblemon.rumblecore.mixin.waystones;

import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import net.blay09.mods.waystones.client.gui.widget.AbstractWaystoneButton;
import net.blay09.mods.waystones.client.gui.widget.ManageWaystonesButton;
import net.blay09.mods.waystones.client.gui.widget.SortWaystonesButton;
import net.blay09.mods.waystones.client.gui.widget.WaystoneGroupFilterButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Skin the native button face; subclass requirement, distance and group icons still render. */
@Mixin(AbstractButton.class)
abstract class OreTravelWidgets {
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void rumble$buttons(GuiGraphics g, int x, int y, float partialTick, CallbackInfo ci) {
        var button = (AbstractButton) (Object) this;
        Component label;
        if (button instanceof AbstractWaystoneButton) label = button.getMessage();
        else if (button instanceof ManageWaystonesButton || button instanceof SortWaystonesButton || button instanceof WaystoneGroupFilterButton) label = null;
        else return;
        OreTheme.button(g, button, label, false, button.active);
        ci.cancel();
    }
}
