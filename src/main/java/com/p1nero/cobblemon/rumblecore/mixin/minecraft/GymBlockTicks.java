package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 冻结预制场景中的自然方块变化，同时保留实体和方块实体的正常 tick。
 */
@Mixin(ServerLevel.class)
public class GymBlockTicks {

    @Inject(method = "tickChunk", at = @At("HEAD"), cancellable = true)
    private void rumble$freezeRandomTicks(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        if (PrologueWorld.GYMS.equals(((ServerLevel) (Object) this).dimension())) {
            ci.cancel();
        }
    }

    @Inject(method = "tickBlock", at = @At("HEAD"), cancellable = true)
    private void rumble$freezeScheduledBlockTicks(BlockPos pos, Block block, CallbackInfo ci) {
        if (PrologueWorld.GYMS.equals(((ServerLevel) (Object) this).dimension())
                && !rumble$isInteractiveBlock(block)) {
            ci.cancel();
        }
    }

    private static boolean rumble$isInteractiveBlock(Block block) {
        BlockState state = block.defaultBlockState();
        return state.is(BlockTags.BEDS)
                || state.is(BlockTags.DOORS)
                || state.is(BlockTags.TRAPDOORS);
    }

    @Inject(method = "tickFluid", at = @At("HEAD"), cancellable = true)
    private void rumble$freezeScheduledFluidTicks(BlockPos pos, Fluid fluid, CallbackInfo ci) {
        if (PrologueWorld.GYMS.equals(((ServerLevel) (Object) this).dimension())) {
            ci.cancel();
        }
    }
}
