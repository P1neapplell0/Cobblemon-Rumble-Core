package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.client.ClientSmash;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaKnockoutVisual;
import com.p1nero.cobblemon.rumblecore.client.combat.FrontierKnockoutOverlay;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientSmash.class, remap = false)
abstract class RealtimeKnockoutClient {
    @Inject(method = "onRenderOwnPokemon", at = @At("HEAD"), cancellable = true)
    private static void rumble$showFirstPersonKnockout(RenderLivingEvent.Pre<?, ?> event, CallbackInfo ci) {
        if (FrontierKnockoutOverlay.isFramed(event.getEntity())) ci.cancel();
    }

    @Inject(method = "snap", at = @At("HEAD"), cancellable = true)
    private static void rumble$followServerFlight(LivingEntity player, LivingEntity body, CallbackInfo ci) {
        if (body instanceof ArenaKnockoutVisual visual && visual.rumble$isKnockedOut()) ci.cancel();
    }
}
