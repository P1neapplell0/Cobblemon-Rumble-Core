package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.client.ClientSmash;
import com.p1nero.cobblemon.rumblecore.client.combat.BattleOrbit;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ClientSmash.class, remap = false)
abstract class OrbitFlightMovement {
    // RTC flight reads keys directly, not the rotated Input used by ground movement and dodge.
    @Redirect(method = "plant", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private static float rumble$flightHeading(LocalPlayer player) { return BattleOrbit.movementYaw(player); }
}
