package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.mojang.blaze3d.platform.IconSet;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies the pack art to Minecraft's GLFW and macOS application-icon paths. */
@Mixin(IconSet.class)
abstract class PackWindowIcon {
    @Inject(method = "getFile", at = @At("HEAD"), cancellable = true)
    private void rumble$brandIcon(PackResources pack, String name, CallbackInfoReturnable<IoSupplier<InputStream>> ci) {
        String path = "/rumble-window-icons/" + name;
        if (CobblemonRumbleCore.class.getResource(path) != null) {
            ci.setReturnValue(() -> {
                var input = CobblemonRumbleCore.class.getResourceAsStream(path);
                if (input == null) throw new IOException("Missing Rumble window icon " + name);
                return input;
            });
        }
    }
}
