package com.p1nero.cobblemon.rumblecore.shop;

import fr.harmex.cobbledollars.common.api.shop.Category;
import fr.harmex.cobbledollars.common.api.shop.Offer;
import fr.harmex.cobbledollars.common.api.shop.Shop;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** The old core's rare-stock pools, prices and weighted draw. */
public final class WanderingCatalogue {
    private WanderingCatalogue() { }

    private static final String WHITE_HOVERBOARD = "hoverboards:hoverboard_white";
    private static final String BLACK_HOVERBOARD = "hoverboards:hoverboard_black";
    private static final int HOVERBOARD_PRICE = 10000;
    private static final PricedItem REWIND_WATCH = item("structure_rewind_watch:structure_rewind_watch", 10_000);
    private static final List<PricedItem> GUARANTEED = List.of(
            item("horizoncobblemonitems:void_bottle_cap", 10000), item("horizoncobblemonitems:golden_bottle_cap", 500000),
            item("horizoncobblemonitems:silver_bottle_cap", 100000), item("artifacts:mimic_spawn_egg", 10000),
            item("horizoncobblemonitems:axiom_berry", 50000), item("horizoncobblemonitems:tutor_hidden_machine", 100000));
    private static final List<Draw> DRAWS = List.of(
            new Draw(2, variants("_carat", 100000, "red", "blue", "green", "yellow", "pink", "cyan")),
            new Draw(2, variants("_phoenix_feather", 50000, "health", "muscle", "resist", "genius", "clever", "swift")),
            new Draw(1, variants("_mac_and_cheese", 20000, "spicy", "dry", "sweet", "bitter", "sour")));
    private static final int MARKET_SIZE = 15;
    private static final int ULTRA_RARE_WEIGHT = 1;
    private static final int EXTREMELY_RARE_WEIGHT = 3;
    private static final int RARE_WEIGHT = 8;
    private static final int MEDIUM_WEIGHT = 16;
    private static final int COMMON_WEIGHT = 32;
    private static final int VERY_COMMON_WEIGHT = 64;

    public static Shop create(RandomSource random) {
        List<PricedItem> selected = new ArrayList<>();
        Set<String> selectedIds = new HashSet<>();

        GUARANTEED.subList(0, 4).forEach(item -> addUnique(selected, selectedIds, item));
        pick(selected, selectedIds, random, DRAWS.get(0).count(), DRAWS.get(0).pool());
        addUnique(selected, selectedIds, GUARANTEED.get(4));
        for (var draw : DRAWS.subList(1, DRAWS.size())) pick(selected, selectedIds, random, draw.count(), draw.pool());
        addUnique(selected, selectedIds, GUARANTEED.get(5));

        List<WeightedItem> pool = weightedBlackMarketPool();
        while (selected.size() < MARKET_SIZE && !pool.isEmpty()) {
            int totalWeight = pool.stream().mapToInt(WeightedItem::weight).sum();
            int roll = random.nextInt(totalWeight);
            int index = 0;
            for (; index < pool.size(); index++) {
                roll -= pool.get(index).weight();
                if (roll < 0) break;
            }
            WeightedItem candidate = pool.remove(Math.min(index, pool.size() - 1));
            addUnique(selected, selectedIds, candidate.item());
        }

        // Append hoverboards after the original 15 selections so their odds are unchanged.
        addUnique(selected, selectedIds, item(WHITE_HOVERBOARD, HOVERBOARD_PRICE));
        addUnique(selected, selectedIds, item(BLACK_HOVERBOARD, HOVERBOARD_PRICE));
        pick(selected, selectedIds, random, 1, randomHoverboardPool());

        addUnique(selected, selectedIds, REWIND_WATCH);

        ArrayList<Offer> offers = new ArrayList<>();
        selected.forEach(entry -> addIfPresent(offers, offer(entry.id(), entry.price())));
        return new Shop(List.of(new Category("rumble.merchant.category", offers)));
    }

    public record Odds(String item, int price, double fraction) {}
    private record Draw(int count, List<PricedItem> pool) {}
    private static List<Odds> cachedOdds;

