package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.p1nero.cobblemon.rumblecore.client.combat.FrontierKnockoutOverlay;
import com.p1nero.cobblemon.rumblecore.combat.arena.FrontierKnockout;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

/** RTC moves the local player and mirrors its position onto the controlled Pokémon. */
@Mixin(LocalPlayer.class)
abstract class KnockoutLocalMovement {
    @WrapMethod(method = "move")
    private void rumble$slowControlledMovement(MoverType type, Vec3 movement, Operation<Void> original) {
        var player = (LocalPlayer) (Object) this;
        boolean slow = type == MoverType.SELF && FrontierKnockoutOverlay.slowCue(player) != null;
        original.call(type, slow ? movement.scale(FrontierKnockout.SPEED) : movement);
    }
}
