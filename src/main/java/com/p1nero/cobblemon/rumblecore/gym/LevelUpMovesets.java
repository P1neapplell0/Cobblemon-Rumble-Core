package com.p1nero.cobblemon.rumblecore.gym;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.Pokemon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Selects competitive moves strictly from the current form's level-up learnset. */
public final class LevelUpMovesets {

    private static final Map<String, Integer> SUPPORT_PRIORITY = Map.ofEntries(
            Map.entry("quiverdance", 100), Map.entry("shellsmash", 100),
            Map.entry("dragondance", 98), Map.entry("swordsdance", 96),
            Map.entry("nastyplot", 96), Map.entry("calmmind", 94),
            Map.entry("bellydrum", 92), Map.entry("cottonguard", 92),
            Map.entry("recover", 92), Map.entry("roost", 92),
            Map.entry("strengthsap", 92), Map.entry("synthesis", 92),
            Map.entry("moonlight", 92), Map.entry("morningsun", 92),
            Map.entry("shoreup", 92), Map.entry("softboiled", 92),
            Map.entry("slackoff", 92), Map.entry("milkdrink", 92),
            Map.entry("wish", 90), Map.entry("spore", 90),
            Map.entry("willowisp", 88), Map.entry("thunderwave", 88),
            Map.entry("glare", 88), Map.entry("toxic", 86),
            Map.entry("leechseed", 84), Map.entry("tailwind", 82),
            Map.entry("stickyweb", 82), Map.entry("auroraveil", 82),
            Map.entry("stealthrock", 80), Map.entry("toxicspikes", 80),
            Map.entry("spikes", 80), Map.entry("taunt", 80),
            Map.entry("encore", 80), Map.entry("destinybond", 78),
            Map.entry("counter", 78), Map.entry("mirrorcoat", 78),
            Map.entry("protect", 74), Map.entry("reflect", 72),
            Map.entry("lightscreen", 72), Map.entry("hypnosis", 70),
            Map.entry("confuseray", 68), Map.entry("screech", 66),
            Map.entry("agility", 64), Map.entry("growth", 62),
            Map.entry("harden", 45), Map.entry("sandattack", 42),
            Map.entry("leer", 38), Map.entry("growl", 36));
    private static final Set<String> LOW_VALUE_ATTACKS = Set.of(
            "explosion", "selfdestruct", "mistyexplosion", "memento",
            "finalgambit", "hyperbeam", "gigaimpact", "frenzyplant",
            "blastburn", "hydrocannon", "steelbeam");

    private LevelUpMovesets() {
    }

    public static void apply(Pokemon pokemon, String context) {
        apply(pokemon, context, Integer.MAX_VALUE);
    }

    public static void apply(Pokemon pokemon, String context, int maxMovePower) {
        Set<MoveTemplate> legalMoves = pokemon.getForm().getMoves()
                .getLevelUpMovesUpTo(pokemon.getLevel());
        if (legalMoves.isEmpty()) {
            pokemon.getMoveSet().clear();
            GymCatalog.LOG.warn("[LevelUpMoveset] {} has no level-up moves at Lv.{}; "
                            + "leaving the moveset empty",
                    context, pokemon.getLevel());
            return;
        }

        List<MoveTemplate> candidates = legalMoves.stream()
                .filter(move -> move.getPower() <= maxMovePower)
                .sorted(Comparator.comparingDouble(
                                (MoveTemplate move) -> score(pokemon, move)).reversed()
                        .thenComparing(MoveTemplate::getName))
                .toList();
        List<MoveTemplate> selected = new ArrayList<>(4);
        Set<String> selectedNames = new HashSet<>();
        Set<ElementalType> selectedAttackTypes = new HashSet<>();

        addBestStab(candidates, selected, selectedNames, pokemon.getForm().getPrimaryType());
        ElementalType secondary = pokemon.getForm().getSecondaryType();
        if (secondary != null) addBestStab(candidates, selected, selectedNames, secondary);
        selected.stream().map(MoveTemplate::getElementalType).forEach(selectedAttackTypes::add);

        candidates.stream()
                .filter(move -> move.getPower() <= 0)
                .filter(move -> SUPPORT_PRIORITY.getOrDefault(move.getName(), 0) >= 60)
                .filter(move -> selectedNames.add(move.getName()))
                .findFirst().ifPresent(selected::add);

        while (selected.size() < 4) {
            MoveTemplate best = candidates.stream()
                    .filter(move -> !selectedNames.contains(move.getName()))
                    .filter(move -> move.getPower() > 0
                            || SUPPORT_PRIORITY.getOrDefault(move.getName(), 0) >= 80)
                    .max(Comparator.comparingDouble(move -> selectionScore(
                            pokemon, move, selectedAttackTypes)))
                    .orElse(null);
            if (best == null) break;
            selected.add(best);
            selectedNames.add(best.getName());
            if (best.getPower() > 0) selectedAttackTypes.add(best.getElementalType());
        }
        for (MoveTemplate move : candidates) {
            if (selected.size() >= 4) break;
            if (move.getPower() > 0 && selectedNames.add(move.getName())) {
                selected.add(move);
            }
        }
        for (MoveTemplate move : candidates) {
            if (selected.size() >= 4) break;
            if (selectedNames.add(move.getName())) selected.add(move);
        }

        pokemon.getMoveSet().clear();
        selected.forEach(move -> pokemon.getMoveSet().add(move.create()));
        pokemon.getMoveSet().heal();
        GymCatalog.LOG.debug("[LevelUpMoveset] {} Lv.{} -> {}", context,
                pokemon.getLevel(), selected.stream().map(MoveTemplate::getName).toList());
    }

    private static void addBestStab(List<MoveTemplate> candidates, List<MoveTemplate> selected,
                                    Set<String> selectedNames, ElementalType type) {
        candidates.stream()
                .filter(move -> move.getPower() > 0 && type.equals(move.getElementalType()))
                .filter(move -> selectedNames.add(move.getName()))
                .findFirst().ifPresent(selected::add);
    }

    private static double score(Pokemon pokemon, MoveTemplate move) {
        if (move.getPower() <= 0) return SUPPORT_PRIORITY.getOrDefault(move.getName(), -1000);
        double accuracy = move.getAccuracy() <= 0 ? 100.0 : Math.min(100.0, move.getAccuracy());
        double result = move.getPower() * accuracy / 100.0 + move.getPriority() * 8.0;
        if (move.getElementalType().equals(pokemon.getForm().getPrimaryType())
                || move.getElementalType().equals(pokemon.getForm().getSecondaryType())) {
            result += 30.0;
        }

        int attack = pokemon.getStat(Stats.ATTACK);
        int specialAttack = pokemon.getStat(Stats.SPECIAL_ATTACK);
        String category = move.getDamageCategory().getName();
        if ("physical".equalsIgnoreCase(category)) {
            result += attack >= specialAttack ? 12.0 : -8.0;
        } else if ("special".equalsIgnoreCase(category)) {
            result += specialAttack >= attack ? 12.0 : -8.0;
        }
        if (LOW_VALUE_ATTACKS.contains(move.getName())) result -= 90.0;
        return result;
    }

    private static double selectionScore(Pokemon pokemon, MoveTemplate move,
                                         Set<ElementalType> selectedAttackTypes) {
        double result = score(pokemon, move);
        if (move.getPower() > 0 && !selectedAttackTypes.contains(move.getElementalType())) {
            result += 30.0;
        }
        return result;
    }
}
