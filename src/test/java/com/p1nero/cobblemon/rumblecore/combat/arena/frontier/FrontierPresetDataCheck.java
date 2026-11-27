package com.p1nero.cobblemon.rumblecore.combat.arena.frontier;

import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.Util;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.IOUtilities;


public final class FrontierPresetDataCheck {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();

        var first = UUID.randomUUID();
        var second = UUID.randomUUID();
        var legacy = List.of(new FrontierRoster.Pick(ResourceLocation.parse("cobblemon:charizard"),
                FrontierRoster.NONE, List.of("flamethrower", "", "", "")));
        var edited = List.of(new FrontierRoster.Pick(ResourceLocation.parse("cobblemon:charizard"),
                ResourceLocation.parse("cobblemon:choice_specs"), List.of("flamethrower", "dragonpulse", "", "")));
        var data = new FrontierPresetData();
        data.update(first, legacy, true);
        data.update(first, edited, false);
        data.update(first, legacy, true);
        data.update(second, legacy, true);
        data.update(second, List.of(), false);
        var path = Files.createTempFile("rumble-frontier-presets-", ".dat");
        try {
            data.save(path.toFile(), null); // This format stores plain IDs, not registry-bound data.
            IOUtilities.waitUntilIOWorkerComplete();

            var restored = FrontierPresetData.load(NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()).getCompound("data"), null);
            restored.update(second, legacy, true);
            if (!edited.equals(restored.get(first)) || !List.of().equals(restored.get(second))) {
                throw new AssertionError("Presets or intentional deletions did not survive reload");
            }
            var oversized = FrontierRooms.encodePresets(Collections.nCopies(FrontierRooms.PRESET_LIMIT, legacy.getFirst()));
            oversized.add(FrontierRooms.encode(legacy.getFirst()));
            if (FrontierRooms.decodePresets(oversized) != null) throw new AssertionError("Oversized preset payload accepted");
            var malformed = FrontierRooms.encodePresets(legacy);
            malformed.add(new CompoundTag());
            if (FrontierRooms.decodePresets(malformed) != null) throw new AssertionError("Malformed preset payload accepted");
        } finally {
            Files.deleteIfExists(path);
            Util.shutdownExecutors();

        }
        System.out.println("Frontier preset disk round trip, player isolation, migration and payload limits passed");
    }
}
