package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.starter.CobblemonStarterHandler;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueStory;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CobblemonStarterHandler.class, remap = false)
abstract class PrologueStarterGate {
    @Inject(method = {"handleJoin", "requestStarterChoice"}, at = @At("HEAD"), cancellable = true)
    private void rumble$gateScreen(ServerPlayer player, CallbackInfo ci) {
        if (!PrologueStory.starterAllowed(player)) ci.cancel();
    }

    @Inject(method = "chooseStarter", at = @At("HEAD"), cancellable = true)
    private void rumble$gateSelection(ServerPlayer player, String category, int slot, CallbackInfo ci) {
        if (!PrologueStory.starterAllowed(player)) ci.cancel();
    }
}
