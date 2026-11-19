package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
abstract class InfiniteDurability {
    @Inject(method = "isDamageableItem", at = @At("HEAD"), cancellable = true)
    private void rumble$infiniteDurability(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
