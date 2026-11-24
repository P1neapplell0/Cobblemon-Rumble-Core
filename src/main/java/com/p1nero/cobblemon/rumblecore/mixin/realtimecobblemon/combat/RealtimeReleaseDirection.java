package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.MoveCombat;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.MoveEngine;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeReleaseAim;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MoveCombat.class, remap = false)
abstract class RealtimeReleaseDirection {
    @WrapOperation(method = "beam", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/player/Player;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 rumble$trackLockedTarget(Player player, Operation<Vec3> original, Smash.Session session, double range) {
        return RealtimeReleaseAim.direction(session, player.getEyePosition(), original.call(player));
    }
}

@Mixin(value = MoveEngine.class, remap = false)
abstract class RealtimeProjectileDirection {
    @WrapOperation(method = {"launch", "beam"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/level/ServerPlayer;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 rumble$trackMuzzle(ServerPlayer player, Operation<Vec3> original) {
        var session = Smash.session(player);
        Vec3 look = original.call(player);
        return session == null ? look : RealtimeReleaseAim.direction(session,
                player.getEyePosition().subtract(0, session.pokemon.getBbHeight() * .15, 0), look);
    }
}
