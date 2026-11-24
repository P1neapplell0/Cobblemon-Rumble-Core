package com.p1nero.cobblemon.rumblecore.combat.arena.frontier;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Battle Frontier rules shared by the roster editor and the authoritative room service. */
public final class FrontierRoster {
    public static final int TEAM_SIZE = 3, MOVE_SLOTS = 4;
    public static final ResourceLocation NONE = ResourceLocation.withDefaultNamespace("air");
    public static final Pick EMPTY = new Pick(NONE, NONE, List.of("", "", "", ""));
    private static final ResourceLocation STRUGGLE = ResourceLocation.fromNamespaceAndPath("cobblemon", "struggle");
    private static final TagKey<Item> HELD = tag("cobblemon", "held/is_held_item");
    private static final TagKey<Item> WHITELIST = tag("cobblemon", "held/whitelisted_items_to_hold");
    private static final TagKey<Item> BLACKLIST = tag("cobblemon", "held/blacklisted_items_to_hold");
    private static final TagKey<Item> MEGA = tag("mega_showdown", "mega_stone");
    private static final int[] GENERATION_END = {151, 251, 386, 493, 649, 721, 809, 905, 1025};
    private static final List<Stats> STATS = List.of(Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK,
            Stats.SPECIAL_DEFENCE, Stats.SPEED);

    private FrontierRoster() {}

    public record Pick(ResourceLocation species, ResourceLocation held, List<String> moves) {
        public Pick { moves = List.copyOf(moves); }
    }

    public enum Issue { NONE, TEAM_SIZE, PICK_SPECIES, REPEAT_SPECIES, HELD_ITEM, MEGA_LIMIT, PICK_MOVES, ILLEGAL_MOVE, REPEAT_MOVE }

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    public static Species species(ResourceLocation id) {
        if (NONE.equals(id)) return null;
        var species = PokemonSpecies.getByIdentifier(id);
        return species != null && species.getImplemented() ? species : null;
    }

    public static int generation(Species species) {
        int number = species.getNationalPokedexNumber();
        if (number <= 0) return 0;
        for (int i = 0; i < GENERATION_END.length; i++) if (number <= GENERATION_END[i]) return i + 1;
        return 0;
    }

    public static boolean isMega(ItemStack item) { return item.is(MEGA); }

    private static boolean mayHold(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(BLACKLIST) && (stack.is(HELD) || stack.is(WHITELIST) || isMega(stack));
    }

