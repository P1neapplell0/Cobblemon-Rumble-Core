package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

/** A compact public battle snapshot. Does not serialize Pokémon ownership or persistent data. */
public final class ArenaRoster {
    public static final String KIND = "cobblemon_rumble_core:arena_roster";
    private static final List<Stats> STATS = List.of(Stats.HP, Stats.ATTACK, Stats.DEFENCE,
            Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED);

    private ArenaRoster() {}

    static CompoundTag pokemon(Pokemon pokemon) {
        var entry = new CompoundTag();
        entry.putString("species", pokemon.getSpecies().getResourceIdentifier().toString());
        entry.putString("aspects", String.join(",", pokemon.getAspects().stream().filter(aspect -> aspect.length() <= 96).sorted().limit(10).toList()));
        entry.putInt("level", pokemon.getLevel());
        entry.putString("nature", pokemon.getNature().getDisplayName());
        entry.putString("ability", pokemon.getAbility().getDisplayName());
        if (!pokemon.heldItem().isEmpty()) entry.putString("item", BuiltInRegistries.ITEM.getKey(pokemon.heldItem().getItem()).toString());
        var moves = new ListTag();
        pokemon.getMoveSet().getMoves().forEach(move -> moves.add(StringTag.valueOf(move.getName())));
        entry.put("moves", moves);
        entry.putIntArray("ivs", STATS.stream().mapToInt(stat -> pokemon.getIvs().getOrDefault(stat)).toArray());
        entry.putIntArray("evs", STATS.stream().mapToInt(stat -> pokemon.getEvs().getOrDefault(stat)).toArray());
        return entry;
    }
}
