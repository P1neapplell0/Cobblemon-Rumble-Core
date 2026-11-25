package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.client.ClientSmash;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes RTC combat audio originate at the controlled creature rather than the
 * camera, so oversized Pokémon and distant PvP opponents remain audible.
 *
 * Maintenance: targets Minecraft SoundEngine.updateSource and RTC 1.0.3alpha.1
 * ClientSmash APIs. Re-check the redirect and controlled-entity API on updates.
 */
@Mixin(SoundEngine.class)
abstract class RealtimeBattleSoundListener {
    @Redirect(method = "updateSource", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/Camera;getPosition()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 rumble$listenFromCombatant(Camera camera) {
        var mc = Minecraft.getInstance();
        if (mc.level != null && ClientSmash.active()) {
            var id = ClientSmash.controlledEntity();
            var pokemon = id == null ? null : mc.level.getEntity(id);
            if (pokemon != null && !pokemon.isRemoved()) {
                // Match RTC's sound origins around the body, even for oversized models.
                return pokemon.getPosition(mc.getTimer().getGameTimeDeltaPartialTick(true))
                        .add(0, Math.min(1.6, pokemon.getBbHeight() * 0.5), 0);
            }
        }
        return camera.getPosition();
    }
}
