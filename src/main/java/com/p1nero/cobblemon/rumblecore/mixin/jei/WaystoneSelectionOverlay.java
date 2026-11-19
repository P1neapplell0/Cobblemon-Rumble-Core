package com.p1nero.cobblemon.rumblecore.mixin.jei;

import java.util.Optional;
import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A destination picker has no item slots; omit it from JEI's container overlay layout. */
@Mixin(targets = "mezz.jei.library.gui.helpers.ScreenHelper", remap = false)
abstract class WaystoneSelectionOverlay {
    @Inject(method = "getGuiProperties", at = @At("HEAD"), cancellable = true)
    private void rumble$hideOverlay(Screen screen, CallbackInfoReturnable<Optional<?>> cir) {
        if (screen instanceof WaystoneSelectionScreenBase) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
