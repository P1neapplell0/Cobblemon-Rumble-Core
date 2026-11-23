package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.cobblemon.rumblecore.gym.MegaTrials;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import com.p1nero.misc.custom_instances.network.InstanceNetwork;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InstanceNetwork.class, remap = false)
abstract class MegaLobbyData {
    @Inject(method = "lobbyData", at = @At("RETURN"))
    private static void rumble$writePortrait(CallbackInfoReturnable<CompoundTag> cir, @Local(argsOnly = true) ServerPlayer viewer) {
        var data = cir.getReturnValue();
        var match = ArenaMatch.inArena(viewer);
        if (data.contains("party") && match != null && match.cinemaRoom()) {
            var roles = new CompoundTag();
            match.writeCinemaRoles(roles, viewer.getUUID());
            data.getCompound("party").put("rumbleCinema", roles);
        }
        var trial = MegaTrials.get(data.getCompound("definition").getString("id"));
        if (trial == null) return;
        var icon = new CompoundTag();
        trial.writePortrait(icon);
        data.put("rumbleMegaPortrait", icon);
    }
}
