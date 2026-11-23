package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.pokemon.Pokemon;

/** Applied only to temporary movie Pokémon, before sending them out. */
public interface CinemaHealth {
    void rumble$cinemaHealthScale(double scale);

    static void apply(Pokemon pokemon, double scale) {
        if (scale <= 1) return;
        ((CinemaHealth) (Object) pokemon).rumble$cinemaHealthScale(scale);
        pokemon.setCurrentHealth(pokemon.getMaxHealth());
    }
}
