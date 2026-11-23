package com.p1nero.cobblemon.rumblecore.combat.arena;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-world records of decisive player-versus-player arena matches. */
final class PvpStandings extends SavedData {
    private static final Factory<PvpStandings> FACTORY = new Factory<>(PvpStandings::new, PvpStandings::load);
    private static final Comparator<Entry> ORDER = Comparator.comparingLong(Entry::wins).reversed()
            .thenComparingLong(Entry::losses).thenComparing(Entry::id);
    private final Map<UUID, Entry> records = new HashMap<>();
    private List<Entry> sorted;

    record Entry(UUID id, String name, long wins, long losses) {}

    static PvpStandings get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "rumble_pvp_standings");
    }

    void record(ServerPlayer winner, ServerPlayer loser) {
        if (winner.getUUID().equals(loser.getUUID())) return;
        update(winner, true);
        update(loser, false);
        sorted = null;
        setDirty();
    }

    private void update(ServerPlayer player, boolean win) {
        UUID id = player.getUUID();
        Entry old = records.getOrDefault(id, new Entry(id, "", 0, 0));
        records.put(id, new Entry(id, player.getGameProfile().getName(),
                increment(old.wins(), win), increment(old.losses(), !win)));
    }

    private static long increment(long value, boolean add) { return add && value < Long.MAX_VALUE ? value + 1 : value; }

    List<Entry> ranked() {
        if (sorted == null) sorted = records.values().stream().sorted(ORDER).toList();
        return sorted;
    }

    private static PvpStandings load(CompoundTag tag, HolderLookup.Provider registries) {
        var standings = new PvpStandings();
        for (Tag item : tag.getList("players", Tag.TAG_COMPOUND)) {
            if (!(item instanceof CompoundTag row) || !row.hasUUID("id")) continue;
            UUID id = row.getUUID("id");
            standings.records.put(id, new Entry(id, row.getString("name"), Math.max(0, row.getLong("wins")), Math.max(0, row.getLong("losses"))));
        }
        return standings;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var rows = new ListTag();
        for (Entry record : records.values()) {
            var row = new CompoundTag();
            row.putUUID("id", record.id());
            row.putString("name", record.name());
            row.putLong("wins", record.wins());
            row.putLong("losses", record.losses());
            rows.add(row);
        }
        tag.put("players", rows);
        return tag;
    }
}
