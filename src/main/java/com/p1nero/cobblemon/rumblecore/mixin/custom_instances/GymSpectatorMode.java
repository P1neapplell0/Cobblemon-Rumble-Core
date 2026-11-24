package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.misc.custom_instances.event.InstanceEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InstanceEvents.class, remap = false)
abstract class GymSpectatorMode {
    @Inject(method = "gameMode", at = @At("HEAD"), cancellable = true)
    private static void rumble$allowGymObserver(PlayerEvent.PlayerChangeGameModeEvent event, CallbackInfo ci) {
        if (event.getNewGameMode() == GameType.SPECTATOR && event.getEntity() instanceof ServerPlayer player) {
            var match = ArenaMatch.inArena(player);
            if (match != null && match.isGymObserver(player)) ci.cancel();
        }
    }
}
