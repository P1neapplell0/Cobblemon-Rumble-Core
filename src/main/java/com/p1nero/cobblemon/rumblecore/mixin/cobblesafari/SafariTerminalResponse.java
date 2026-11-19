package com.p1nero.cobblemon.rumblecore.mixin.cobblesafari;

import com.p1nero.cobblemon.rumblecore.prologue.SafariChapter;
import maxigregrze.cobblesafari.teleporter.TeleporterTickHandler;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TeleporterTickHandler.class, remap = false)
abstract class SafariTerminalResponse {
    @ModifyConstant(method = "teleportToSafari", constant = @Constant(intValue = 32), expect = 1)
    private static int rumble$dropNearTerrain(int vanillaOffset) {
        // The native slow-fall from 32 blocks up can trip Minecraft's floating check.
        return 4;
    }

    @Inject(method = "handleAcceptResponse", at = @At("HEAD"), cancellable = true)
    private static void rumble$validateTerminal(ServerPlayer player, boolean accepted, CallbackInfo ci) {
        if (!SafariChapter.checkTerminalResponse(player)) ci.cancel();
    }
}
