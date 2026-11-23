package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
abstract class RealtimeMoveWindupMobAI {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void rumble$pausePokemonBrain(CallbackInfo ci) {
        if ((Object) this instanceof PokemonEntity pokemon && RealtimeMoveWindup.locked(pokemon)) ci.cancel();
    }
}
