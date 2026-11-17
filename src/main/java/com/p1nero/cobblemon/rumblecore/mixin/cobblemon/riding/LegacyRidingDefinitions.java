package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.riding;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.SpeciesAdditions;
import com.p1nero.cobblemon.rumblecore.compat.riding.LegacyRidingData;
import java.io.BufferedReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = {PokemonSpecies.class, SpeciesAdditions.class}, remap = false)
abstract class LegacyRidingDefinitions {
    @ModifyArg(method = "parse", at = @At(value = "INVOKE",
            target = "Lcom/cobblemon/mod/common/api/data/JsonDataRegistry;parse(Ljava/io/BufferedReader;Lnet/minecraft/resources/ResourceLocation;)Ljava/lang/Object;"), index = 0)
    private BufferedReader rumble$upgradeSeats(BufferedReader reader) {
        return LegacyRidingData.migrate(reader);
    }
}
