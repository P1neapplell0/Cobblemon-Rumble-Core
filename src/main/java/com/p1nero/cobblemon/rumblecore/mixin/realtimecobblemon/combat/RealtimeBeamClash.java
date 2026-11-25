package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.moves.Clash;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeBeamClashPacket;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Clash.class, remap = false)
abstract class RealtimeBeamClash {
    // This helper is invoked only by matched beams, not by projectile-versus-projectile collisions.
    @Inject(method = "start", at = @At("TAIL"))
    private static void rumble$frameBeamClash(ServerLevel level, Clash.Side a, Clash.Side b, long time, CallbackInfo ci) {
        if (a.s() != null) PacketDistributor.sendToPlayer(a.s().player, new RealtimeBeamClashPacket(a.user().getId(), b.user().getId()));
        if (b.s() != null) PacketDistributor.sendToPlayer(b.s().player, new RealtimeBeamClashPacket(b.user().getId(), a.user().getId()));
    }
}
