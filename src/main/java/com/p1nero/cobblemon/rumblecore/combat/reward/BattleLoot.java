package com.p1nero.cobblemon.rumblecore.combat.reward;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining;
import net.minecraft.server.level.ServerLevel;

/** Shared by two transformed targets; do not reference another mixin class at runtime. */
public final class BattleLoot {
    private BattleLoot() {}

    public static boolean suppressed(PokemonEntity pokemon) {
        return PrologueTraining.target(pokemon) || pokemon.level() instanceof ServerLevel level
                && ArenaMatch.isArena(level) && !ArenaMatch.isLegendaryFoe(pokemon);
    }
}
