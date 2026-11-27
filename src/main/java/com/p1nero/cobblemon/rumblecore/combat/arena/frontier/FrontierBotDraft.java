package com.p1nero.cobblemon.rumblecore.combat.arena.frontier;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import net.minecraft.resources.ResourceLocation;
import com.johnn.realtimecobblemon.TypeChart;
import com.johnn.realtimecobblemon.moves.MoveRegistry;
import com.p1nero.cobblemon.rumblecore.gym.GymTeamPokemon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class FrontierBotDraft {
    // Pick from held items that the realtime practice AI actually knows how to use.
    private static final List<ResourceLocation> HEALING_ITEMS = List.of(
            ResourceLocation.fromNamespaceAndPath("cobblemon", "oran_berry"),
            ResourceLocation.fromNamespaceAndPath("cobblemon", "sitrus_berry"));
    private static final String[] CALLSIGNS = {"Comet", "Beryl", "Volt", "Nova", "Ember", "Flint", "Echo", "Aster", "Nimbus", "Mica"};
    private FrontierBotDraft() {}

    static String name() {
        var random = ThreadLocalRandom.current();
        return CALLSIGNS[random.nextInt(CALLSIGNS.length)] + "-" + random.nextInt(10, 100);
    }

    static FrontierRoster.Pick pick(List<FrontierRoster.Pick> draft, FrontierRoster.Pick opponent) {
        var used = new HashSet<>(draft.stream().map(FrontierRoster.Pick::species).toList());
        var candidates = new ArrayList<>(PokemonSpecies.getSpecies().stream().filter(Species::getImplemented)
                .filter(s -> !used.contains(s.getResourceIdentifier())).toList());
        Collections.shuffle(candidates);
        var defender = FrontierRoster.species(opponent.species());
        List<String> defendingTypes;
        if (defender == null) defendingTypes = List.of();
        else if (FrontierRoster.heldGroup(opponent.held()) == FrontierRoster.HeldGroup.FORM) {
            var preview = defender.create(100);
            FrontierRoster.equip(preview, opponent.held());
            defendingTypes = types(preview.getForm());
        } else defendingTypes = types(defender.getStandardForm());
        record Option(FrontierRoster.Pick pick, double score) {}
        Option best = null;
        int viable = 0;
        for (var species : candidates) {
            var types = types(species.getStandardForm());
            var attacks = FrontierRoster.legalMoves(species).stream().filter(GymTeamPokemon::isArenaAttack)
                    .sorted(Comparator.comparingDouble((MoveTemplate m) -> {
                        var info = MoveRegistry.info(m.getName());
                        double matchup = defender == null ? 1 : TypeChart.multiplier(info.type(), defendingTypes);
                        return m.getPower() * (types.contains(info.type()) ? 1.5 : 1) * (0.4 + 0.6 * matchup)
                                * (info.accuracy() < 0 ? 1 : info.accuracy() / 100);
                    }).reversed()).toList();
            if (attacks.isEmpty()) continue;
            var names = new ArrayList<String>();
            attacks.stream().filter(GymTeamPokemon::isCinemaRangedAttack).findFirst().ifPresent(m -> names.add(m.getName()));
            var coverage = new HashSet<String>();
            for (var attack : attacks) {
                if (names.size() == FrontierRoster.MOVE_SLOTS) break;
                if (coverage.add(MoveRegistry.info(attack.getName()).type()) && !names.contains(attack.getName())) names.add(attack.getName());
            }
            for (var attack : attacks) {
                if (names.size() == FrontierRoster.MOVE_SLOTS) break;
                if (!names.contains(attack.getName())) names.add(attack.getName());
            }
            double score = defender == null ? 0 : names.stream().map(MoveRegistry::info)
                    .mapToDouble(info -> TypeChart.multiplier(info.type(), defendingTypes) * (types.contains(info.type()) ? 1.25 : 1))
                    .max().orElse(0) - 0.4 * defendingTypes.stream().mapToDouble(type -> TypeChart.multiplier(type, types)).max().orElse(1);
            while (names.size() < FrontierRoster.MOVE_SLOTS) names.add("");
            var option = new Option(new FrontierRoster.Pick(species.getResourceIdentifier(), FrontierRoster.NONE, names), score);
            if (best == null || score > best.score()) best = option;
            if (++viable >= (defender == null ? 1 : 32)) break;
        }
        if (best == null) return null;
        var items = FrontierRoster.heldItems().stream().filter(HEALING_ITEMS::contains).toList();
        var held = items.isEmpty() ? FrontierRoster.NONE : items.get(ThreadLocalRandom.current().nextInt(items.size()));
        return new FrontierRoster.Pick(best.pick().species(), held, best.pick().moves());
    }

    private static List<String> types(FormData form) {
        return form.getSecondaryType() == null ? List.of(form.getPrimaryType().getName())
                : List.of(form.getPrimaryType().getName(), form.getSecondaryType().getName());
    }
}
