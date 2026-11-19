package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.client.gui.interact.battleRequest.BattleConfigureGUI;
import com.cobblemon.mod.common.net.messages.client.PlayerInteractOptionsPacket;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "com.cobblemon.mod.common.client.gui.interact.wheel.InteractWheelGuiFactoryKt", remap = false)
abstract class SingleChallengeScreen {
    @WrapOperation(method = "createPlayerInteractGui$lambda$3", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private static void rumble$singles(Minecraft mc, Screen screen, Operation<Void> call, PlayerInteractOptionsPacket packet) {
        call.call(mc, screen instanceof BattleConfigureGUI ? ArenaClient.challenge(packet) : screen);
    }
}
