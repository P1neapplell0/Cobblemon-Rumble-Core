package com.p1nero.cobblemon.rumblecore.gym;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import java.util.Set;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Creates detached challenge Pokémon using Cobblemon registries. */
public final class GymTeamPokemon {
    private GymTeamPokemon() {}

    private static final TrainingStats ZERO = new TrainingStats(0, 0, 0, 0, 0, 0);
    private static final TrainingStats FAST = new TrainingStats(31, 31, 0, 0, 0, 31);

    public static List<Pokemon> jessie() {
        return List.of(rocket("ekans", "adamant", "jolly", FAST, "glare", "bite", "poisonsting", "screech"),
                rocket("wobbuffet", "bold", "calm", new TrainingStats(31, 0, 31, 0, 31, 0),
                        "counter", "charm", "amnesia", "destinybond"));
    }

    public static List<Pokemon> james() {
        return List.of(rocket("koffing", "modest", "bold", new TrainingStats(31, 0, 31, 31, 0, 0),
                        "sludge", "smog", "smokescreen", "selfdestruct"),
                rocket("bellsprout", "jolly", "adamant", FAST, "sleeppowder", "poisonpowder", "vinewhip", "acid"));
    }

    public static List<Pokemon> cassidy() {
        return List.of(teraRival("mightyena", "moxie", "choice_scarf", "snarl", "crunch", "suckerpunch", "thunderfang"),
                teraRival("drowzee", "innerfocus", "twisted_spoon", "hypnosis", "psychup", "nastyplot", "psyshock"));
    }

    public static List<Pokemon> butch() {
        return List.of(teraRival("hitmontop", "technician", "wide_lens", "triplekick", "fakeout", "detect", "rapidspin"),
                teraRival("primeape", "angerpoint", "punching_glove", "ragefist", "seismictoss", "assurance", "bulkup"));
    }

    private static Pokemon teraRival(String species, String ability, String item, String... moves) {
        var pokemon = create(species, null, 55, "hardy", ability, new LinkedHashSet<>(List.of(moves)),
                new TrainingStats(31, 31, 31, 31, 31, 31), new TrainingStats(0, 0, 0, 0, 0, 0), item, Set.of(), "tera_training");
        pokemon.heal();
        return pokemon;
    }

    public static List<Pokemon> may() {
        var special = new TrainingStats(16, 0, 0, 16, 0, 16);
        var physical = new TrainingStats(16, 16, 0, 0, 0, 16);
        var specialEv = new TrainingStats(0, 0, 0, 88, 0, 88);
        return List.of(
                may("ninetales", "timid", "drought", "charcoal_stick", special, specialEv, "flamethrower", "extrasensory", "darkpulse", "babydolleyes"),
                may("blaziken", "adamant", "speedboost", "mega_showdown:blazikenite", physical, new TrainingStats(0, 88, 0, 0, 0, 88), "detect", "blazekick", "brickbreak", "rockslide"),
                may("lilligant", "modest", "chlorophyll", "sitrus_berry", special, specialEv, "solarbeam", "gigadrain", "pollenpuff", "disarmingvoice"),
                may("solrock", "", "levitate", "focus_sash", new TrainingStats(31, 16, 0, 0, 0, 0), new TrainingStats(88, 88, 0, 0, 0, 0), "stoneedge", "zenheadbutt", "flareblitz", "bulldoze"));
    }

    private static Pokemon may(String species, String nature, String ability, String item, TrainingStats ivs, TrainingStats evs, String... moves) {
        var pokemon = create(species, null, 36, nature, ability, new LinkedHashSet<>(List.of(moves)), ivs, evs, item, Set.of(), "mega_training");
        pokemon.heal();
        return pokemon;
    }

    private static Pokemon rocket(String species, String natureA, String natureB, TrainingStats ivs, String... moves) {
        var pokemon = create(species, "", 20, ThreadLocalRandom.current().nextBoolean() ? natureA : natureB,
                "", new LinkedHashSet<>(List.of(moves)), ivs, ZERO, "", Set.of(), "professor_restock");
        pokemon.heal();
        return pokemon;
    }

    record TrainingStats(int hp, int attack, int defence, int specialAttack, int specialDefence, int speed) {
        private static final Stats[] ORDER = {Stats.HP, Stats.ATTACK, Stats.DEFENCE,
                Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED};

        void apply(Pokemon pokemon, boolean iv) {
            int[] values = {hp, attack, defence, specialAttack, specialDefence, speed};
            for (int i = 0; i < ORDER.length; i++) {
                if (iv) pokemon.setIV(ORDER[i], values[i]);
                else pokemon.setEV(ORDER[i], values[i]);
            }
        }
    }

    static Pokemon create(String species, String gender, int level, String nature, String ability,
                          Set<String> moves, TrainingStats ivs, TrainingStats evs, String item,
                          Set<String> aspects, String context) {
        var pokemon = new Pokemon();
        var speciesData = PokemonSpecies.getByIdentifier(cobblemonId(species));
        if (speciesData == null) throw new IllegalArgumentException("Unknown species: " + species);
        pokemon.setSpecies(speciesData);
        if (gender != null && !gender.isBlank()) pokemon.setGender(Gender.valueOf(gender));
        pokemon.setLevel(level);
        pokemon.setShiny(false);
        pokemon.setForcedAspects(Set.copyOf(aspects));
        pokemon.updateAspects();
        pokemon.updateForm();

        if (nature != null && !nature.isBlank()) {
            Nature selected = null;
            try {
                selected = Natures.getNature(cobblemonId(nature).getPath());
            } catch (RuntimeException ignored) {
                // Keep the generated nature for malformed legacy names.
            }
            if (selected != null) pokemon.setNature(selected);
            else GymCatalog.LOG.warn("[TrainerTeam] Unknown nature '{}' for {}/{}; using a random nature", nature, context, species);
        }
        ivs.apply(pokemon, true);
        evs.apply(pokemon, false);
        if (item != null && !item.isBlank()) {
            var id = cobblemonId(item);
            if (!BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalArgumentException("Unknown held item: " + id);
            pokemon.setHeldItem$common(BuiltInRegistries.ITEM.get(id).getDefaultInstance());
        }
        if (ability != null && !ability.isBlank()) {
            var template = Abilities.get(cobblemonId(ability).getPath());
            if (template == null) throw new IllegalArgumentException("Unknown ability: " + ability);
            pokemon.updateAbility(template.create(true, Priority.NORMAL));
        }
        int slot = 0;
        for (String move : moves) {
            var template = Moves.getByName(cobblemonId(move).getPath());
            if (template == null) throw new IllegalArgumentException("Unknown move: " + move);
            pokemon.getMoveSet().setMove(slot++, template.create());
        }
        return pokemon;
    }

    private static ResourceLocation cobblemonId(String key) {
        return key.indexOf(':') == -1 ? ResourceLocation.fromNamespaceAndPath("cobblemon", key) : ResourceLocation.parse(key);
    }
}
