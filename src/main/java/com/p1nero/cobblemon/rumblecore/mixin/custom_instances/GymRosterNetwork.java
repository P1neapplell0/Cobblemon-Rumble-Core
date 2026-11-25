package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaRoster;
import com.p1nero.misc.custom_instances.network.InstanceNetwork;
import com.p1nero.misc.custom_instances.storage.record.LeaderboardEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = InstanceNetwork.class, remap = false)
abstract class GymRosterNetwork {
    // Only decorate the verified run_details response, not the paged leaderboard payload.
    @WrapOperation(method = "handleAction", at = @At(value = "INVOKE", ordinal = 0, target =
            "Lcom/p1nero/misc/custom_instances/network/InstanceNetwork;send(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;Lnet/minecraft/nbt/CompoundTag;)V"))
    private static void rumble$teamReport(ServerPlayer player, String kind, CompoundTag data, Operation<Void> original,
                                          @Local LeaderboardEntry entry) {
        var run = entry.run();
        if (kind.equals("run_details") && run.completeTeam()) {
            try {
                var details = TagParser.parseTag(data.getString("details"));
                if (details.getString("kind").equals(ArenaRoster.KIND)) {
                    details.putBoolean("rumbleTeamRun", true);
                    if (run.battleReport() != null) details.put("rumbleTeamReport", run.battleReport().toTag());
                    data.putString("details", details.toString());
                }
            } catch (CommandSyntaxException ignored) { }
        }
        original.call(player, kind, data);
    }
}
