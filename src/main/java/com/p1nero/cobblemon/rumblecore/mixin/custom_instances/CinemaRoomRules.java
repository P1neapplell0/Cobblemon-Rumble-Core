package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InstanceManager.class, remap = false)
abstract class CinemaRoomRules {
    @Inject(method = "setReady", at = @At("HEAD"), cancellable = true)
    private void rumble$requireRole(ServerPlayer player, UUID id, boolean ready, CallbackInfo ci) {
        var match = ArenaMatch.inArena(player);
        if (ready && match != null && match.getId().equals(id) && !match.cinemaRoleSelected(player)) {
            player.displayClientMessage(Component.translatable("rumble.cinema.role_required"), false);
            ci.cancel();
        }
    }

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void rumble$requireCast(ServerPlayer player, String template, CallbackInfo ci) {
        var match = ArenaMatch.inArena(player);
        if (match != null && match.getDefinition().id().equals(template) && !match.cinemaRolesReady()) {
            player.displayClientMessage(Component.translatable("rumble.cinema.role_required"), false);
            ci.cancel();
        }
    }
}
