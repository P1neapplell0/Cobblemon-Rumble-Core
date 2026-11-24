package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class RealtimeMoveWindupServerInput {
    private static final String CHECK_THREAD = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V";

    @Shadow public ServerPlayer player;

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = CHECK_THREAD,
            shift = At.Shift.AFTER), cancellable = true)
    private void rumble$allowAimOnly(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (!RealtimeMoveWindup.locked(player)) return;
        if (packet.hasRotation()) {
            float yaw = packet.getYRot(player.getYRot()), pitch = packet.getXRot(player.getXRot());
            if (Float.isFinite(yaw) && Float.isFinite(pitch)) {
                player.setYRot(Mth.wrapDegrees(yaw));
                player.setXRot(Mth.clamp(pitch, -90, 90));
            }
        }
        ci.cancel();
    }

    @Inject(method = {"handlePlayerInput", "handleMoveVehicle", "handlePlayerAction",
            "handleUseItemOn", "handleUseItem", "handleInteract", "handlePlayerCommand", "handleAnimate",
            "handleSetCarriedItem", "handleContainerClick", "handleContainerButtonClick", "handlePlaceRecipe",
            "handleSetCreativeModeSlot", "handlePlayerAbilities", "handlePaddleBoat"},
            at = @At(value = "INVOKE", target = CHECK_THREAD, shift = At.Shift.AFTER), cancellable = true, require = 15)
    private void rumble$blockGameplayPackets(CallbackInfo ci) {
        if (RealtimeMoveWindup.locked(player)) ci.cancel();
    }
}
