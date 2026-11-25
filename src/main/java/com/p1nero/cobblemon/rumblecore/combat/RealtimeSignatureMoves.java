package com.p1nero.cobblemon.rumblecore.combat;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import com.johnn.realtimecobblemon.moves.MoveRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Adapts Showdown's held-item move overrides without changing the saved move slot. */
public final class RealtimeSignatureMoves {
    private static final ResourceLocation ZACIAN = ResourceLocation.fromNamespaceAndPath("cobblemon", "zacian");
    private static final ResourceLocation ZAMAZENTA = ResourceLocation.fromNamespaceAndPath("cobblemon", "zamazenta");
    private static final ResourceLocation SWORD = ResourceLocation.fromNamespaceAndPath("mega_showdown", "rusted_sword");
    private static final ResourceLocation SHIELD = ResourceLocation.fromNamespaceAndPath("mega_showdown", "rusted_shield");

    private RealtimeSignatureMoves() {}

    public static MoveInfo resolve(Pokemon pokemon, Move move, MoveInfo original) {
        if (!"ironhead".equals(move.getName())) return original;
        var moveId = replacement(pokemon.getSpecies().getResourceIdentifier(),
                BuiltInRegistries.ITEM.getKey(pokemon.heldItem().getItem()), move.getName());
        if (moveId == null) return original;
        var replacement = MoveRegistry.info(moveId);
        return replacement == null ? original : replacement;
    }

    public static String replacement(ResourceLocation species, ResourceLocation item, String move) {
        if (!"ironhead".equals(move)) return null;
        if (ZACIAN.equals(species) && SWORD.equals(item)) return "behemothblade";
        if (ZAMAZENTA.equals(species) && SHIELD.equals(item)) return "behemothbash";
        return null;
    }

    public static boolean usesSlot(Pokemon pokemon, Move slot, MoveInfo resolved) {
        return resolved.id().equals(slot.getName()) || resolved.id().equals(replacement(
                pokemon.getSpecies().getResourceIdentifier(),
                BuiltInRegistries.ITEM.getKey(pokemon.heldItem().getItem()), slot.getName()));
    }
}
