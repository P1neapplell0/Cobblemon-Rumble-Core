package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.johnn.realtimecobblemon.moves.Brawl$Foe", remap = false)
public interface BrawlFoeAccess {
    @Accessor("mon") PokemonEntity rumble$mon();
    @Accessor("windup") int rumble$windup();
    @Accessor("actAt") long rumble$actAt();
    @Accessor("moves") List<?> rumble$moves();
}
