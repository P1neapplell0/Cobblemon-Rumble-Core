package com.p1nero.cobblemon.rumblecore.gym;

import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaTheme;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.misc.custom_instances.api.InstanceApi;
import com.p1nero.misc.custom_instances.instance.InstanceDefinition;
import com.p1nero.misc.custom_instances.instance.reward.RewardDefinition;
import com.p1nero.misc.custom_instances.storage.DefinitionStore;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Shared, immutable encounter data. The same roster drives quest, portrait and arena deployment. */
public final class LegendaryTrials {
    public static final String PREFIX = "rumble_legendary_";
    private static final Map<String, Trial> TRIALS = load();
    private static final GymTeamPokemon.TrainingStats PERFECT = new GymTeamPokemon.TrainingStats(31, 31, 31, 31, 31, 31);
    private static final GymTeamPokemon.TrainingStats ZERO = new GymTeamPokemon.TrainingStats(0, 0, 0, 0, 0, 0);

    private LegendaryTrials() {}

    public record Foe(String species, String aspect, int level, String nature, List<String> moves) {
        public ResourceLocation speciesId() { return ResourceLocation.fromNamespaceAndPath("cobblemon", species); }
        public Set<String> aspects() { return aspect.equals("-") ? Set.of() : Set.of(aspect); }
        public String speciesKey() { return "cobblemon.species." + species + ".name"; }
        boolean supported() {
            var data = PokemonSpecies.getByIdentifier(speciesId());
            return data != null && (aspect.equals("-") || data.getForms().stream().anyMatch(f -> f.getAspects().contains(aspect)))
                    && moves.stream().allMatch(m -> Moves.getByName(m) != null);
        }
        public Pokemon create(String trialId) {
            var mon = GymTeamPokemon.create(species, null, level, nature, null, new LinkedHashSet<>(moves), PERFECT, ZERO, "", aspects(), trialId);
            GymTeamPokemon.prepareArenaMoves(mon, trialId, true);
            mon.setCurrentHealth(mon.getMaxHealth());
            return mon;
        }
    }

    public record Trial(String id, int generation, ArenaTheme arena, List<Foe> foes) {
        public String template() { return PREFIX + id; }
        public ResourceLocation quest() { return PrologueWorld.id("legendary/" + id); }
        public String titleKey() { return "rumble.legendary.trial." + id; }
        public boolean supported() { return foes.stream().allMatch(Foe::supported); }
    }

    public static List<Trial> all() { return List.copyOf(TRIALS.values()); }
    public static Trial get(String id) { return TRIALS.get(id); }
    public static Trial template(String template) { return template.startsWith(PREFIX) ? get(template.substring(PREFIX.length())) : null; }

    private static Map<String, Trial> load() {
        var building = new LinkedHashMap<String, Trial>();
        var table = Objects.requireNonNull(LegendaryTrials.class.getResourceAsStream("/data/cobblemon_rumble_core/legendary_trials.tsv"));
        try (var reader = new BufferedReader(new InputStreamReader(table, StandardCharsets.UTF_8))) {
            reader.readLine();
            int row = 1;
            for (String line; (line = reader.readLine()) != null;) {
                row++;
                if (line.isBlank() || line.startsWith("#")) continue;
                try {
                    String[] c = line.split("\t", -1);
                    int gen = Integer.parseInt(c[1]), level = Integer.parseInt(c[5]);
                    var arena = ArenaTheme.valueOf(c[2].toUpperCase(Locale.ROOT));
                    var moves = List.of(c[7].split(","));
                    if (!c[0].matches("[a-z0-9_]+") || gen < 1 || gen > 9 || level < 1 || level > 100
                            || moves.size() != 4 || Set.copyOf(moves).size() != 4) throw new IllegalArgumentException("invalid roster");
                    var trial = building.computeIfAbsent(c[0], id -> new Trial(id, gen, arena, new ArrayList<>()));
                    var foe = new Foe(c[3], c[4], level, c[6], moves);
                    if (trial.generation() != gen || trial.arena() != arena || trial.foes().size() >= 4
                            || trial.foes().stream().anyMatch(f -> f.species().equals(foe.species()) && f.aspect().equals(foe.aspect())))
                        throw new IllegalArgumentException("inconsistent encounter or duplicate foe");
                    trial.foes().add(foe);
                } catch (RuntimeException e) { throw new IllegalStateException("Invalid legendary_trials.tsv at row " + row, e); }
            }
        } catch (IOException e) { throw new IllegalStateException("Cannot read legendary_trials.tsv", e); }
        building.replaceAll((id, trial) -> new Trial(id, trial.generation(), trial.arena(), List.copyOf(trial.foes())));
        return Collections.unmodifiableMap(building);
    }

    public static void install(DefinitionStore store, InstanceDefinition base) throws IOException {
        for (var trial : all()) {
            var title = "{" + trial.titleKey() + "}";
            var definition = new InstanceDefinition(trial.template(), title, 0, 1, false, 0, base.spawn(),
                    InstanceDefinition.VictoryType.CUSTOM, trial.template(), "", trial.arena().available().chunks())
                    .withDimensionSettings(base.dimensionSettings())
                    .withPresentation("cobblemon:master_ball", "", "{rumble.legendary.objective}");
            int coins = 50_000 + (trial.foes().size() - 1) * 30_000;
            definition = definition.withRewards(List.of(RewardDefinition.custom("{rumble.mega.reward} " + coins,
                    "cobbledollars give @s " + coins)));
            store.save(definition);
            ArenaMatch.registerLegendaryTrial(trial);
            InstanceApi.registerLobbyPresentation(trial.template(), p -> new InstanceApi.LobbyPresentation(title, null, false));
        }
    }
}
