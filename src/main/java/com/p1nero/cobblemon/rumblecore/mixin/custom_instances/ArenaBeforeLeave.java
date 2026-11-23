package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.api.ExitReason;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InstanceManager.class, remap = false)
abstract class ArenaBeforeLeave {
    @Inject(method = "leave", at = @At("HEAD"))
    private void rumble$closeCombatBeforeTransfer(ServerPlayer player, ExitReason reason, boolean commit, CallbackInfo ci) {
        if (((InstanceManager) (Object) this).forPlayer(player) instanceof ArenaMatch arena) arena.beforeLeave(player);
    }
}
