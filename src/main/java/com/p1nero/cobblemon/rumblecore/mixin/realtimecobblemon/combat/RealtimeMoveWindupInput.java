package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.cobblemon.mod.common.api.moves.Move;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.Component;
import com.johnn.realtimecobblemon.moves.MoveEngine;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MoveEngine.class, remap = false)
abstract class RealtimeMoveWindupInput {
    @Inject(method = "use0", at = @At("HEAD"), cancellable = true)
    private static void rumble$blockUnavailableMove(ServerPlayer player, int slot, float charge, CallbackInfo ci) {
        if (RealtimeMoveWindup.playerCastBlocked(player)) ci.cancel();
    }

    @Inject(method = "use0", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/MoveRegistry;info(Lcom/cobblemon/mod/common/api/moves/Move;)Lcom/johnn/realtimecobblemon/moves/MoveInfo;"), cancellable = true)
    private static void rumble$requirePp(ServerPlayer player, int slot, float charge, CallbackInfo ci, @Local Move move) {
        if (move.getCurrentPp() > 0) return;
        player.displayClientMessage(Component.translatable("rumble.realtime.no_pp", move.getDisplayName()), true);
        ci.cancel();
    }

    // This is after validation and after the windup; cancelled casts never spend PP.
    @Inject(method = "use0", at = @At(value = "FIELD", target =
            "Lcom/johnn/realtimecobblemon/Smash$Session;cooldownTotal:[I"))
    private static void rumble$spendPp(ServerPlayer player, int slot, float charge, CallbackInfo ci, @Local Move move) {
        move.setCurrentPp(Math.max(0, move.getCurrentPp() - 1));
    }

    @Inject(method = "use0", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/MoveRegistry;style(Lcom/johnn/realtimecobblemon/moves/MoveInfo;)Lcom/johnn/realtimecobblemon/moves/MoveStyle;"),
            cancellable = true)
    private static void rumble$windUpValidatedMove(ServerPlayer player, int slot, float charge, CallbackInfo ci) {
        if (RealtimeMoveWindup.releasingPlayerMove(player)) return;
        RealtimeMoveWindup.playerMove(player, () -> MoveEngine.use(player, slot, charge));
        ci.cancel();
    }

    @WrapOperation(method = "use0", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/Fighting;spinFree(Lcom/johnn/realtimecobblemon/Smash$Session;)Z"))
    private static boolean rumble$deferRapidSpin(Smash.Session session, Operation<Boolean> original) {
        return RealtimeMoveWindup.releasingPlayerMove(session.player) && original.call(session);
    }

    @Inject(method = "deal(Lcom/johnn/realtimecobblemon/Smash$Session;Lnet/minecraft/world/entity/LivingEntity;F)F",
            at = @At("HEAD"), cancellable = true)
    private static void rumble$blockDamageAndFlinch(Smash.Session source, LivingEntity target, float damage,
            CallbackInfoReturnable<Float> cir) {
        if (RealtimeMoveWindup.locked(target)) cir.setReturnValue(0.0F);
    }
}
