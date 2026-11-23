package com.p1nero.cobblemon.rumblecore.gym;

import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaIntro;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaTheme;
import com.p1nero.misc.custom_instances.api.InstanceApi;
import com.p1nero.misc.custom_instances.instance.InstanceDefinition;
import com.p1nero.misc.custom_instances.instance.reward.RewardDefinition;
import com.p1nero.misc.custom_instances.storage.DefinitionStore;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class CinemaTrials {
    private static final List<Trial> TRIALS = load();
    private static final GymTeamPokemon.TrainingStats IVS = new GymTeamPokemon.TrainingStats(31, 31, 31, 31, 31, 31);
    private static final GymTeamPokemon.TrainingStats EVS = new GymTeamPokemon.TrainingStats(144, 92, 48, 92, 48, 84);

    private CinemaTrials() {}

    public record Cast(String species, String aspect, List<String> moves) {
        public ResourceLocation speciesId() { return ResourceLocation.fromNamespaceAndPath("cobblemon", species); }
        public Set<String> aspects() { return aspect.equals("-") ? Set.of() : Set.of(aspect); }
        public Component name() {
            Component base = Component.translatable("cobblemon.species." + species + ".name");
            return aspect.equals("-") ? base : Component.translatable("rumble.cinema.form." + aspect, base);
        }
        public ArenaIntro.Portrait portrait() {
            return ArenaIntro.Portrait.pokemon(speciesId(), name(), List.copyOf(aspects()));
        }
        public boolean supported() {
            var data = PokemonSpecies.getByIdentifier(speciesId());
            return data != null && (aspects().isEmpty() || data.getForms().stream().anyMatch(f -> f.getAspects().containsAll(aspects())))
                    && moves.stream().allMatch(move -> Moves.getByName(move) != null);
        }
        public Pokemon create(String context) {
            var pokemon = GymTeamPokemon.create(species, null, 80, "hardy", null, new LinkedHashSet<>(moves),
                    IVS, EVS, "", aspects(), context);
            pokemon.heal();
            return pokemon;
        }
    }

    public record Trial(String id, ArenaTheme arena, int coins, int candies, List<List<Cast>> sides) {
        public String titleKey() { return "rumble.cinema.trial." + id; }
        public String template() { return "rumble_cinema_" + id; }
        public int capacity() { return sides.stream().mapToInt(List::size).max().orElse(1); }
        public static Trial fromTemplate(String template) {
            return all().stream().filter(trial -> trial.template().equals(template)).findFirst().orElse(null);
        }
        public boolean supported() { return sides.stream().flatMap(List::stream).allMatch(Cast::supported); }
        public Component sideName(int side) {
            var result = Component.empty();
            for (var cast : sides.get(side)) {
                if (!result.getSiblings().isEmpty()) result.append(" / ");
                result.append(cast.name());
            }
            return result;
        }
    }

    public static List<Trial> all() { return TRIALS; }
    public static Trial get(String id) { return all().stream().filter(t -> t.id().equals(id)).findFirst().orElse(null); }

    private static List<Trial> load() {
        var result = new ArrayList<Trial>();
        try (var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(CinemaTrials.class
                .getResourceAsStream("/data/cobblemon_rumble_core/cinema_trials.tsv")), StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) {
                if (line.isBlank() || line.startsWith("#")) continue;
                var columns = line.split("\t", -1);
                var sides = Arrays.stream(columns[4].split("\\|")).map(side -> Arrays.stream(side.split(";")).map(slot -> {
                    var fields = slot.split(",");
                    if (fields.length != 6) throw new IllegalArgumentException("Invalid cinema cast: " + slot);
                    return new Cast(fields[0], fields[1], List.of(fields[2], fields[3], fields[4], fields[5]));
                }).toList()).toList();
                if (sides.size() < 2 || sides.size() > 3 || sides.stream().anyMatch(side -> side.isEmpty() || side.size() > 3)
                        || result.stream().anyMatch(trial -> trial.id().equals(columns[0])))
                    throw new IllegalArgumentException("Invalid cinema matchup: " + columns[0]);
                result.add(new Trial(columns[0], ArenaTheme.valueOf(columns[1].toUpperCase(Locale.ROOT)),
                        Integer.parseInt(columns[2]), Integer.parseInt(columns[3]), sides));
            }
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read cinema trials", error);
        }
        return List.copyOf(result);
    }

    public static void install(DefinitionStore store, InstanceDefinition base) throws IOException {
        for (var trial : all()) {
            String id = trial.template();
            String title = "{" + trial.titleKey() + "}";
            // The native room form takes a capacity up to definition.maxPlayers() - 1.
            var definition = new InstanceDefinition(id, title, 0, 1, false, trial.capacity() + 1, base.spawn(),
                    InstanceDefinition.VictoryType.CUSTOM, id, "", trial.arena().available().chunks())
                    .withDimensionSettings(base.dimensionSettings())
                    .withPresentation("minecraft:music_disc_otherside", "", "{rumble.cinema.rules}")
                    .withRewards(List.of(RewardDefinition.custom("{rumble.mega.reward} " + trial.coins(),
                            "cobbledollars give @s " + trial.coins()),
                            RewardDefinition.item("cobblemon:exp_candy_xl", trial.candies(), "")));
            store.save(definition);
            ArenaMatch.registerCinemaTrial(trial);
            InstanceApi.registerLobbyPresentation(id, player -> new InstanceApi.LobbyPresentation(title, null, false));
        }
    }
}
