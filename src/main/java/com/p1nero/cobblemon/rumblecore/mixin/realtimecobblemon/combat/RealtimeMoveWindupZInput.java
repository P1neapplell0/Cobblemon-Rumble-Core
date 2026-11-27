package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.moves.ZMoves;
import com.johnn.realtimecobblemon.Smash;
import com.llamalad7.mixinextras.sugar.Local;
import com.p1nero.cobblemon.rumblecore.combat.arena.GimmickCinematics;
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
    private static void rumble$warnBeforeZScene(ServerPlayer player, CallbackInfoReturnable<Boolean> cir, @Local Smash.Session session) {
        // RTC 1.0.3alpha.1: validation is complete here, but Z charge is not spent.
        // One hook owns both paths. On cinematic commit don't queue the short windup
        // and lose the replay flag before ZMoves.activate gets to spend the charge.
        if (GimmickCinematics.releasingZ(player)) return;
        if (GimmickCinematics.start(session, GimmickCinematics.Kind.Z_MOVE,
                () -> GimmickCinematics.releaseZ(player, () -> ZMoves.activate(player)))) {
            cir.setReturnValue(true);
            return;
        }
        if (!RealtimeMoveWindup.releasingPlayerMove(player)) {
            cir.setReturnValue(RealtimeMoveWindup.playerMove(player, null, () -> ZMoves.activate(player)));
        }
    }
}
