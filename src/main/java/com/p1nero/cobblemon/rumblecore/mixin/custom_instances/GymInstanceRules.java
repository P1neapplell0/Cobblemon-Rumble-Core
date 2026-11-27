package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.instance.InstanceSession;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InstanceManager.class, remap = false)
abstract class GymInstanceRules {
    @ModifyExpressionValue(method = "create(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;Ljava/lang/String;ZI)Ljava/util/UUID;",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"))
    private int rumble$gymCapacity(int limit, ServerPlayer player, String template, String password, boolean pvp, int requested) {
        return ArenaMatch.gymTemplate(template) ? ArenaMatch.GYM_SLOTS : limit;
    }

    @Inject(method = "enforceGameMode", at = @At("HEAD"), cancellable = true)
    private void rumble$preserveArenaObserver(ServerPlayer player, InstanceSession session, CallbackInfo ci) {
        // The all-bot Frontier owner is registered only for instance lifecycle and return recovery.
        if (session instanceof ArenaMatch match && (match.isGymObserver(player) || match.isFrontierObserverHost(player))) {
            if (!player.isSpectator()) player.setGameMode(GameType.SPECTATOR);
            ci.cancel();
        }
    }

    @WrapOperation(method = "tickSession", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target =
            "Lcom/p1nero/misc/custom_instances/instance/InstanceSession;elapsedTicks:J"))
    private void rumble$freezeResultTime(InstanceSession session, long ticks, Operation<Void> original) {
        if (!(session instanceof ArenaMatch match) || !match.gymPreviewActive()) original.call(session, ticks);
    }
}
