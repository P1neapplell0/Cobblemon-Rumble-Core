package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.net.messages.server.SendOutPokemonPacket;
import com.cobblemon.mod.common.net.serverhandling.storage.SendOutPokemonHandler;
import com.johnn.realtimecobblemon.Smash;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeBattleEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SendOutPokemonHandler.class, remap = false)
abstract class SendOutSessionCleanup {
    @Inject(method = "handle(Lcom/cobblemon/mod/common/net/messages/server/SendOutPokemonPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"))
    private void rumble$normalSend(SendOutPokemonPacket packet, MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
        // A later ordinary send/recall invalidates an earlier challenge waiting on its throw animation.
        RealtimeBattleEntry.cancelPending(player);
        if (packet.getSlot() < 0 || packet.getSlot() >= 6) return;
        var session = Smash.session(player);
        if (session != null && session.pokemon.getPokemon() == Cobblemon.INSTANCE.getStorage().getParty(player).get(packet.getSlot())) {
            Smash.end(player, null, true);
        }
    }
}
