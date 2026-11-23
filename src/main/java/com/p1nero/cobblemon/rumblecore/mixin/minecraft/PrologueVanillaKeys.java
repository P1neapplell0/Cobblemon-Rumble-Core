package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.prologue.client.PrologueClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class PrologueVanillaKeys {
    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void rumble$blockInventoryDuringRealtimeCombat(CallbackInfo ci) {
        if (!com.johnn.realtimecobblemon.client.ClientSmash.active()) return;
        var inventory = Minecraft.getInstance().options.keyInventory;
        while (inventory.consumeClick()) { }
        inventory.setDown(false);
    }

    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void rumble$focusGameplayKeys(CallbackInfo ci) {
        if (PrologueClient.restrict()) {
            PrologueClient.allowTutorialChat();
            ci.cancel();
        }
    }
}
