package com.p1nero.cobblemon.rumblecore.gym;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaTheme;
import com.p1nero.cobblemon.rumblecore.prologue.MegaIntroduction;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.misc.custom_instances.api.InstanceApi;
import com.p1nero.misc.custom_instances.instance.InstanceDefinition;
import com.p1nero.misc.custom_instances.instance.reward.RewardDefinition;
import com.p1nero.misc.custom_instances.storage.DefinitionStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

public final class MegaTrials {
    public static final String TABLE = "cobblemon_rumble_core/mega_challenges.tsv";
    public static final ResourceLocation PORTRAIT = PrologueWorld.id("mega_portrait");
    public static final String PREFIX = "rumble_mega_";
    private static Map<String, Trial> trials = Map.of();

    public record Trial(String id, String species, String form, String baseAspect, int level, String nature,
                        GymTeamPokemon.TrainingStats evs, List<String> moves, ResourceLocation stone, int coins) {
        public String speciesKey() { return "cobblemon.species." + species + ".name"; }
        public String title() { return "{" + speciesKey() + "} {rumble.mega.form." + form + "}"; }
        public Set<String> aspects() { return baseAspect.equals("-") ? Set.of(form) : Set.of(baseAspect, form); }
        public boolean supported() {
            var mon = PokemonSpecies.getByIdentifier(ResourceLocation.fromNamespaceAndPath("cobblemon", species));
            return mon != null && mon.getForms().stream().anyMatch(f -> f.getAspects().containsAll(aspects()));
        }
        public ArenaTheme arena() {
            var mon = PokemonSpecies.getByIdentifier(ResourceLocation.fromNamespaceAndPath("cobblemon", species));
            if (mon == null) return ArenaTheme.DEFAULT;
            var requiredAspects = aspects();
            var formData = mon.getForms().stream().filter(candidate -> candidate.getAspects().containsAll(requiredAspects))
                    .findFirst().orElse(null);
            if (formData == null) return ArenaTheme.DEFAULT;
            var primary = ArenaTheme.type(formData.getPrimaryType().getName());
            if (primary != ArenaTheme.DEFAULT) return primary;
            var secondary = formData.getSecondaryType();
            return secondary == null ? ArenaTheme.DEFAULT : ArenaTheme.type(secondary.getName());
        }
        public GymCatalog.Actor actor() { return new GymCatalog.Actor("mega_simulator", BlockPos.ZERO, () -> List.of(pokemon())); }
        private Pokemon pokemon() {
            return GymTeamPokemon.create(species, null, level, nature, null, new LinkedHashSet<>(moves),
                    new GymTeamPokemon.TrainingStats(31, 31, 31, 31, 31, 31), evs, "", aspects(), id);
        }
    }

    private MegaTrials() { }

    public static void install(Path table) throws IOException {
        if (!ModList.get().isLoaded("mega_showdown")) { trials = Map.of(); return; }
        var store = new DefinitionStore(DefinitionStore.configRoot().resolve("definitions"));
        var gym = DefinitionStore.fromJson(com.google.gson.JsonParser.parseString(Files.readString(
                DefinitionStore.configRoot().resolve("definitions/rumble_gym_rock.json"))).getAsJsonObject());
        var next = new LinkedHashMap<String, Trial>();
        var lines = Files.readAllLines(table);
        for (int i = 1; i < lines.size(); i++) {
            var line = lines.get(i);
            if (line.isBlank() || line.startsWith("#")) continue;
            try {
                String[] columns = line.split("\t", -1);
                int[] ev = java.util.Arrays.stream(columns[6].split(",")).mapToInt(Integer::parseInt).toArray();
                var trial = new Trial(PREFIX + columns[0], columns[1], columns[2], columns[3], Integer.parseInt(columns[4]), columns[5],
                        new GymTeamPokemon.TrainingStats(ev[0], ev[1], ev[2], ev[3], ev[4], ev[5]), List.of(columns[7].split(",")),
                        ResourceLocation.parse(columns[8]), Integer.parseInt(columns[9]));
                if (next.containsKey(trial.id()) || trial.level() < 1 || trial.level() > 100 || trial.coins() <= 0
                        || trial.moves().size() != 4 || Set.copyOf(trial.moves()).size() != 4 || ev.length != 6
                        || java.util.Arrays.stream(ev).anyMatch(v -> v < 0 || v > 252) || java.util.Arrays.stream(ev).sum() > 510
                        || !BuiltInRegistries.ITEM.containsKey(trial.stone())) throw new IllegalArgumentException("invalid roster row");
                var definition = new InstanceDefinition(trial.id(), trial.title(), 0, 1, false, 0,
                        gym.spawn(), InstanceDefinition.VictoryType.CUSTOM, trial.id(), "", trial.arena().available().chunks())
                        .withDimensionSettings(gym.dimensionSettings())
                        .withPresentation(trial.stone().toString(), "", "");
                store.save(definition.withRewards(List.of(RewardDefinition.item(trial.stone().toString(), 1, ""),
                        RewardDefinition.custom("{rumble.mega.reward} " + trial.coins(),
                                "cobbledollars give {player} " + trial.coins()))));
                next.put(trial.id(), trial);
                ArenaMatch.registerMegaTrial(trial);
                InstanceApi.registerLobbyPresentation(trial.id(), player -> new InstanceApi.LobbyPresentation(
                        trial.title(), null, false));
            } catch (RuntimeException exception) {
                throw new IOException("Bad Mega challenge at " + table + ":" + (i + 1), exception);
            }
        }
        trials = java.util.Collections.unmodifiableMap(next);
    }

    public static List<String> ids() { return trials.values().stream().filter(Trial::supported).map(Trial::id).toList(); }
    public static Trial get(String id) { return trials.get(id); }

    public static boolean unlocked(ServerPlayer player) {
        return ModList.get().isLoaded("mega_showdown") && ArcQuestPlayerManager.getOrCreate(player).hasFlag(MegaIntroduction.UNLOCKED);
    }
}
