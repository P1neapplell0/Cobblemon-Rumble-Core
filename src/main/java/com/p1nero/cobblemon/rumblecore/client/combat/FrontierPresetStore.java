package com.p1nero.cobblemon.rumblecore.client.combat;

import com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierRooms;
import com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierRoster;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

final class FrontierPresetStore {
    private FrontierPresetStore() {}

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config/cobblemon_rumble_core/frontier_presets.nbt");
    }

    static List<FrontierRoster.Pick> load() {
        var picks = new ArrayList<FrontierRoster.Pick>();
        try {
            var root = NbtIo.read(file());
            if (root != null) {
                var data = root.getList("presets", Tag.TAG_COMPOUND);
                for (int i = 0; i < data.size() && picks.size() < FrontierRooms.PRESET_LIMIT; i++) {
                    var pick = FrontierRooms.decode(data.getCompound(i));
                    if (pick != null && !pick.equals(FrontierRoster.EMPTY)) picks.add(pick);
                }
            }
        } catch (IOException e) { LogUtils.getLogger().warn("Could not load Frontier presets", e); }
        return picks;
    }

    static void save(List<FrontierRoster.Pick> picks) {
        Path path = file(), temp = path.resolveSibling("frontier_presets.tmp");
        try {
            Files.createDirectories(path.getParent());
            var root = new CompoundTag();
            var data = new ListTag();
            picks.stream().limit(FrontierRooms.PRESET_LIMIT).map(FrontierRooms::encode).forEach(data::add);
            root.put("presets", data);
            NbtIo.write(root, temp);
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { LogUtils.getLogger().warn("Could not save Frontier presets", e); }
    }
}
