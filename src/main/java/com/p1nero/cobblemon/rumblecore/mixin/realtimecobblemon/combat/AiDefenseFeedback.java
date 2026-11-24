package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.moves.Brawl;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Brawl.class, remap = false)
abstract class AiDefenseFeedback {
    @ModifyExpressionValue(method = "parries", at = @At(value = "FIELD",
            target = "Lcom/johnn/realtimecobblemon/moves/Brawl$Difficulty;parry:F"))
    private static float rumble$lessParry(float chance) { return chance * .35f; }

    @ModifyExpressionValue(method = "tick", at = @At(value = "FIELD",
            target = "Lcom/johnn/realtimecobblemon/moves/Brawl$Difficulty;dodge:F"))
    private static float rumble$preferDodging(float chance) { return Math.min(.9f, chance + .15f); }

    @WrapOperation(method = "parries", at = @At(value = "INVOKE",
            target = "Lcom/johnn/realtimecobblemon/moves/Fx3D;send(Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;IIFII)V"))
    private static void rumble$raiseShield(ServerLevel level, int kind, Vec3 at, Vec3 direction, int primary,
                                           int secondary, float radius, int life, int entityId, Operation<Void> send) {
        send.call(level, 40, at, direction, primary, secondary, radius, 6, entityId);
        send.call(level, kind, at, direction, primary, secondary, radius, life, entityId);
    }
}
