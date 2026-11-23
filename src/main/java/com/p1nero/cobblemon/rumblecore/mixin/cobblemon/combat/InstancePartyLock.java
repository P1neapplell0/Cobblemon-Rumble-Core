package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.api.net.NetworkPacket;
import com.p1nero.misc.custom_instances.api.InstanceApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lock player-initiated storage edits at the packet boundary. Capture still writes through the native storage API. */
@Mixin(targets = {
        "com.cobblemon.mod.common.net.serverhandling.storage.party.MovePartyPokemonHandler",
        "com.cobblemon.mod.common.net.serverhandling.storage.party.SwapPartyPokemonHandler",
        "com.cobblemon.mod.common.net.serverhandling.storage.pc.ReleasePartyPokemonHandler",
        "com.cobblemon.mod.common.net.serverhandling.storage.pc.MovePartyPokemonToPCHandler",
        "com.cobblemon.mod.common.net.serverhandling.storage.pc.MovePCPokemonToPartyHandler",
        "com.cobblemon.mod.common.net.serverhandling.storage.SwapPCPartyPokemonHandler",
        "com.cobblemon.mod.common.net.serverhandling.storage.RequestMoveSwapHandler",
        "com.cobblemon.mod.common.net.serverhandling.storage.BenchMoveHandler",
        "com.cobblemon.mod.common.net.serverhandling.evolution.AcceptEvolutionHandler",
        "com.cobblemon.mod.common.net.serverhandling.block.TMMachineTeachMoveHandler",
        "com.cobblemon.mod.common.net.serverhandling.trade.ChangeTradeAcceptanceHandler",
        "com.cobblemon.mod.common.net.serverhandling.trade.PerformTradeHandler"
}, remap = false)
abstract class InstancePartyLock {
    @Inject(method = "handle(Lcom/cobblemon/mod/common/api/net/NetworkPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"), cancellable = true)
    private void rumble$lockParty(NetworkPacket<?> packet, MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
        var session = InstanceApi.find(player).orElse(null);
        if (session == null || !session.getDimension().equals(player.level().dimension())) return;
        player.displayClientMessage(Component.translatable("rumble.arena.party_locked"), true);
        ci.cancel();
    }
}
