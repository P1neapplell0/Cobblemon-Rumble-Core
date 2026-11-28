package com.p1nero.cobblemon.rumblecore.mixin.cobblesafari;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.p1nero.cobblemon.rumblecore.prologue.MerchantHoopa;
import maxigregrze.cobblesafari.dungeon.DungeonTeleportCountdown;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = DungeonTeleportCountdown.class, remap = false)
abstract class MerchantDungeonCountdown {
    // Cobble Safari 0.4.1 re-reads its entrance block each tick. Only paid merchant
    // countdowns use a private virtual entrance; all real portals use the original lookup.
    @WrapOperation(method = "onServerTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private static BlockEntity rumble$merchantPortal(ServerLevel level, BlockPos pos, Operation<BlockEntity> original,
                                                      @Local ServerPlayer player) {
        var virtual = MerchantHoopa.virtualPortal(player, level, pos);
        return virtual == null ? original.call(level, pos) : virtual;
    }
}
