package com.p1nero.cobblemon.rumblecore.integration.bca;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class AdditionsNpcNames {
    private static final ResourceLocation NURSE_JOY = ResourceLocation.parse("cobblemon:nurse_joy");
    private static final ResourceLocation NURSE = ResourceLocation.parse("cobblemon:nurse");
    private static final ResourceLocation COBBLE_MERCHANT = ResourceLocation.parse("cobbledollars:cobble_merchant");
    private static final Map<String, String> SHOPKEEPERS = Map.ofEntries(
            Map.entry("Apricorn Salesman", "apricorns"),
            Map.entry("Arborist", "arborist"),
            Map.entry("Battle Master", "battle_master"),
            Map.entry("Battle Specialist", "battle_specialist"),
            Map.entry("Battle Strategist", "battle_strategist"),
            Map.entry("Chef", "chef"),
            Map.entry("Common Fish Salesman", "common_fish"),
            Map.entry("Currency Exchange", "currency_exchange"),
            Map.entry("Evolution Specialist", "evolution"),
            Map.entry("Exotic Fish Salesman", "exotic_fish"),
            Map.entry("Experience Trainer", "experience"),
            Map.entry("Food Merchant", "food"),
            Map.entry("Fruit Salesman", "fruit"),
            Map.entry("Gardening Specialist", "gardening"),
            Map.entry("Herb Salesman", "herbs"),
            Map.entry("Livestock Meat Salesman", "livestock_meat"),
            Map.entry("Master of Stones", "master_of_stones"),
            Map.entry("Mulch Salesman", "mulch"),
            Map.entry("Pasture Meat Salesman", "pasture_meat"),
            Map.entry("Pokeball Artisan", "pokeball_artisan"),
            Map.entry("Pokeball Specialist", "pokeball_specialist"),
            Map.entry("Pokemart Clerk", "pokemart"),
            Map.entry("Pokemon Pharmacist", "pharmacist"),
            Map.entry("Pokémon Pharmacist", "pharmacist_accented"),
            Map.entry("Pokémon Tech Specialist", "tech"),
            Map.entry("Potions Salesman", "potions"),
            Map.entry("Provisions Merchant", "provisions"),
            Map.entry("Sapling Salesman", "saplings"),
            Map.entry("Small Game Salesman", "small_game"),
            Map.entry("Stone Specialist", "stone_specialist"),
            Map.entry("Supplement Specialist", "supplements"),
            Map.entry("Vegetable Salesman", "vegetables"),
            Map.entry("Wheat Salesman", "wheat"));

    private AdditionsNpcNames() {}

    public static boolean additionsLoaded() {
        // Connector normalizes Fabric's mod ID for NeoForge.
        return ModList.get().isLoaded("cobblemon_additions") || ModList.get().isLoaded("cobblemon-additions");
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        var entity = event.getEntity();
        var name = entity.getCustomName();
        if (name == null || !(name.getContents() instanceof PlainTextContents text) || !name.getSiblings().isEmpty()) return;
        String original = text.text();
        // The structure templates store literal CustomName values, including for existing NPCs in saves.
        if (original.equals("Nurse Joy") && (entity instanceof NPCEntity npc && NURSE_JOY.equals(npc.getNpc().getId())
                || entity instanceof Villager villager && NURSE.equals(BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession())))) {
            entity.setCustomName(Component.translatableWithFallback("npc.nurse_joy.name", original).setStyle(name.getStyle()));
        } else if (COBBLE_MERCHANT.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()))) {
            String key = SHOPKEEPERS.get(original);
            if (key != null) entity.setCustomName(Component.translatableWithFallback("npc.bca.shopkeeper." + key, original).setStyle(name.getStyle()));
        } else if (entity instanceof Villager villager && (original.equals("Shopkeeper") || original.equals("Food Shopkeeper"))
                && villager.getOffers().stream().anyMatch(offer -> offer.getCostA().is(CobblemonItems.RELIC_COIN))) {
            String key = original.equals("Shopkeeper") ? "relic_shopkeeper" : "relic_food_shopkeeper";
            entity.setCustomName(Component.translatableWithFallback("npc.bca.shopkeeper." + key, original).setStyle(name.getStyle()));
        }
    }
}
