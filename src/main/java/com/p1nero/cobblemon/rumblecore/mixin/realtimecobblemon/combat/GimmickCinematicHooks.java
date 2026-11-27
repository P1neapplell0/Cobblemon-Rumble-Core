package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Eternamax;
import com.johnn.realtimecobblemon.MegaEvo;
import com.johnn.realtimecobblemon.Smash;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.combat.arena.GimmickCinematics;
import com.p1nero.cobblemon.rumblecore.combat.arena.GimmickCinematics.Kind;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** RTC 1.0.3alpha.1: intercept validated transitions, not key presses or translated messages. */
final class GimmickCinematicHooks {
    @Mixin(value = MegaEvo.class, remap = false)
    static abstract class Mega {
        @Unique private static boolean rumble$afterCinematic;

        // begin is reached after the Mega checks; reversions take a separate branch.
        @WrapOperation(method = {"onKey", "arena"}, at = @At(value = "INVOKE", target =
                "Lcom/johnn/realtimecobblemon/MegaEvo;begin(Lnet/minecraft/server/level/ServerPlayer;Lcom/johnn/realtimecobblemon/Smash$Session;Ljava/lang/String;)V"))
        private static void rumble$beforeMega(ServerPlayer player, Smash.Session session, String form, Operation<Void> original) {
            if (GimmickCinematics.locked(player)) return;
            if (!GimmickCinematics.start(session, Kind.MEGA_EVOLUTION, () -> {
                rumble$afterCinematic = true;
                try { original.call(player, session, form); }
                finally { rumble$afterCinematic = false; }
            })) original.call(player, session, form);
        }

        // RTC 1.0.3alpha.1 evolves at start + 60. The cutscene replaces only that windup.
        @ModifyExpressionValue(method = "begin", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getGameTime()J"))
        private static long rumble$replaceMegaWindup(long now) {
            return rumble$afterCinematic ? now - 60 : now;
        }
    }

    @Mixin(value = Eternamax.class, remap = false)
    static abstract class Max {
        // Dynamax and Gigantamax share begin(); RTC chooses which form inside it.
        @WrapOperation(method = {"onKey", "arena"}, at = @At(value = "INVOKE", target =
                "Lcom/johnn/realtimecobblemon/Eternamax;begin(Lnet/minecraft/server/level/ServerPlayer;Lcom/johnn/realtimecobblemon/Smash$Session;J)V"))
        private static void rumble$beforeMax(ServerPlayer player, Smash.Session session, long now, Operation<Void> original) {
            if (GimmickCinematics.locked(player)) return;
            // RTC 1.0.3alpha.1 starts growing at start + 40, then plays its usual scale transition.
            if (!GimmickCinematics.start(session, Kind.DYNAMAX,
                    () -> original.call(player, session, player.level().getGameTime() - 40))) original.call(player, session, now);
        }
    }

}
