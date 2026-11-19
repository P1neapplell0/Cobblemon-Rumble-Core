package com.p1nero.cobblemon.rumblecore.prologue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.SavedData;

final class VillageArrival extends SavedData {
    private static final ResourceKey<Structure> DEPARTMENT_STORE = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath("bca", "village/default_large"));
    private static final Factory<VillageArrival> FACTORY = new Factory<>(VillageArrival::new, VillageArrival::load);
    private BlockPos origin;
    private boolean searchedAdditions;

    static BlockPos get(ServerLevel level) {
        var data = level.getDataStorage().computeIfAbsent(FACTORY, "rumble_village_arrival");
        var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var market = structures.getHolder(DEPARTMENT_STORE);
        if (data.origin != null && data.searchedAdditions == market.isPresent()) return data.origin;

        var spawn = level.getSharedSpawnPos();
        var generator = level.getChunkSource().getGenerator();
        var found = market.map(holder -> generator.findNearestMapStructure(level, HolderSet.direct(holder), spawn, 100, false)).orElse(null);
        BlockPos at;
        if (found != null) {
            // Step outside the main building's footprint, rather than using the roof height at the locate marker.
            var marker = found.getFirst();
            var chunk = level.getChunkAt(marker);
            var start = chunk.getStartForStructure(found.getSecond().value());
            if (start != null && start.isValid() && !start.getPieces().isEmpty()) {
                var box = start.getPieces().getFirst().getBoundingBox();
                marker = new BlockPos(box.getCenter().getX(), box.minY(), box.maxZ() + 2);
            }
            at = surface(level, marker);
        } else {
            var village = level.findNearestMapStructure(StructureTags.VILLAGE, spawn, 100, false);
            at = village == null ? spawn : surface(level, village);
        }
        data.origin = level.getWorldBorder().isWithinBounds(at) ? at : spawn;
        data.searchedAdditions = market.isPresent();
        data.setDirty();
        return data.origin;
    }

    private static BlockPos surface(ServerLevel level, BlockPos marker) {
        if (!level.getWorldBorder().isWithinBounds(marker)) return level.getSharedSpawnPos();
        var chunk = level.getChunkAt(marker);
        return marker.atY(chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, marker.getX() & 15, marker.getZ() & 15) + 1);
    }

    private static VillageArrival load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new VillageArrival();
        if (tag.contains("origin")) data.origin = BlockPos.of(tag.getLong("origin"));
        data.searchedAdditions = tag.getBoolean("searched_additions");
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (origin != null) tag.putLong("origin", origin.asLong());
        tag.putBoolean("searched_additions", searchedAdditions);
        return tag;
    }
}
