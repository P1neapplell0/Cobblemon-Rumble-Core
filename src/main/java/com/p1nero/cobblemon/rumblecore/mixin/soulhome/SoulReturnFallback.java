package com.p1nero.cobblemon.rumblecore.mixin.soulhome;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import leaf.soulhome.utils.DimensionHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = DimensionHelper.class, remap = false)
abstract class SoulReturnFallback {
    @WrapOperation(method = "FlipDimension", at = @At(value = "INVOKE", target =
            "Lleaf/soulhome/utils/TeleportHelper;teleportEntity(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/server/level/ServerLevel;DDDFF)V"))
    private static void rumble$returnToAvailableLevel(Entity entity, ServerLevel level, double x, double y, double z,
                                                     float yaw, float pitch, Operation<Void> original) {
        if (level != null) {
            original.call(entity, level, x, y, z, yaw, pitch);
        } else if (entity.getServer() != null) {
            var overworld = entity.getServer().overworld();
            var spawn = overworld.getSharedSpawnPos();
            entity.fallDistance = 0;
            entity.setDeltaMovement(Vec3.ZERO);
            original.call(entity, overworld, spawn.getX() + .5, (double) spawn.getY(), spawn.getZ() + .5, overworld.getSharedSpawnAngle(), 0f);
        }
    }
}
