package com.p1nero.cobblemon.rumblecore.mixin.rct;

import com.gitlab.srcmc.rctmod.api.service.TrainerSpawner;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TrainerSpawner.class, remap = false)
abstract class TrainerSpawnLimits {
    @Inject(method = "attemptSpawnFor(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true)
    private void rumble$limitNaturalSpawns(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player.isSpectator() || !player.isAlive() || !Level.OVERWORLD.equals(player.level().dimension())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "spawnFor(Lnet/minecraft/world/entity/player/Player;Ljava/lang/String;Lnet/minecraft/core/BlockPos;ZZ)Lcom/gitlab/srcmc/rctmod/world/entities/TrainerMob;",
            at = @At("HEAD"), cancellable = true)
    private void rumble$blockHallSpawners(Player player, String id, BlockPos pos, boolean persistent, boolean unique,
            CallbackInfoReturnable<TrainerMob> cir) {
        if (PrologueWorld.GYMS.equals(player.level().dimension())) cir.setReturnValue(null);
    }

    @Redirect(method = "nextPos", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;isLoaded(Lnet/minecraft/core/BlockPos;)Z", remap = true))
    private boolean rumble$onlySearchLoadedChunks(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && !server.isOutsideBuildHeight(pos)
                && server.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()),
                        SectionPos.blockToSectionCoord(pos.getZ())) != null;
    }
}
