package com.p1nero.cobblemon.rumblecore.gym;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import com.johnn.realtimecobblemon.moves.MoveRegistry;
import com.johnn.realtimecobblemon.moves.MoveStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Creates detached challenge Pokémon using Cobblemon registries. */
public final class GymTeamPokemon {
    private GymTeamPokemon() {}

    private static final TrainingStats ZERO = new TrainingStats(0, 0, 0, 0, 0, 0);
    private static final TrainingStats FAST = new TrainingStats(31, 31, 0, 0, 0, 31);

    // Keep this in sync with the shapes accepted by RTC's Brawl.picks, not MoveEngine's full set.
    private static final List<MoveStyle.Shape> BRAWL_SHAPES = List.of(MoveStyle.Shape.STRIKE, MoveStyle.Shape.RAIN,
            MoveStyle.Shape.SHOCKWAVE, MoveStyle.Shape.DASH, MoveStyle.Shape.PROJECTILE, MoveStyle.Shape.BEAM,
            MoveStyle.Shape.BOLT, MoveStyle.Shape.STREAM, MoveStyle.Shape.MELEE);
    private static final Set<String> REACTIVE_MOVES = Set.of("counter", "mirrorcoat", "destinybond", "protect",
            "detect", "spikyshield", "kingsshield", "banefulbunker", "silktrap", "burningbulwark");

    /** Replaces tactical sets with legal moves that RTC's live-battle AI can actually cast. */
    public static void prepareArenaMoves(Pokemon pokemon, String context) {
        prepareArenaMoves(pokemon, context, false);
    }

    /** Authored encounters keep their legal, castable signature moves before filling missing slots. */
    public static void prepareArenaMoves(Pokemon pokemon, String context, boolean preferAuthored) {
        prepareMoves(pokemon, context, preferAuthored, false);
    }

    static void prepareCinemaMoves(Pokemon pokemon, String context) {
        prepareMoves(pokemon, "cinema/" + context, true, true);
    }

    private static void prepareMoves(Pokemon pokemon, String context, boolean preferAuthored, boolean cinema) {
        var moveset = pokemon.getMoveSet();
        var authored = moveset.getMoveTemplates();
        var legalNames = new HashSet<String>();
        var castable = new HashMap<String, ScoredMove>();
        var passive = new HashSet<String>();
        for (var move : pokemon.getForm().getMoves().getAllLegalMoves()) {
            String key = moveKey(move);
            if (!legalNames.add(key)) continue;
            try {
                var info = MoveRegistry.info(move.create()); // Same fallback to Cobblemon move data as Brawl.
                if (!hasDamage(info)) {
                    if (info != null && (info.isStatus() || info.power() <= 0)) passive.add(key);
                    continue;
                }
                if (REACTIVE_MOVES.contains(info.id()) || info.selfdestruct() || info.ohko()) continue;
                var style = MoveRegistry.style(info);
                if (BRAWL_SHAPES.contains(style.shape())) castable.put(key, score(pokemon, move, info, style));
            } catch (RuntimeException e) {
                GymCatalog.LOG.warn("[ArenaMoves] Ignoring unreadable RTC move {} for {}", key, context, e);
            }
        }

        var selected = new LinkedHashSet<MoveTemplate>(4);
        if (preferAuthored) {
            for (var move : authored) {
                var candidate = castable.get(moveKey(move));
                if (candidate != null) selected.add(candidate.template());
            }
        }
        if (selected.size() < 4) {
            var candidates = new ArrayList<>(castable.values());
            candidates.sort(Comparator.comparingDouble(ScoredMove::score).reversed()
                    .thenComparing(move -> move.template().getName()));
            var coveredShapes = new HashSet<MoveStyle.Shape>();
            for (var move : selected) coveredShapes.add(castable.get(moveKey(move)).shape());
            for (var shape : BRAWL_SHAPES) {
                if (selected.size() == 4) break;
                if (coveredShapes.contains(shape)) continue;
                candidates.stream().filter(candidate -> candidate.shape() == shape).findFirst()
                        .ifPresent(candidate -> selected.add(candidate.template()));
            }
            for (var candidate : candidates) {
                if (selected.size() == 4) break;
                selected.add(candidate.template());
            }
        }
        java.util.function.Predicate<MoveTemplate> ranged = cinema ? GymTeamPokemon::isCinemaRangedAttack : GymTeamPokemon::isRangedAttack;
        if (selected.stream().noneMatch(ranged)) {
            castable.values().stream().filter(candidate -> ranged.test(candidate.template()))
                    .max(Comparator.comparingDouble(ScoredMove::score).thenComparing(candidate -> candidate.template().getName()))
                    .ifPresent(candidate -> {
                        if (selected.size() == 4) selected.removeLast();
                        selected.add(candidate.template());
                    });
        }
        boolean basicAttacksOnly = selected.isEmpty();
        if (basicAttacksOnly) {
            // Brawl already supplies close-range attacks for a foe with no castable moves.
            // Keep legal zero-power moves, but never reintroduce an excluded damaging move.
            authored.stream().filter(move -> passive.contains(moveKey(move))).forEach(selected::add);
        }
        moveset.clear();
        int slot = 0;
        for (var move : selected) moveset.setMove(slot++, move.create());
        GymCatalog.LOG.info("[ArenaMoves] {} {} Lv.{}: {}{}", context, pokemon.getSpecies().getName(),
                pokemon.getLevel(), selected.stream().map(MoveTemplate::getName).toList(),
                basicAttacksOnly ? " (RTC basic attacks)" : "");
    }

