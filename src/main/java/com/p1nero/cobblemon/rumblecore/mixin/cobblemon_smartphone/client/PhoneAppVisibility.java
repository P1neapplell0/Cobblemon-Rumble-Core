package com.p1nero.cobblemon.rumblecore.mixin.cobblemon_smartphone.client;

import com.nbp.cobblemon_smartphone.api.SmartphoneAction;
import com.nbp.cobblemon_smartphone.api.SmartphoneActionRegistry;
import com.p1nero.cobblemon.rumblecore.client.phone.ClientPhoneState;
import com.p1nero.cobblemon.rumblecore.phone.PhoneApp;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SmartphoneActionRegistry.class, remap = false)
abstract class PhoneAppVisibility {
    @Inject(method = "getEnabledActions", at = @At("RETURN"), cancellable = true)
    private void rumble$hideLockedApps(CallbackInfoReturnable<List<SmartphoneAction>> cir) {
        var player = Minecraft.getInstance().player;
        cir.setReturnValue(cir.getReturnValue().stream().filter(action -> {
            PhoneApp app = PhoneApp.forAction(action.getId());
            return app == null || app.available() && ClientPhoneState.unlocked(player, app);
        }).toList());
    }
}
