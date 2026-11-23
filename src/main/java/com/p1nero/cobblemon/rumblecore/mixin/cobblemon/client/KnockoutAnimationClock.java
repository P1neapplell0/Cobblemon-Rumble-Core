package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.client;

import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.p1nero.cobblemon.rumblecore.client.combat.FrontierKnockoutOverlay;
import com.p1nero.cobblemon.rumblecore.combat.arena.FrontierKnockout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PosableState.class, remap = false)
abstract class KnockoutAnimationClock {
    @Unique private Object rumble$koCue;
    @Unique private float rumble$koAnchor;
    @Unique private float rumble$koOffset;
    @Unique private float rumble$koLastRaw;

    @Inject(method = "getAnimationSeconds", at = @At("RETURN"), cancellable = true)
    private void rumble$animateAtKnockoutSpeed(CallbackInfoReturnable<Float> result) {
        if (!((Object) this instanceof PokemonClientDelegate delegate)) return;
        float raw = result.getReturnValueF();
        Object cue = FrontierKnockoutOverlay.slowCue(delegate.getEntity());
        if (raw + .1f < rumble$koLastRaw) {
            // Cobblemon explicitly reset this animation clock.
            rumble$koOffset = 0;
            rumble$koCue = null;
        }
        if (cue != rumble$koCue) {
            if (rumble$koCue != null) rumble$koOffset += Math.max(0, raw - rumble$koAnchor) * (1 - FrontierKnockout.SPEED);
            rumble$koCue = cue;
            rumble$koAnchor = raw;
        }
        rumble$koLastRaw = raw;
        float stretch = cue == null ? 0 : Math.max(0, raw - rumble$koAnchor) * (1 - FrontierKnockout.SPEED);
        if (rumble$koOffset != 0 || stretch != 0) result.setReturnValue(raw - rumble$koOffset - stretch);
    }
}
