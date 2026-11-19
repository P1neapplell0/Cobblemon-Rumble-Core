package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.config.CobblemonConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CobblemonConfig.class, remap = false)
abstract class CobblemonDefaults {
    @Shadow private boolean infiniteHealerCharge;
    @Shadow private int defaultBoxCount;
    @Shadow private double experienceShareMultiplier;
    @Shadow private float pokemonPerChunk;
    @Shadow private boolean rememberRidingCamera;
    @Shadow private double maxPokedexScanningDetectionRange;
    @Shadow private boolean disableRoll;
    @Shadow private int pastureMaxWanderDistance;
    @Shadow private boolean enableInFlightDismounting;
    @Shadow private boolean displayNameForUnknownPokemon;
    @Shadow private boolean infiniteRideStamina;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void rumble$packDefaults(CallbackInfo ci) {
        // Set defaults at creation; Cobblemon can still read explicit values from an existing JSON file.
        infiniteHealerCharge = true;
        defaultBoxCount = 100;
        experienceShareMultiplier = 1.0;
        pokemonPerChunk = 1.0F;
        rememberRidingCamera = true;
        maxPokedexScanningDetectionRange = 16.0;
        disableRoll = true;
        pastureMaxWanderDistance = 64;
        enableInFlightDismounting = true;
        displayNameForUnknownPokemon = true;
        infiniteRideStamina = true;
    }
}
