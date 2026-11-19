package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.spawning;

import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawner;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import java.util.UUID;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerSpawner.class, remap = false)
abstract class PrologueWildSpawns {
    @Shadow public abstract UUID getUuid();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void rumble$skipWildSpawns(CallbackInfo ci) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        var player = server.getPlayerList().getPlayer(getUuid());
        if (player != null && PrologueWorld.blocksAmbientSpawns(player.serverLevel())) ci.cancel();
    }
}
