package com.p1nero.cobblemon.rumblecore;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@Mod(CobblemonRumbleCore.MOD_ID)
public final class CobblemonRumbleCore {
    public static final String MOD_ID = "cobblemon_rumble_core";

    public CobblemonRumbleCore(IEventBus modBus) {
        String packDirectory = System.getProperty("rumble.developmentPacks");
        if (packDirectory != null && !packDirectory.isBlank()) {
            Path root = Path.of(packDirectory);
            modBus.addListener((AddPackFindersEvent event) -> addDevelopmentPacks(event, root));
        }
    }

    private static void addDevelopmentPacks(AddPackFindersEvent event, Path root) {
        String folder = event.getPackType() == PackType.SERVER_DATA ? "datapacks" : "resourcepacks";
        DirectoryValidator validator = new DirectoryValidator(target -> false);
        for (Path directory : new Path[] { root.resolve("both"), root.resolve(folder) }) {
            if (Files.isDirectory(directory)) {
                event.addRepositorySource(new FolderRepositorySource(directory, event.getPackType(), PackSource.DEFAULT, validator));
            }
        }
    }
}
