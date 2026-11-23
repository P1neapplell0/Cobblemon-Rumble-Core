package com.p1nero.cobblemon.rumblecore.shop;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import fr.harmex.cobbledollars.common.api.bank.Bank;
import fr.harmex.cobbledollars.common.api.shop.Offer;
import fr.harmex.cobbledollars.common.api.shop.Shop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.FMLPaths;

/** Legacy CobbleDollars item/price rows, with the professor's retail catalogue applied last. */
public final class RecyclingCatalogue {
    private static final String CONFIG = "cobblemon_rumble_core/recycling.json";

    private static final java.util.Set<String> EXCLUDED = java.util.Set.of(
            "cobblemon:ability_patch",
            "cobblemon:ancient_origin_ball",
            "cobblemon:master_ball",
            "cobblesafari:master_ball",
            "cobblemon:relic_coin_sack",
            "cobblesafari:red_chain_core",
            "cobblesafari:redchain_fragment",
            "cobblesafari:starpiece",
            "minecraft:amethyst_block",
            "minecraft:amethyst_shard",
            "minecraft:coal",
            "minecraft:coal_block",
            "minecraft:copper_block",
            "minecraft:copper_ingot",
            "minecraft:diamond",
            "minecraft:diamond_block",
            "minecraft:emerald",
            "minecraft:emerald_block",
            "minecraft:gold_block",
            "minecraft:gold_ingot",
            "minecraft:heart_of_the_sea",
            "minecraft:iron_block",
            "minecraft:iron_ingot",
            "minecraft:lapis_block",
            "minecraft:lapis_lazuli",
            "minecraft:nether_star",
            "minecraft:netherite_block",
            "minecraft:netherite_ingot",
            "minecraft:netherite_scrap",
            "minecraft:quartz",
            "minecraft:quartz_block",
            "minecraft:raw_copper",
            "minecraft:raw_iron",
            "minecraft:raw_gold",
            "minecraft:redstone",
            "minecraft:redstone_block"
    );

    private RecyclingCatalogue() {}

    public static Bank create(Shop shop) {
        Map<Item, BigInteger> prices = new LinkedHashMap<>();
        Path config = FMLPaths.CONFIGDIR.get().resolve(CONFIG);
        try (var reader = Files.isRegularFile(config) ? Files.newBufferedReader(config)
                : new BufferedReader(new InputStreamReader(RecyclingCatalogue.class.getResourceAsStream(
                        "/defaults/" + CONFIG), StandardCharsets.UTF_8))) {
            for (var entry : JsonParser.parseReader(reader).getAsJsonArray()) {
                try {
                    var row = entry.getAsJsonObject();
                    var id = ResourceLocation.parse(row.get("item").getAsString());
                    var price = new BigInteger(row.get("price").getAsString());
                    BuiltInRegistries.ITEM.getOptional(id).filter(item -> item != Items.AIR)
                            .filter(item -> price.signum() > 0 && price.bitLength() <= 63)
                            .ifPresent(item -> prices.put(item, price));
                } catch (RuntimeException e) {
                    LogUtils.getLogger().warn("Invalid recycling price row: {}", entry);
                }
            }
        } catch (Exception e) {
            LogUtils.getLogger().error("Could not read recycling catalogue {}", config, e);
        }
        for (var category : shop.getCategories()) for (var offer : category.getOffers()) {
            var price = offer.getPrice().divide(BigInteger.TEN);
            for (var stack : offer.getItems()) {
                if (price.signum() > 0) prices.put(stack.getItem(), price);
                else prices.remove(stack.getItem());
            }
        }
        prices.keySet().removeIf(item -> EXCLUDED.contains(BuiltInRegistries.ITEM.getKey(item).toString())
                || BuiltInRegistries.ITEM.wrapAsHolder(item).is(net.neoforged.neoforge.common.Tags.Items.ORES)
                || BuiltInRegistries.ITEM.wrapAsHolder(item).is(net.neoforged.neoforge.common.Tags.Items.RAW_MATERIALS));
        Map<Offer, List<DataComponentType<?>>> offers = new LinkedHashMap<>();
        prices.forEach((item, price) -> offers.put(new Offer(new ItemStack(item), price, -1), List.of()));
        return new Bank(offers);
    }

    public static String legacyCategory(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        if (path.endsWith("_berry") || path.endsWith("_mint_leaf") || path.contains("apricorn") || path.endsWith("_sweet")
                || path.equals("berry_juice")) return "produce";
        if (path.contains("coin") || path.contains("experience") || path.equals("nether_star") || path.equals("heart_of_the_sea")
                || path.equals("starpiece") || path.equals("heartscale")) return "valuables";
        if (id.getNamespace().equals("minecraft") || id.getNamespace().equals("create") || path.contains("tumblestone")
                || path.startsWith("sphere_") || path.contains("galar_particle")) return "materials";
        return "other";
    }
}
