package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.moves.Pops;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Pops.class, remap = false)
abstract class HideMoveNamePops {
    @Inject(method = "moveTag", at = @At("HEAD"), cancellable = true)
    private static void rumble$hideMoveName(Entity user, String name, String type, int ticks, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "word", at = @At("HEAD"), cancellable = true)
    private static void rumble$hideZMoveName(Entity entity, String text, int color, boolean big, CallbackInfo ci) {
        if (text.startsWith("zmove.realtimecobblemon.")) ci.cancel();
    }
}
