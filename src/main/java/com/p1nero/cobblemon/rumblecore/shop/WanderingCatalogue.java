package com.p1nero.cobblemon.rumblecore.shop;

import fr.harmex.cobbledollars.common.api.shop.Category;
import fr.harmex.cobbledollars.common.api.shop.Offer;
import fr.harmex.cobbledollars.common.api.shop.Shop;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
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
    private static final int UNLIMITED_STOCK = -1;
    private static final int ULTRA_RARE_WEIGHT = 1;
    private static final int EXTREMELY_RARE_WEIGHT = 3;
    private static final int RARE_WEIGHT = 8;
    private static final int MEDIUM_WEIGHT = 16;
    private static final int COMMON_WEIGHT = 32;
    private static final int VERY_COMMON_WEIGHT = 64;

    public static Shop create(RandomSource random) {
        List<PricedItem> selected = new ArrayList<>();
        Set<String> selectedIds = new HashSet<>();

        addUnique(selected, selectedIds, item("horizoncobblemonitems:void_bottle_cap", 10000));
        addUnique(selected, selectedIds, item("horizoncobblemonitems:golden_bottle_cap", 500000));
        addUnique(selected, selectedIds, item("horizoncobblemonitems:silver_bottle_cap", 100000));
        addUnique(selected, selectedIds, item("artifacts:mimic_spawn_egg", 10000));
        pick(selected, selectedIds, random, 2, List.of(
                item("horizoncobblemonitems:red_carat", 100000),
                item("horizoncobblemonitems:blue_carat", 100000),
                item("horizoncobblemonitems:green_carat", 100000),
                item("horizoncobblemonitems:yellow_carat", 100000),
                item("horizoncobblemonitems:pink_carat", 100000),
                item("horizoncobblemonitems:cyan_carat", 100000)));
        addUnique(selected, selectedIds, item("horizoncobblemonitems:axiom_berry", 50000));
        pick(selected, selectedIds, random, 2, List.of(
                item("horizoncobblemonitems:health_phoenix_feather", 50000),
                item("horizoncobblemonitems:muscle_phoenix_feather", 50000),
                item("horizoncobblemonitems:resist_phoenix_feather", 50000),
                item("horizoncobblemonitems:genius_phoenix_feather", 50000),
                item("horizoncobblemonitems:clever_phoenix_feather", 50000),
                item("horizoncobblemonitems:swift_phoenix_feather", 50000)));
        pick(selected, selectedIds, random, 1, List.of(
                item("horizoncobblemonitems:spicy_mac_and_cheese", 20000),
                item("horizoncobblemonitems:dry_mac_and_cheese", 20000),
                item("horizoncobblemonitems:sweet_mac_and_cheese", 20000),
                item("horizoncobblemonitems:bitter_mac_and_cheese", 20000),
                item("horizoncobblemonitems:sour_mac_and_cheese", 20000)));
        addUnique(selected, selectedIds, item("horizoncobblemonitems:tutor_hidden_machine", 100000));

        List<WeightedItem> pool = weightedBlackMarketPool();
        while (selected.size() < 15 && !pool.isEmpty()) {
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

        ArrayList<Offer> offers = new ArrayList<>();
        selected.forEach(entry -> addIfPresent(offers, offer(entry.id(), entry.price())));
        return new Shop(List.of(new Category("rumble.merchant.category", offers)));
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
