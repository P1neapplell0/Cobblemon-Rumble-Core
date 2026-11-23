package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.RealTimeCobblemon;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RealTimeCobblemon.class, remap = false)
abstract class RealtimeMoveWindupRtcPackets {
    // RTC's shared main-thread wrapper covers switch, dodge, charge, guard and the other combat keys.
    @Inject(method = "lambda$safe$2", at = @At("HEAD"), cancellable = true)
    private static void rumble$blockRtcInput(IPayloadHandler<?> handler, String name, CustomPacketPayload payload,
            IPayloadContext context, CallbackInfo ci) {
        if (context.flow().isServerbound() && context.player() instanceof ServerPlayer player
                && RealtimeMoveWindup.locked(player)) ci.cancel();
    }
}
