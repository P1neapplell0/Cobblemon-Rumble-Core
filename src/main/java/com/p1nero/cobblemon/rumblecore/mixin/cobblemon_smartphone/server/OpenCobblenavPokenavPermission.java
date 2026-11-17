package com.p1nero.cobblemon.rumblecore.mixin.cobblemon_smartphone.server;

import com.nbp.cobblemon_smartphone.network.handler.OpenCobblenavPokenavHandler;
import com.nbp.cobblemon_smartphone.network.packet.OpenCobblenavPokenavPacket;
import com.p1nero.cobblemon.rumblecore.phone.PhoneApp;
import com.p1nero.cobblemon.rumblecore.phone.PhoneState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OpenCobblenavPokenavHandler.class, remap = false)
abstract class OpenCobblenavPokenavPermission {
    @Inject(method = "handle(Lcom/nbp/cobblemon_smartphone/network/packet/OpenCobblenavPokenavPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"), cancellable = true)
    private void rumble$requireApp(OpenCobblenavPokenavPacket packet, MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
        if (!PhoneState.allow(player, PhoneApp.POKEMON_TRACKER)) ci.cancel();
    }
}
