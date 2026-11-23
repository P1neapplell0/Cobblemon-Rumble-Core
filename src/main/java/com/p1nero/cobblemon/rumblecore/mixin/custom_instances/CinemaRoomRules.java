package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.instance.InstanceSession;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InstanceManager.class, remap = false)
abstract class CinemaRoomRules {
    @Inject(method = "setReady", at = @At("HEAD"), cancellable = true)
    private void rumble$requireRole(ServerPlayer player, UUID id, boolean ready, CallbackInfo ci) {
        var match = ArenaMatch.inArena(player);
        if (ready && match != null && match.getId().equals(id)
                && (!match.cinemaRoleSelected(player) || match.cinemaRoleConflicts(player))) {
            player.displayClientMessage(Component.translatable(match.cinemaRoleConflicts(player)
                    ? "rumble.cinema.role_conflict" : "rumble.cinema.role_required"), false);
            ci.cancel();
        }
    }

    @Inject(method = "eligibleWinners", at = @At("RETURN"), cancellable = true)
    private void rumble$rewardWinningFaction(InstanceSession session, CallbackInfoReturnable<List<UUID>> cir) {
        if (session instanceof ArenaMatch match && match.cinemaRoom())
            cir.setReturnValue(cir.getReturnValue().stream().filter(match::cinemaRewardEligible).toList());
    }

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void rumble$requireCast(ServerPlayer player, String template, CallbackInfo ci) {
        var match = ArenaMatch.inArena(player);
        if (match != null && match.getDefinition().id().equals(template) && !match.cinemaRolesReady()) {
            player.displayClientMessage(Component.translatable(match.cinemaRoleConflicts()
                    ? "rumble.cinema.role_conflict" : "rumble.cinema.role_required"), false);
            ci.cancel();
        }
    }
}