    public static boolean isArenaAttack(MoveTemplate move) {
        var info = MoveRegistry.info(move.create());
        return hasDamage(info) && !REACTIVE_MOVES.contains(info.id()) && !info.selfdestruct() && !info.ohko()
                && BRAWL_SHAPES.contains(MoveRegistry.style(info).shape());
    }

    static boolean isRangedAttack(MoveTemplate move) {
        if (!isArenaAttack(move)) return false;
        var style = MoveRegistry.style(MoveRegistry.info(move.create()));
        return switch (style.shape()) {
            case PROJECTILE, BEAM, STREAM, BOLT, STRIKE, RAIN, SHOCKWAVE -> Math.max(style.range(), style.radius()) >= 4;
            default -> false;
        };
    }

    /** Movie casts must reach a distant target without bringing the attacker into contact. */
    public static boolean isCinemaRangedAttack(MoveTemplate move) {
        if (!isArenaAttack(move)) return false;
        var style = MoveRegistry.style(MoveRegistry.info(move.create()));
        return switch (style.shape()) {
            case PROJECTILE, BEAM, STREAM, BOLT, STRIKE, RAIN -> style.range() >= 8;
            default -> false;
        };
    }

    private static boolean hasDamage(MoveInfo info) {
        return info != null && !info.isStatus() && Double.isFinite(info.power()) && info.power() > 0;
    }

    private static String moveKey(MoveTemplate move) { return move.getName().toLowerCase(Locale.ROOT); }

    private static ScoredMove score(Pokemon pokemon, MoveTemplate move, MoveInfo info, MoveStyle style) {
        double score = Math.min(info.power(), 120) * (info.accuracy() < 0 ? 1 : Math.max(.3, Math.min(1, info.accuracy() / 100.0)));
        var form = pokemon.getForm();
        if (move.getElementalType().equals(form.getPrimaryType()) || move.getElementalType().equals(form.getSecondaryType())) score += 24;
        score += switch (style.shape()) {
            case MELEE -> -5;
            case DASH -> 15;
            case STRIKE, RAIN, SHOCKWAVE -> 22;
            default -> 0;
        };
        if (style.chargeTicks() > 0) score += 12;
        return new ScoredMove(move, style.shape(), score);
    }

    private record ScoredMove(MoveTemplate template, MoveStyle.Shape shape, double score) {}

    /** The former core's standard Champion team, adapted to detached real-time Pokémon. */
    public static List<Pokemon> champion() {
        var fast = new TrainingStats(0, 252, 4, 0, 0, 252);
        return List.of(
                champion("zapdos", "GENDERLESS", "timid", "static", "sitrus_berry", new TrainingStats(31, 31, 31, 0, 0, 31),
                        new TrainingStats(0, 252, 0, 0, 0, 252), "thunderbolt", "tailwind", "airslash", "heatwave"),
                champion("gallade", null, "jolly", "justified", "mega_showdown:galladite", new TrainingStats(31, 31, 31, 0, 0, 31),
                        fast, "closecombat", "earthquake", "leafblade", "aquacutter"),
                champion("porygon2", "GENDERLESS", "modest", "analytic", "eviolite", new TrainingStats(31, 0, 31, 31, 31, 0),
                        new TrainingStats(252, 0, 0, 252, 4, 0), "thunderbolt", "triattack", "psychic", "shadowball"),
                champion("silvally", "GENDERLESS", "jolly", "rkssystem", "life_orb", new TrainingStats(31, 31, 31, 0, 0, 31),
                        fast, "multiattack", "uturn", "crunch", "dragonclaw"),
                champion("snorlax", null, "careful", "thickfat", "leftovers", new TrainingStats(31, 31, 31, 0, 31, 0),
                        new TrainingStats(252, 88, 168, 0, 0, 0), "rest", "sleeptalk", "bodyslam", "earthquake"),
                champion("dragonite", null, "jolly", "multiscale", "weakness_policy", new TrainingStats(31, 31, 31, 0, 0, 31),
                        fast, "dragondance", "extremespeed", "earthquake", "dragonclaw"));
    }

    private static Pokemon champion(String species, String gender, String nature, String ability, String item,
                                    TrainingStats ivs, TrainingStats evs, String... moves) {
        var pokemon = create(species, gender, 80, nature, ability, new LinkedHashSet<>(List.of(moves)), ivs, evs, item, Set.of(), "champion");
        pokemon.heal();
        return pokemon;
    }

    public static List<Pokemon> jessie() {
        return List.of(rocket("ekans", "adamant", "jolly", FAST, "glare", "bite", "poisonsting", "screech"),
                rocket("seviper", "adamant", "jolly", FAST, "poisontail", "poisonjab", "sludgebomb", "crunch"));
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
            if (template == null) {
                GymCatalog.LOG.warn("[TrainerTeam] Skipping unknown move '{}' for {}/{}", move, context, species);
                continue;
            }
            pokemon.getMoveSet().setMove(slot++, template.create());
        }
        return pokemon;
    }

    private static ResourceLocation cobblemonId(String key) {
        return key.indexOf(':') == -1 ? ResourceLocation.fromNamespaceAndPath("cobblemon", key) : ResourceLocation.parse(key);
    }
}
