package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeMoveWindupClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class RealtimeMoveWindupKeys {
    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void rumble$lockKeybinds(CallbackInfo ci) {
        if (RealtimeMoveWindupClient.active()) {
            var options = ((Minecraft) (Object) this).options;
            // Only drain queued clicks. KeyboardInput still reads the held movement keys normally.
            for (KeyMapping key : options.keyMappings) {
                if (key == options.keyUp || key == options.keyDown || key == options.keyLeft || key == options.keyRight
                        || key == options.keyJump || key == options.keyShift || key == options.keySprint) continue;
                while (key.consumeClick()) { }
            }
            ci.cancel();
        }
    }
}
