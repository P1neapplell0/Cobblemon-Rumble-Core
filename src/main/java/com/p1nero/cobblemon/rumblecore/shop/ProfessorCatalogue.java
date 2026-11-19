package com.p1nero.cobblemon.rumblecore.shop;

import fr.harmex.cobbledollars.common.api.shop.Category;
import fr.harmex.cobbledollars.common.api.shop.Offer;
import fr.harmex.cobbledollars.common.api.shop.Shop;
import com.p1nero.cobblemon.rumblecore.phone.PhoneApp;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ProfessorCatalogue {
    public static final String CATEGORY = "rumble.shop.category.";

    private ProfessorCatalogue() {}

    public static Shop create() {
        List<Category> shop = new ArrayList<>();
        add(shop, "balls",
                200, "poke_ball", 600, "great_ball", 1200, "ultra_ball", 1000, "quick_ball", "dusk_ball", "timer_ball",
                "repeat_ball", "net_ball", "dive_ball", "nest_ball", "heal_ball", "luxury_ball", "premier_ball");
        add(shop, "gems", 600, "normal_gem", "fire_gem", "water_gem", "electric_gem", "grass_gem", "ice_gem",
                "fighting_gem", "poison_gem", "ground_gem", "flying_gem", "psychic_gem", "bug_gem", "rock_gem",
                "ghost_gem", "dragon_gem", "dark_gem", "steel_gem", "fairy_gem");
        add(shop, "medicine",
                200, "potion", "antidote", "awakening", "burn_heal", "ice_heal", "paralyze_heal",
                400, "full_heal", 700, "super_potion", 1500, "hyper_potion", 2000, "revive",
                2500, "max_potion", 3000, "full_restore", 4000, "max_revive");
        add(shop, "training",
                1000, "x_attack", "x_defence", "x_speed", "x_accuracy", "x_special_attack", "x_special_defence",
                7500, "pp_up", 10000, "pp_max",
                15000, "ability_capsule", 30000, "ability_patch");
        add(shop, "evolution",
                2500, "fire_stone", "water_stone", "thunder_stone", "leaf_stone", "moon_stone", "sun_stone",
                "shiny_stone", "dusk_stone", "dawn_stone", "ice_stone", 4000, "link_cable", "metal_coat",
                "kings_rock", "dragon_scale", "upgrade", "dubious_disc", "protector", "electirizer", "magmarizer",
                "reaper_cloth", "razor_claw", "razor_fang", "prism_scale", "sweet_apple", "tart_apple", "syrupy_apple");
        add(shop, "decor", 1200, "black_glasses", "wise_glasses", "silk_scarf",
                650, "big_malasada", "casteliacone", "lava_cookie", "lumiose_galette", "old_gateau", "pewter_crunchies",
                "rage_candy_bar", "sinister_tea", "smoked_tail_curry", 800, "gilded_chest", "black_gilded_chest",
                "blue_gilded_chest", "green_gilded_chest", "pink_gilded_chest", "white_gilded_chest", "yellow_gilded_chest",
                1200, "minecraft:leather_helmet", "minecraft:golden_helmet");
        add(shop, "held", 1200, "black_belt", "muscle_band", "charcoal_stick", "mystic_water", "magnet", "miracle_seed",
                "never_melt_ice", "poison_barb", "soft_sand", "sharp_beak", "twisted_spoon", "silver_powder", "hard_stone",
                "spell_tag", "dragon_fang", "fairy_feather", 4000, "focus_sash", "safety_goggles", "protective_pads",
                "covert_cloak", "rocky_helmet", "leftovers", "eviolite", "heavy_duty_boots", "ability_shield", "focus_band",
                "black_sludge", "bright_powder", "flame_orb", "toxic_orb", "loaded_dice", "punching_glove", "scope_lens",
                "wide_lens", "zoom_lens", "terrain_extender", "utility_umbrella", "weakness_policy", 6000, "assault_vest",
                "choice_band", "choice_specs", "choice_scarf", "life_orb", 2000, "absorb_bulb", "air_balloon", "binding_band",
                "blunder_policy", "cell_battery", "eject_button", "eject_pack", "electric_seed", "grassy_seed", "misty_seed",
                "psychic_seed", "iron_ball", "mental_herb", "mirror_herb", "power_herb", "white_herb", "red_card", "ring_target",
                "room_service", "throat_spray");
        add(shop, "candy",
                100, "exp_candy_xs", 350, "exp_candy_s", 1000, "exp_candy_m", 3000, "exp_candy_l",
                8000, "exp_candy_xl", 12000, "rare_candy",
                3000, "health_candy", "mighty_candy", "tough_candy", "smart_candy", "courage_candy", "quick_candy",
                1000, "sickly_candy", "weak_candy", "brittle_candy", "numb_candy", "coward_candy", "slow_candy");
        add(shop, "effort",
                250, "health_feather", "muscle_feather", "resist_feather", "genius_feather", "clever_feather", "swift_feather",
                2200, "hp_up", "protein", "iron", "calcium", "zinc", "carbos",
                5500, "power_weight", "power_bracer", "power_belt", "power_lens", "power_band", "power_anklet",
                3500, "fresh_start_mochi");
        var berries = new ArrayList<Offer>();
        BuiltInRegistries.ITEM.entrySet().stream()
                .filter(entry -> entry.getKey().location().getNamespace().equals("cobblemon"))
                .filter(entry -> entry.getValue() instanceof com.cobblemon.mod.common.item.berry.BerryItem)
                .sorted(java.util.Comparator.comparing(entry -> entry.getKey().location()))
                .forEach(entry -> berries.add(new Offer(new ItemStack(entry.getValue()), BigInteger.valueOf(1800), -1)));
        if (!berries.isEmpty()) shop.add(new Category(CATEGORY + "berries", berries));
        add(shop, "farming", 350, "coarse_mulch", "growth_mulch", "humid_mulch", "loamy_mulch", "peat_mulch", "rich_mulch", "sandy_mulch", "surprise_mulch",
                1200, "minecraft:golden_hoe", 5000, "poke_rod");
        add(shop, "waystones", 1000, "waystones:waystone", "waystones:mossy_waystone", "waystones:sandy_waystone",
                "waystones:blackstone_waystone", "waystones:deepslate_waystone", "waystones:end_stone_waystone",
                "waystones:mud_bricks_waystone", "waystones:prismarine_waystone", "waystones:purpur_waystone",
                "waystones:red_nether_bricks_waystone");
        List<Offer> phones = new ArrayList<>();
        for (var color : net.minecraft.world.item.DyeColor.values()) {
            phone(phones, color.getName() + "_smartphone");
        }
        phone(phones, "smartphone");
        if (!phones.isEmpty()) shop.add(new Category(CATEGORY + "phones", phones));
        List<Offer> apps = new ArrayList<>();
        for (PhoneApp app : PhoneApp.values()) {
            int price = switch (app) {
                case NATURE_COMPASS, STRUCTURE_COMPASS -> 2500;
                case HEALING, PC -> 8000;
                case POKEDEX -> 3000;
                case POKEMON_TRACKER, FISHING_NAVIGATOR -> 5000;
                default -> 0;
            };
            if (price > 0 && app.available()) BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(
                    "cobblemon_rumble_core", app.itemId())).ifPresent(item -> apps.add(new Offer(new ItemStack(item), BigInteger.valueOf(price), -1)));
        }
        if (!apps.isEmpty()) shop.add(new Category(CATEGORY + "apps", apps));
        return new Shop(shop);
    }

    private static void phone(List<Offer> offers, String path) {
        BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath("cobblemon_smartphone", path))
                .ifPresent(item -> offers.add(new Offer(new ItemStack(item), BigInteger.valueOf(2000), -1)));
    }

    // A price applies to the following item IDs until the next price in the catalogue.
    private static void add(List<Category> shop, String id, Object... catalogue) {
        List<Offer> offers = new ArrayList<>();
        int price = 0;
        for (Object entry : catalogue) {
            if (entry instanceof Integer amount) { price = amount; continue; }
            String key = (String) entry;
            var itemId = ResourceLocation.parse(key.contains(":") ? key : "cobblemon:" + key);
            var item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
            if (item == null) continue;
            offers.add(new Offer(new ItemStack(item), BigInteger.valueOf(price), -1));
        }
        shop.add(new Category(CATEGORY + id, offers));
    }

}