    public static List<ResourceLocation> heldItems() {
        return BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR && mayHold(item.getDefaultInstance()))
                .map(BuiltInRegistries.ITEM::getKey).sorted().toList();
    }

    public static Set<MoveTemplate> legalMoves(Species species) {
        return species == null ? Set.of() : species.getStandardForm().getMoves().getAllLegalMoves();
    }

    private static MoveTemplate legalMove(Species species, String name) {
        if (STRUGGLE.getPath().equals(name)) return null;
        for (var move : legalMoves(species)) if (move.getName().equals(name)) return move;
        return null;
    }

    public static Pick initialPick(Species species) {
        // Dedicated servers do not sync moveset builders. Seed the editor from the synced learnset.
        var learnset = species.getStandardForm().getMoves();
        var levelUp = new LinkedHashSet<MoveTemplate>();
        learnset.getLevelUpMoves().entrySet().stream().filter(e -> e.getKey() <= 100)
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .forEach(e -> levelUp.addAll(e.getValue().reversed()));
        var legal = legalMoves(species);
        var names = new LinkedHashSet<String>();
        levelUp.stream().filter(legal::contains).filter(move -> !STRUGGLE.getPath().equals(move.getName()))
                .filter(move -> move.getPower() > 0).findFirst().ifPresent(move -> names.add(move.getName()));
        levelUp.stream().filter(legal::contains).map(MoveTemplate::getName)
                .filter(name -> !STRUGGLE.getPath().equals(name)).forEach(names::add);
        if (names.isEmpty()) legal.stream().map(MoveTemplate::getName)
                .filter(name -> !STRUGGLE.getPath().equals(name)).sorted().forEach(names::add);
        var moves = new ArrayList<>(names.stream().limit(MOVE_SLOTS).toList());
        while (moves.size() < MOVE_SLOTS) moves.add("");
        return new Pick(species.getResourceIdentifier(), NONE, moves);
    }

    /** A ready team has one to three populated slots; empty slots are not combatants. */
    public static Issue validate(List<Pick> picks, boolean complete) {
        if (picks.size() != TEAM_SIZE) return Issue.TEAM_SIZE;
        var speciesUsed = new HashSet<ResourceLocation>();
        int mega = 0;
        for (var pick : picks) {
            if (pick.equals(EMPTY)) continue;
            var species = species(pick.species());
            if (species == null) return Issue.PICK_SPECIES;
            if (!speciesUsed.add(pick.species())) return Issue.REPEAT_SPECIES;
            if (!NONE.equals(pick.held())) {
                var item = BuiltInRegistries.ITEM.getOptional(pick.held()).orElse(Items.AIR).getDefaultInstance();
                if (!mayHold(item)) return Issue.HELD_ITEM;
                if (isMega(item) && ++mega > 1) return Issue.MEGA_LIMIT;
            }
            if (pick.moves().size() != MOVE_SLOTS) return Issue.PICK_MOVES;
            var moves = new HashSet<String>();
            for (String name : pick.moves()) {
                if (name.isEmpty()) continue;
                if (legalMove(species, name) == null) return Issue.ILLEGAL_MOVE;
                if (!moves.add(name)) return Issue.REPEAT_MOVE;
            }
            // Species such as Ditto have fewer than four legal moves. Leave unused slots empty.
            if (complete && moves.isEmpty()) return Issue.PICK_MOVES;
        }
        return complete && speciesUsed.isEmpty() ? Issue.TEAM_SIZE : Issue.NONE;
    }

    static List<Pokemon> snapshotParty(ServerPlayer player) {
        return Cobblemon.INSTANCE.getStorage().getParty(player).toGappyList().stream()
                .filter(Objects::nonNull).limit(TEAM_SIZE)
                .map(pokemon -> pokemon.clone(true, player.registryAccess())).toList();
    }

    static Pick describe(Pokemon pokemon) {
        var moves = new ArrayList<String>(MOVE_SLOTS);
        for (int i = 0; i < MOVE_SLOTS; i++) {
            var move = pokemon.getMoveSet().get(i);
            moves.add(move == null ? "" : move.getName());
        }
        return new Pick(pokemon.getSpecies().getResourceIdentifier(), BuiltInRegistries.ITEM.getKey(pokemon.heldItem().getItem()), moves);
    }

    static List<Pokemon> copyForMatch(ServerPlayer player, List<Pokemon> snapshot) {
        return snapshot.stream().map(source -> {
            var copy = source.clone(true, player.registryAccess());
            copy.setTetheringId(null);
            copy.heal();
            return copy;
        }).toList();
    }

    public static List<Pokemon> create(List<Pick> picks) {
        var issue = validate(picks, true);
        if (issue != Issue.NONE) throw new IllegalArgumentException("Invalid Frontier lineup: " + issue);
        var result = new ArrayList<Pokemon>(TEAM_SIZE);
        for (var pick : picks) {
            if (pick.equals(EMPTY)) continue;
            var species = species(pick.species());
            var pokemon = species.create(100);
            for (var stat : STATS) {
                pokemon.setIV(stat, 31);
                pokemon.setEV(stat, 0);
            }
            if (!NONE.equals(pick.held())) {
                pokemon.setHeldItem$common(BuiltInRegistries.ITEM.getOptional(pick.held()).orElseThrow().getDefaultInstance());
            }
            pokemon.getMoveSet().clear();
            for (var name : pick.moves()) {
                if (!name.isEmpty()) pokemon.getMoveSet().add(legalMove(species, name).create());
            }
            pokemon.heal();
            result.add(pokemon);
        }
        return List.copyOf(result);
    }
}