    public static List<Odds> odds() {
        if (cachedOdds != null) return cachedOdds;
        var rows = new ArrayList<Odds>();
        int selected = disclose(rows, GUARANTEED, GUARANTEED.size());
        for (var draw : DRAWS) selected += disclose(rows, draw.pool(), draw.count());
        var pool = weightedBlackMarketPool();
        var byWeight = new LinkedHashMap<Integer, Integer>();
        for (var entry : pool) byWeight.merge(entry.weight(), 1, Integer::sum);
        var weights = byWeight.keySet().stream().mapToInt(Integer::intValue).toArray();
        var counts = byWeight.values().stream().mapToInt(Integer::intValue).toArray();
        int draws = Math.min(pool.size(), Math.max(0, MARKET_SIZE - selected));
        var expected = expectedDraws(weights, counts, draws);
        for (var entry : pool) {
            int i = 0;
            while (weights[i] != entry.weight()) i++;
            var item = entry.item();
            rows.add(new Odds(item.id(), item.price(), Math.min(1.0, expected[i] / counts[i])));
        }
        disclose(rows, List.of(item(WHITE_HOVERBOARD, HOVERBOARD_PRICE), item(BLACK_HOVERBOARD, HOVERBOARD_PRICE), REWIND_WATCH), 3);
        disclose(rows, randomHoverboardPool(), 1);
        return cachedOdds = List.copyOf(rows);
    }

    // Exact weighted sampling without replacement. Identical weights are interchangeable,
    // so each state contains just a four-bit draw count per weight (at most 15 draws).
    private static double[] expectedDraws(int[] weights, int[] counts, int draws) {
        Map<Long, Double> states = Map.of(0L, 1.0);
        for (int draw = 0; draw < draws; draw++) {
            Map<Long, Double> next = new HashMap<>();
            for (var state : states.entrySet()) {
                long key = state.getKey();
                int total = 0;
                for (int i = 0; i < counts.length; i++) total += (counts[i] - (int) (key >>> (4 * i) & 15)) * weights[i];
                for (int i = 0; i < counts.length; i++) {
                    int weight = (counts[i] - (int) (key >>> (4 * i) & 15)) * weights[i];
                    if (weight > 0) next.merge(key + (1L << (4 * i)), state.getValue() * weight / total, Double::sum);
                }
            }
            states = next;
        }
        double[] expected = new double[counts.length];
        for (var state : states.entrySet()) for (int i = 0; i < counts.length; i++)
            expected[i] += (state.getKey() >>> (4 * i) & 15) * state.getValue();
        return expected;
    }

    private static int disclose(List<Odds> output, List<PricedItem> pool, int count) {
        var available = pool.stream().filter(p -> isAvailable(p.id())).toList();
        int draws = Math.min(count, available.size());
        if (draws > 0) for (var item : available) output.add(new Odds(item.id(), item.price(), (double) draws / available.size()));
        return draws;
    }

    private static List<PricedItem> variants(String suffix, int price, String... variants) {
        return java.util.Arrays.stream(variants).map(s -> item("horizoncobblemonitems:" + s + suffix, price)).toList();
    }

