package com.p1nero.cobblemon.rumblecore;

import com.p1nero.cobblemon.rumblecore.phone.PhoneItems;
import com.p1nero.cobblemon.rumblecore.prologue.ProfessorEntity;
import com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining;
import com.p1nero.cobblemon.rumblecore.prologue.TrialWaystones;
import com.p1nero.cobblemon.rumblecore.combat.InstancePokemonDamage;
import com.p1nero.cobblemon.rumblecore.combat.BattleMusic;
import com.p1nero.cobblemon.rumblecore.combat.reward.WildCapture;
import com.p1nero.cobblemon.rumblecore.guide.QuestAcceptedNotice;
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
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@Mod(CobblemonRumbleCore.MOD_ID)
public final class CobblemonRumbleCore {
    public static final String MOD_ID = "cobblemon_rumble_core";

    public CobblemonRumbleCore(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, RumbleConfig.SPEC);
        PhoneItems.register(modBus);
        com.p1nero.cobblemon.rumblecore.prologue.MayMegaDevice.register(modBus);
        com.p1nero.cobblemon.rumblecore.prologue.PersonalDimension.registerItems(modBus);
        PrologueWorld.initialize();
        PrologueTraining.initialize();
        com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch.initialize();
        modBus.addListener(com.p1nero.cobblemon.rumblecore.combat.arena.ArenaPackets::register);
        modBus.addListener(com.p1nero.cobblemon.rumblecore.combat.arena.ArenaIntro::register);
        modBus.addListener(com.p1nero.cobblemon.rumblecore.combat.arena.FrontierKnockout::register);
        modBus.addListener(com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierRooms::register);
        modBus.addListener(com.p1nero.cobblemon.rumblecore.combat.arena.HallOfFame::register);
        TrialWaystones.initialize();
        InstancePokemonDamage.initialize();
        WildCapture.initialize();
        com.p1nero.cobblemon.rumblecore.prologue.DailyCommissions.initialize();
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> {
            if (ModList.get().isLoaded("mine_chat")) event.enqueueWork(com.p1nero.cobblemon.rumblecore.prologue.ProfessorStockMessage::register);
        });
        com.p1nero.cobblemon.rumblecore.prologue.HallQuestMarkers.register();
        ProfessorEntity.register(modBus);
        com.p1nero.cobblemon.rumblecore.prologue.TrialMerchantEntity.register(modBus);
        com.p1nero.cobblemon.rumblecore.prologue.MegaSimulator.register(modBus);
        modBus.addListener(ProloguePackets::register);
        modBus.addListener(com.p1nero.cobblemon.rumblecore.prologue.OpeningTutorial::register);
        modBus.addListener(com.p1nero.cobblemon.rumblecore.prologue.MerchantPanels::register);
        modBus.addListener(BattleMusic::register);
        modBus.addListener(QuestAcceptedNotice::register);
        modBus.addListener(PhoneSync::register);
        modBus.addListener(com.p1nero.cobblemon.rumblecore.shop.RecyclingPackets::register);
        NeoForge.EVENT_BUS.addListener(PhoneState::onLogin);
        NeoForge.EVENT_BUS.addListener(PhoneState::onRespawn);
        NeoForge.EVENT_BUS.addListener(PhoneState::onDimensionChange);
        modBus.addListener(CobblemonRumbleCore::addTranslations);
        modBus.addListener(CobblemonRumbleCore::addModelFixes);
        modBus.addListener(CobblemonRumbleCore::addAdditionsDistribution);
        modBus.addListener(CobblemonRumbleCore::addLegacyTrainerTeams);
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

    private static void addAdditionsDistribution(AddPackFindersEvent event) {
        if (!ModList.get().isLoaded("cobblemon-additions")) return;
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(MOD_ID, "packs/bca_distribution"),
                PackType.SERVER_DATA, Component.literal("Rumble - Cobblemon Additions Distribution"),
                PackSource.BUILT_IN, true, Pack.Position.TOP);
    }

    private static void addLegacyTrainerTeams(AddPackFindersEvent event) {
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(MOD_ID, "packs/legacy_trainer_teams"),
                PackType.SERVER_DATA, Component.literal("Rumble - Incandescent Verity Trainer Teams"),
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
