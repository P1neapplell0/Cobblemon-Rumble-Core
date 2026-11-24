package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.client.anime.Reactions;
import com.p1nero.cobblemon.rumblecore.client.combat.FrontierKnockoutOverlay;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaKnockoutVisual;
import com.p1nero.cobblemon.rumblecore.combat.arena.FrontierKnockout;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

@Mixin(value = Reactions.class, remap = false)
abstract class KnockoutTumble {
    @WrapOperation(method = "look", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/client/anime/Reactions;fallAngle(F)F"))
    private static float rumble$slowTumble(float progress, Operation<Float> original, Entity entity) {
        if (entity instanceof ArenaKnockoutVisual visual && visual.rumble$isKnockedOut()
                && FrontierKnockoutOverlay.inCue(entity)) {
            float slowPortion = FrontierKnockout.SLOW_TICKS / (float) FrontierKnockout.TUMBLE_TICKS;
            progress -= Math.min(progress, slowPortion) * (1 - 1f / FrontierKnockout.FLIGHT_STEP);
        }
        return original.call(progress);
    }
}
