package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.client.ClientSmash;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeMoveKeys;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

final class RealtimeMoveBindings {
    private static final String HOTBAR = "Lnet/minecraft/client/Options;keyHotbarSlots:[Lnet/minecraft/client/KeyMapping;";

    @Mixin(value = ClientSmash.class, remap = false)
    static abstract class Input {
        @WrapOperation(method = "onTickPre", at = @At(value = "FIELD", target = HOTBAR), require = 2)
        private static KeyMapping[] rumble$moveInput(Options options, Operation<KeyMapping[]> original) {
            return RealtimeMoveKeys.SLOTS;
        }
    }

    @Mixin(targets = "com.johnn.realtimecobblemon.client.SmashHud", remap = false)
    static abstract class Hud {
        @WrapOperation(method = "moveTiles", at = @At(value = "FIELD", target = HOTBAR))
        private static KeyMapping[] rumble$moveKeyLabels(Options options, Operation<KeyMapping[]> original) {
            return RealtimeMoveKeys.SLOTS;
        }
    }
}
