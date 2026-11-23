package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.moves.ZMoves;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import net.minecraft.server.level.ServerPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ZMoves.class, remap = false)
abstract class RealtimeMoveWindupZInput {
    @Inject(method = "activate", at = @At("HEAD"), cancellable = true)
    private static void rumble$blockUnavailableZMove(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (RealtimeMoveWindup.playerCastBlocked(player)) cir.setReturnValue(false);
    }

    @Inject(method = "activate", at = @At(value = "FIELD", target =
            "Lcom/johnn/realtimecobblemon/moves/ZMoves$State;used:Z", opcode = Opcodes.PUTFIELD), cancellable = true)
    private static void rumble$warnBeforeZScene(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!RealtimeMoveWindup.releasingPlayerMove(player)) {
            cir.setReturnValue(RealtimeMoveWindup.playerMove(player, () -> ZMoves.activate(player)));
        }
    }
}
