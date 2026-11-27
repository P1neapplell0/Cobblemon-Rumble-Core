package com.p1nero.cobblemon.rumblecore.combat.arena.frontier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Private presets follow the player's UUID within the server world, not the room or client profile. */
final class FrontierPresetData extends SavedData {
    private static final Factory<FrontierPresetData> FACTORY = new Factory<>(FrontierPresetData::new, FrontierPresetData::load);
    private final Map<UUID, List<FrontierRoster.Pick>> presets = new HashMap<>();

    static FrontierPresetData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "rumble_frontier_presets");
    }

    List<FrontierRoster.Pick> get(UUID player) { return presets.get(player); }

    void update(UUID player, List<FrontierRoster.Pick> picks, boolean importOnly) {
        // An intentionally emptied list is still a record and must not resurrect legacy presets.
        if (importOnly && (picks.isEmpty() || presets.containsKey(player))) return;
        if (!picks.equals(presets.get(player))) {
            presets.put(player, List.copyOf(picks));
            setDirty();
        }
    }

    static FrontierPresetData load(CompoundTag tag, HolderLookup.Provider registries) {
        var result = new FrontierPresetData();
        for (var item : tag.getList("players", Tag.TAG_COMPOUND)) {
            if (!(item instanceof CompoundTag row) || !row.hasUUID("id")) continue;
            var picks = FrontierRooms.decodePresets(row.getList("presets", Tag.TAG_COMPOUND));
            if (picks != null) result.presets.put(row.getUUID("id"), picks);
        }
        return result;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var rows = new ListTag();
        presets.forEach((id, picks) -> {
            var row = new CompoundTag();
            row.putUUID("id", id);
            row.put("presets", FrontierRooms.encodePresets(picks));
            rows.add(row);
        });
        tag.put("players", rows);
        return tag;
    }
}
