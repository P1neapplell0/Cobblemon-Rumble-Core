package com.p1nero.cobblemon.rumblecore.mixin.cobblemon_smartphone.client;

import com.nbp.cobblemon_smartphone.actions.EnderAction;
import com.p1nero.cobblemon.rumblecore.client.phone.ClientPhoneState;
import com.p1nero.cobblemon.rumblecore.phone.PhoneApp;
import com.wintercogs.beyonddimensions.client.gui.NetMenuType;
import com.wintercogs.beyonddimensions.network.packet.c2s.OpenNetGuiPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EnderAction.class, remap = false)
abstract class CloudAction {
    @Inject(method = "onClick", at = @At("HEAD"), cancellable = true)
    private void rumble$openNetwork(CallbackInfo ci) {
        if (!ModList.get().isLoaded("beyonddimensions")) return;
        var client = Minecraft.getInstance();
        if (ClientPhoneState.unlocked(client.player, PhoneApp.CLOUD)) {
            // Beyond Dimensions handles its own device and network authorization.
            PacketDistributor.sendToServer(new OpenNetGuiPacket("", NetMenuType.NET_MENU));
            client.setScreen(null);
        }
        ci.cancel();
    }
}
