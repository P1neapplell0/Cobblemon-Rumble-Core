package com.p1nero.cobblemon.rumblecore.mixin.cobblemon_smartphone;

import com.nbp.cobblemon_smartphone.util.SmartphoneHelper;
import com.p1nero.cobblemon.rumblecore.client.phone.ClientPhoneState;
import com.p1nero.cobblemon.rumblecore.phone.PhoneApp;
import com.p1nero.cobblemon.rumblecore.phone.PhoneState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replace phone-stack upgrades for our mapped apps only; leave the other upstream apps alone. */
@Mixin(value = SmartphoneHelper.class, remap = false)
abstract class PhoneUpgradeGate {
    @Inject(method = {"satisfiesUpgradeRequirement", "hasUpgradeOnAnySmartphone"}, at = @At("HEAD"), cancellable = true)
    private void rumble$usePlayerUnlock(Player player, String key, String actionId, CallbackInfoReturnable<Boolean> cir) {
        PhoneApp app = PhoneApp.forAction(actionId);
        if (app == null) return;
        if (player instanceof ServerPlayer serverPlayer) cir.setReturnValue(PhoneState.allow(serverPlayer, app));
        else cir.setReturnValue(ClientPhoneState.unlocked(player, app));
    }
}
