package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.MoveEngine;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Explains a failed RTC grab attempt instead of silently consuming the input.
 *
 * Maintenance: targets MoveEngine.use0 in RTC 1.0.3alpha.1. If that method or
 * Fighting.grabTarget's descriptor changes, update the @Redirect target and
 * verify the feedback still runs only on the server player.
 */
@Mixin(value = MoveEngine.class, remap = false)
abstract class RealtimeGrabTargetFeedback {
    @Redirect(method = "use0", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/Fighting;grabTarget(Lcom/johnn/realtimecobblemon/Smash$Session;)Lnet/minecraft/world/entity/LivingEntity;"))
    private static LivingEntity rumble$explainMissingGrabTarget(Smash.Session session) {
        LivingEntity target = FightingAccess.rumble$grabTarget(session);
        if (target == null && session.player instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("rumble.realtime.no_grab_target"), true);
        }
        return target;
    }
}
