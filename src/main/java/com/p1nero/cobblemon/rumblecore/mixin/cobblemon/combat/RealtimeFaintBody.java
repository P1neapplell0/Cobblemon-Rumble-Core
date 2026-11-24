package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonServerDelegate;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeKnockout;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Pokemon.class, remap = false)
abstract class RealtimeFaintBody {
    @WrapWithCondition(method = "setCurrentHealth", at = @At(value = "INVOKE", target =
            "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;setHealth(F)V"))
    private boolean rumble$keepLaunchedEntityAlive(PokemonEntity body, float health) {
        // The stored Pokémon still faints. Only defer the physical entity's vanilla death.
        return !RealtimeKnockout.inFlight(body);
    }
}

@Mixin(value = PokemonServerDelegate.class, remap = false)
abstract class RealtimeFaintHealthSync {
    @WrapWithCondition(method = "updateHealth", at = @At(value = "INVOKE", target =
            "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;setHealth(F)V"))
    private boolean rumble$keepLaunchedEntityAlive(PokemonEntity body, float health) {
        return health > 0 || !RealtimeKnockout.inFlight(body);
    }
}

@Mixin(LivingEntity.class)
abstract class RealtimeKnockoutPhysics {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void rumble$slowFlightPhysics(Vec3 movement, CallbackInfo ci) {
        // Slow gravity and drag together with displacement, rather than shortening the arc.
        if ((Object) this instanceof PokemonEntity body && RealtimeKnockout.holdFlightStep(body)) ci.cancel();
    }

    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;isControlledByLocalInstance()Z"))
    private boolean rumble$letNoAiBodyTravel(boolean controlled) {
        // NoAI suppresses AI *and* vanilla travel. Enable travel only, not AI decision-making.
        return controlled || (Object) this instanceof PokemonEntity body && RealtimeKnockout.inFlight(body);
    }
}
