package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.net.messages.server.BattleChallengePacket;
import com.cobblemon.mod.common.net.serverhandling.ChallengeHandler;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeBattleEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChallengeHandler.class, remap = false)
abstract class ChallengeStartsRealtime {
    /** Replace the actual challenge, leaving the ordinary send/recall packet available for following. */
    @Inject(method = "handle(Lcom/cobblemon/mod/common/net/messages/server/BattleChallengePacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"), cancellable = true)
    private void rumble$challenge(BattleChallengePacket packet, MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
        RealtimeBattleEntry.challenge(player, packet);
        ci.cancel();
    }
}
