package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NaturalSpawner.class)
abstract class PrologueNaturalSpawns {
    @Inject(method = "spawnForChunk", at = @At("HEAD"), cancellable = true)
    private static void rumble$skipNaturalSpawns(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState state,
            boolean spawnFriendlies, boolean spawnMonsters, boolean spawnPersistent, CallbackInfo ci) {
        if (PrologueWorld.blocksAmbientSpawns(level)) ci.cancel();
    }
}
