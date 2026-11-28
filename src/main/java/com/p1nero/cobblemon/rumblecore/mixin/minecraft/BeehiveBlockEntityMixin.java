package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.RumbleConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Prevents beehives and bee nests from ever materializing stored bee entities. */
@Mixin(BeehiveBlockEntity.class)
public abstract class BeehiveBlockEntityMixin {

    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true)
    private static void rumble$disableOccupantTick(Level level, BlockPos pos,
                                                BlockState state,
                                                BeehiveBlockEntity beehive,
                                                CallbackInfo ci) {
        if (RumbleConfig.disablesBeehives()) ci.cancel();
    }

    @Inject(method = "releaseOccupant", at = @At("HEAD"), cancellable = true)
    private static void rumble$disableBeeRelease(
            Level level,
            BlockPos pos,
            BlockState state,
            BeehiveBlockEntity.Occupant occupant,
            List<Entity> releasedEntities,
            BeehiveBlockEntity.BeeReleaseStatus releaseStatus,
            BlockPos flowerPos,
            CallbackInfoReturnable<Boolean> cir) {
        if (RumbleConfig.disablesBeehives()) cir.setReturnValue(false);
    }
}
