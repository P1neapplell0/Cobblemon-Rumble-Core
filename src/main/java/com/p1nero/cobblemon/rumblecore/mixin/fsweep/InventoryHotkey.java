package com.p1nero.cobblemon.rumblecore.mixin.fsweep;

import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.example.fsweep.client.ClientHandler", remap = false)
abstract class InventoryHotkey {
    @Inject(method = "onRegisterKeyMappings", at = @At("HEAD"), cancellable = true)
    private static void rumble$hideControlsEntry(RegisterKeyMappingsEvent event, CallbackInfo callback) {
        callback.cancel();
    }
}
