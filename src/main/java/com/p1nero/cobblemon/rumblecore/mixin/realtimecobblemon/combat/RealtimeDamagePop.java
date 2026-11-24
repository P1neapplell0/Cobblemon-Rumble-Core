package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.moves.Pops;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeDamageFeedback;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Pops.class, remap = false)
abstract class RealtimeDamagePop {
    @Inject(method = "damage", at = @At("HEAD"))
    private static void rumble$noteNativeDamagePop(Entity target, float hp, float effectiveness, boolean crit, CallbackInfo ci) {
        RealtimeDamageFeedback.nativePop(target);
    }
}
