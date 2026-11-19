package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.p1nero.misc.custom_instances.instance.InstanceDefinition;
import com.p1nero.misc.custom_instances.storage.DefinitionStore;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.Tags;

/** All layouts share the same deployment pads and combat plane. */
public enum ArenaTheme {
    DEFAULT, ROCK, GRASS, GHOST, ICE, GROUND, FIRE, DARK, ELECTRIC, FOREST, WATER, PSYCHIC, SKY;

    public String path() { return name().toLowerCase(Locale.ROOT); }
    public String template() { return this == DEFAULT ? ArenaMatch.TEMPLATE : "rumble_battle_" + path(); }
    public String chunks() { return this == DEFAULT ? "rumble_battle_arena" : "rumble_arena_" + path(); }

    public ArenaTheme available() {
        var regions = DefinitionStore.configRoot().resolve("region").resolve(chunks()).resolve("region");
        return Files.isRegularFile(regions.resolve("r.-1.-1.mca")) && Files.isRegularFile(regions.resolve("r.-1.0.mca"))
                && Files.isRegularFile(regions.resolve("r.0.-1.mca")) && Files.isRegularFile(regions.resolve("r.0.0.mca"))
                ? this : DEFAULT;
    }

    public static ArenaTheme gym(String name) {
        if (name.startsWith("elite4_")) {
            for (String type : name.substring("elite4_".length()).split("_")) {
                ArenaTheme mapped = type(type);
                if (mapped != DEFAULT) return mapped;
            }
        }
        return type(name);
    }

    public static ArenaTheme type(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "rock", "steel" -> ROCK;
            case "grass", "bug", "poison" -> GRASS;
            case "ghost" -> GHOST;
            case "ice" -> ICE;
            case "ground", "fighting" -> GROUND;
            case "fire" -> FIRE;
            case "dark" -> DARK;
            case "electric" -> ELECTRIC;
            case "water" -> WATER;
            case "psychic", "fairy" -> PSYCHIC;
            case "flying", "dragon", "champion", "elite4_champion" -> SKY;
            default -> DEFAULT;
        };
    }

    public static ArenaTheme biome(ServerLevel level, BlockPos origin) {
        var biome = level.getBiome(origin);
        if (biome.is(Tags.Biomes.IS_SNOWY) || biome.is(Tags.Biomes.IS_ICY)) return ICE;
        if (biome.is(Tags.Biomes.IS_OCEAN) || biome.is(Tags.Biomes.IS_RIVER) || biome.is(Tags.Biomes.IS_BEACH)) return WATER;
        if (biome.is(Tags.Biomes.IS_DESERT) || biome.is(Tags.Biomes.IS_BADLANDS) || biome.is(Tags.Biomes.IS_SAVANNA)) return GROUND;
        if (biome.is(Tags.Biomes.IS_JUNGLE) || biome.is(Tags.Biomes.IS_SWAMP)) return GRASS;
        if (biome.is(Tags.Biomes.IS_FOREST) || biome.is(Tags.Biomes.IS_TAIGA)) return FOREST;
        if (biome.is(Tags.Biomes.IS_MOUNTAIN)) return ROCK;
        return DEFAULT;
    }

    public static void install(DefinitionStore store, InstanceDefinition standard) throws IOException {
        for (ArenaTheme theme : values()) {
            if (theme == DEFAULT || theme.available() != theme) continue;
            var definition = new InstanceDefinition(theme.template(), standard.name(), 0, 0, false, 2,
                    standard.spawn(), standard.victoryType(), theme.template(), "", theme.chunks())
                    .withPresentation(standard.icon(), "", standard.description())
                    .withDimensionSettings(standard.dimensionSettings());
            store.save(definition);
        }
    }
}
