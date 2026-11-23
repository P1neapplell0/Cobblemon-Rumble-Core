package com.p1nero.cobblemon.rumblecore.mixin.cobbledollars;

import com.p1nero.cobblemon.rumblecore.prologue.TrialMerchantEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The villager overhaul otherwise opens its shop before the NPC's own interaction handler. */
@Mixin(targets = "nl.streats1.cobbledollarsvillagersoverhaul.integration.MerchantInteractCompat", remap = false)
abstract class TrialMerchantInteraction {
    @Inject(method = "shouldDeferShopOverride", at = @At("HEAD"), cancellable = true)
    private static void rumble$dialogueOnly(Entity entity, boolean sneaking, ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof TrialMerchantEntity) cir.setReturnValue(true);
    }
}
