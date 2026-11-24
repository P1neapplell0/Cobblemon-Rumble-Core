package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.pokemon.Pokemon;

/** Temporary max-HP scaling for the arena's Pokémon, never written to the Pokémon's save data. */
public interface ArenaPokemonHealth {
    void rumble$setArenaHealthScale(double scale);

    static void apply(Pokemon pokemon, double scale) {
        ((ArenaPokemonHealth) (Object) pokemon).rumble$setArenaHealthScale(scale);
        pokemon.setCurrentHealth(pokemon.getMaxHealth());
    }
}
