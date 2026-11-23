package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.cobblemon.rumblecore.prologue.client.CinemaCatalogScreen;
import com.p1nero.cobblemon.rumblecore.prologue.client.CinemaRoleNotice;
import com.p1nero.misc.custom_instances.client.InstanceClient;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InstanceClient.class, remap = false)
abstract class CinemaRoleUpdates {
    @Inject(method = "receive", at = @At("HEAD"), cancellable = true)
    private static void rumble$updateCast(String kind, CompoundTag data, CallbackInfo ci) {
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof CinemaCatalogScreen catalog && catalog.roomUpdate(kind, data)
                || screen instanceof CinemaRoleNotice notice && notice.roomUpdate(kind, data)) ci.cancel();
    }
}
