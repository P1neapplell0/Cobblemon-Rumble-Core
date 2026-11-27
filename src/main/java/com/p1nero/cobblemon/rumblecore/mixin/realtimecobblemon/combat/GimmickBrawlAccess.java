package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.moves.Brawl;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** RTC 1.0.3alpha.1 keeps the wild Pokémon targeting a player in this private foe table. */
@Mixin(value = Brawl.class, remap = false)
public interface GimmickBrawlAccess {
    @Accessor("FOES")
    static Map<PokemonEntity, ?> rumble$foes() { throw new AssertionError(); }
}
