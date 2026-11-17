package com.p1nero.cobblemon.rumblecore;

import com.p1nero.cobblemon.rumblecore.phone.PhoneItems;
import com.p1nero.cobblemon.rumblecore.phone.PhoneState;
import com.p1nero.cobblemon.rumblecore.phone.PhoneSync;
import java.nio.file.Files;
import net.neoforged.neoforge.common.NeoForge;
import java.nio.file.Path;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@Mod(CobblemonRumbleCore.MOD_ID)
public final class CobblemonRumbleCore {
    public static final String MOD_ID = "cobblemon_rumble_core";

    public CobblemonRumbleCore(IEventBus modBus) {
        PhoneItems.register(modBus);
        modBus.addListener(PhoneSync::register);
        NeoForge.EVENT_BUS.addListener(PhoneState::onLogin);
        NeoForge.EVENT_BUS.addListener(PhoneState::onRespawn);
        modBus.addListener(CobblemonRumbleCore::addTranslations);
        modBus.addListener(CobblemonRumbleCore::addModelFixes);
        String packDirectory = System.getProperty("rumble.developmentPacks");
        if (packDirectory != null && !packDirectory.isBlank()) {
            Path root = Path.of(packDirectory);
            modBus.addListener((AddPackFindersEvent event) -> addDevelopmentPacks(event, root));
        }
    }

    private static void addTranslations(AddPackFindersEvent event) {
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(MOD_ID, "packs/mod_translations"),
                PackType.CLIENT_RESOURCES, Component.literal("Cobblemon Rumble Core – Mod Translations"),
                PackSource.BUILT_IN, true, Pack.Position.TOP);
    }

    private static void addModelFixes(AddPackFindersEvent event) {
        if (!ModList.get().isLoaded("mega_showdown")) return;
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(MOD_ID, "packs/model_fixes"),
                PackType.CLIENT_RESOURCES, Component.literal("Cobblemon Rumble Core – Model Fixes"),
                PackSource.BUILT_IN, true, Pack.Position.TOP);
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
