package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierSpectators;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.instance.InstanceSession;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InstanceManager.class, remap = false)
abstract class FrontierSpectatorEntry {
    @Inject(method = "permitsEntry", at = @At("HEAD"), cancellable = true)
    private void rumble$permitSpectatorTransfer(ServerPlayer player, ServerLevel destination, CallbackInfoReturnable<Boolean> cir) {
        if (FrontierSpectators.permitsEntry(player, destination)) cir.setReturnValue(true);
    }

    @Inject(method = "isTransferring", at = @At("HEAD"), cancellable = true)
    private void rumble$flagSpectatorTransfer(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (FrontierSpectators.isEntering(player)) cir.setReturnValue(true);
    }

    // Both of the dependency's non-member sweeps use this method. Do not give observers membership,
    // otherwise the dependency would enforce the fighter game mode or count them toward capacity.
    @Inject(method = "eject", at = @At("HEAD"), cancellable = true)
    private void rumble$keepArenaSpectator(ServerPlayer player, InstanceSession session, CallbackInfo ci) {
        if (FrontierSpectators.isWatching(player, session)) ci.cancel();
    }
}