    private static List<PricedItem> randomHoverboardPool() {
        return BuiltInRegistries.ITEM.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getPath().startsWith("hoverboard_"))
                .map(BuiltInRegistries.ITEM::getKey)
                .filter(id -> id.getNamespace().equals("hoverboards"))
                .map(ResourceLocation::toString)
                .filter(id -> !id.equals(WHITE_HOVERBOARD) && !id.equals(BLACK_HOVERBOARD))
                .sorted()
                .map(id -> item(id, HOVERBOARD_PRICE))
                .toList();
    }

    private static List<WeightedItem> weightedBlackMarketPool() {
        ArrayList<WeightedItem> pool = new ArrayList<>();
        pool.add(weighted("horizoncobblemonitems:prismatic_carat", 250000, EXTREMELY_RARE_WEIGHT));
        pool.add(weighted("horizoncobblemonitems:ultimate_mac_and_cheese", 100000, RARE_WEIGHT));
        pool.add(weighted("horizoncobblemonitems:omnipotent_hidden_machine", 400000, EXTREMELY_RARE_WEIGHT));
        pool.add(weighted("horizoncobblemonitems:tm_hidden_machine", 100000, RARE_WEIGHT));
        pool.add(weighted("horizoncobblemonitems:egg_hidden_machine", 100000, RARE_WEIGHT));
        pool.add(weighted("horizoncobblemonitems:legacy_hidden_machine", 100000, RARE_WEIGHT));
        pool.add(weighted("horizoncobblemonitems:special_hidden_machine", 100000, RARE_WEIGHT));
        pool.add(weighted("horizoncobblemonitems:poke_treat", 50000, MEDIUM_WEIGHT));
        pool.add(weighted("horizoncobblemonitems:stale_poke_treat", 50, MEDIUM_WEIGHT));
        pool.add(weighted("horizoncobblemonitems:basket_ball", 1000000, EXTREMELY_RARE_WEIGHT));
        pool.add(weighted("horizoncobblemonitems:devolution_spray", 100000, RARE_WEIGHT));
        pool.add(weighted("horizoncobblemonitems:gender_change_potion", 10000, MEDIUM_WEIGHT));
        pool.add(weighted("horizoncobblemonitems:magic_8_ball", 10000, MEDIUM_WEIGHT));
        pool.add(weighted("cobblemon:ability_capsule", 20000, COMMON_WEIGHT));
        pool.add(weighted("cobblemon:ability_patch", 50000, COMMON_WEIGHT));
        pool.add(weighted("cobblemon:rare_candy", 9000, COMMON_WEIGHT));
        pool.add(weighted("cobblemon:exp_candy_xl", 10000, COMMON_WEIGHT));
        pool.add(weighted("mega_showdown:zygarde_cell", 10000, COMMON_WEIGHT));
        pool.add(weighted("mega_showdown:zygarde_core", 50000, RARE_WEIGHT));
        pool.add(weighted("legendarymonuments:aurora_essence_jar", 5000, MEDIUM_WEIGHT));
        pool.add(weighted("legendarymonuments:soul_jar", 5000, MEDIUM_WEIGHT));
        pool.add(weighted("mega_showdown:mega_stone", 10000, RARE_WEIGHT));
        pool.add(weighted("mega_showdown:blank_z", 10000, RARE_WEIGHT));
        pool.add(weighted("cobblemonsizevariation:shrink_essence", 5000, RARE_WEIGHT));
        pool.add(weighted("cobblemonsizevariation:growth_essence", 5000, RARE_WEIGHT));
        pool.add(weighted("cobblemon:master_ball", 100000, EXTREMELY_RARE_WEIGHT));
        pool.add(weighted("cobblemon_dreamweaving_prism_ball:prism_ball", 200000, EXTREMELY_RARE_WEIGHT));
        pool.add(weighted("cobblemon_dreamweaving_prism_ball:dreamweaving_prism_ball", 1000000, EXTREMELY_RARE_WEIGHT));
        pool.add(weighted("cobblemon:ancient_origin_ball", 100000, EXTREMELY_RARE_WEIGHT));
        for (String type : List.of("normal", "fire", "water", "electric", "grass", "ice",
                "fighting", "poison", "ground", "flying", "psychic", "bug", "rock",
                "ghost", "dragon", "dark", "steel", "fairy", "stellar")) {
            pool.add(weighted("mega_showdown:" + type + "_tera_shard", 1000, VERY_COMMON_WEIGHT));
        }
        pool.add(weighted("cobblesafari:fossil_random", 10000, MEDIUM_WEIGHT));
        pool.add(weighted("cobblesafari:fossil_hidden_ability", 20000, RARE_WEIGHT));
        pool.add(weighted("cobblesafari:fossil_shiny", 50000, EXTREMELY_RARE_WEIGHT));
        pool.add(weighted("cobblesafari:fossil_iv_max", 80000, EXTREMELY_RARE_WEIGHT));
        pool.add(weighted("cobblesafari:fossil_perfect", 200000, ULTRA_RARE_WEIGHT));
        pool.removeIf(entry -> !isAvailable(entry.item().id()));
        return pool;
    }

    private static PricedItem item(String id, int price) {
        return new PricedItem(id, price);
    }

    private static WeightedItem weighted(String id, int price, int weight) {
        return new WeightedItem(item(id, price), weight);
    }

    private static void addUnique(List<PricedItem> output, Set<String> ids, PricedItem item) {
        if (isAvailable(item.id()) && ids.add(item.id())) output.add(item);
    }

    private static void pick(List<PricedItem> output, Set<String> ids, RandomSource random,
                             int amount, List<PricedItem> source) {
        ArrayList<PricedItem> candidates = new ArrayList<>(source);
        candidates.removeIf(candidate -> !isAvailable(candidate.id()));
        for (int i = 0; i < amount && !candidates.isEmpty(); i++) {
            addUnique(output, ids, candidates.remove(random.nextInt(candidates.size())));
        }
    }

    private static boolean isAvailable(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        return id != null && BuiltInRegistries.ITEM.getOptional(id).isPresent();
    }

    private record PricedItem(String id, int price) {
    }

    private record WeightedItem(PricedItem item, int weight) {
    }

    private static Offer offer(String id, int price) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id))
                .map(item -> new Offer(new ItemStack(item), BigInteger.valueOf(price), -1)).orElse(null);
    }

    private static void addIfPresent(List<Offer> offers, Offer offer) {
        if (offer != null) offers.add(offer);
    }
}
