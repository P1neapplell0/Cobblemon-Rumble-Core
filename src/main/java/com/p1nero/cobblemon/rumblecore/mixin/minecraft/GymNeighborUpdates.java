package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 阻止预制场景中的邻居和形状更新破坏悬空方块等装饰状态。
 */
@Mixin(CollectingNeighborUpdater.class)
public class GymNeighborUpdates {

    @Shadow
    @Final
    private Level level;

    @Inject(method = "shapeUpdate", at = @At("HEAD"), cancellable = true)
    private void rumble$freezeShapeUpdates(Direction direction, BlockState state, BlockPos pos,
                                         BlockPos neighborPos, int updateFlags, int updateLimit,
                                         CallbackInfo ci) {
        rumble$cancelInFrozenDimension(ci);
    }

    @Inject(
            method = "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void rumble$freezeSimpleNeighborUpdates(BlockPos pos, Block block, BlockPos neighborPos,
                                                  CallbackInfo ci) {
        rumble$cancelInFrozenDimension(ci);
    }

    @Inject(
            method = "neighborChanged(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void rumble$freezeFullNeighborUpdates(BlockState state, BlockPos pos, Block block,
                                                BlockPos neighborPos, boolean movedByPiston,
                                                CallbackInfo ci) {
        rumble$cancelInFrozenDimension(ci);
    }

    @Inject(method = "updateNeighborsAtExceptFromFacing", at = @At("HEAD"), cancellable = true)
    private void rumble$freezeNeighborUpdates(BlockPos pos, Block block, Direction skipDirection,
                                            CallbackInfo ci) {
        rumble$cancelInFrozenDimension(ci);
    }

    private void rumble$cancelInFrozenDimension(CallbackInfo ci) {
        if (PrologueWorld.GYMS.equals(this.level.dimension())) {
            ci.cancel();
        }
    }
}
