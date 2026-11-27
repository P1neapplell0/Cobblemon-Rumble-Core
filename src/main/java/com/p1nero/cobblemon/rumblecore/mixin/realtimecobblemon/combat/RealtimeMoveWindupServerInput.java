package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.p1nero.cobblemon.rumblecore.combat.arena.GimmickCinematics;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
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

    // Hard-stop positional packets only for full cutscenes; ordinary move windup allows movement.
    @Inject(method = {"handleMovePlayer", "handleMoveVehicle"}, at = @At(value = "INVOKE", target = CHECK_THREAD,
            shift = At.Shift.AFTER), cancellable = true, require = 2)
    private void rumble$freezeCinematicMovement(CallbackInfo ci) {
        if (GimmickCinematics.locked(player)) ci.cancel();
    }

    @Inject(method = "handlePlayerCommand", at = @At(value = "INVOKE", target = CHECK_THREAD,
            shift = At.Shift.AFTER), cancellable = true)
    private void rumble$allowMovementCommands(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        if (GimmickCinematics.locked(player)) { ci.cancel(); return; }
        if (!RealtimeMoveWindup.locked(player)) return;
        switch (packet.getAction()) {
            case PRESS_SHIFT_KEY, RELEASE_SHIFT_KEY, START_SPRINTING, STOP_SPRINTING, START_FALL_FLYING -> { }
            default -> ci.cancel();
        }
    }

    @Inject(method = {"handlePlayerAction",
            "handleUseItemOn", "handleUseItem", "handleInteract", "handleAnimate",
            "handleSetCarriedItem", "handleContainerClick", "handleContainerButtonClick", "handlePlaceRecipe",
            "handleSetCreativeModeSlot"},
            at = @At(value = "INVOKE", target = CHECK_THREAD, shift = At.Shift.AFTER), cancellable = true, require = 9)
    private void rumble$blockGameplayPackets(CallbackInfo ci) {
        if (RealtimeMoveWindup.locked(player)) ci.cancel();
    }
}
