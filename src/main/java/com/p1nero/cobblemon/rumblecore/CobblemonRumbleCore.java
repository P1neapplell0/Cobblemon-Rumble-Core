package com.p1nero.cobblemon.rumblecore;

import com.p1nero.cobblemon.rumblecore.combat.BattleMusic;
import com.p1nero.cobblemon.rumblecore.combat.InstancePokemonDamage;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindupPacket;
import com.p1nero.cobblemon.rumblecore.item.PokeBrick;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeImpactPacket;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeBeamClashPacket;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeReleaseAim;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaIntro;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaPackets;
import com.p1nero.cobblemon.rumblecore.combat.arena.FrontierKnockout;
import com.p1nero.cobblemon.rumblecore.combat.arena.HallOfFame;
import com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierRooms;
import com.p1nero.cobblemon.rumblecore.combat.reward.WildCapture;
import com.p1nero.cobblemon.rumblecore.guide.QuestAcceptedNotice;
import com.p1nero.cobblemon.rumblecore.integration.bca.AdditionsNpcNames;
import com.p1nero.cobblemon.rumblecore.phone.PhoneItems;
import com.p1nero.cobblemon.rumblecore.phone.PhoneState;
import com.p1nero.cobblemon.rumblecore.phone.PhoneSync;
import com.p1nero.cobblemon.rumblecore.prologue.DailyCommissions;
import com.p1nero.cobblemon.rumblecore.prologue.HallQuestMarkers;
import com.p1nero.cobblemon.rumblecore.prologue.MayMegaDevice;
import com.p1nero.cobblemon.rumblecore.prologue.MegaSimulator;
import com.p1nero.cobblemon.rumblecore.prologue.MerchantPanels;
import com.p1nero.cobblemon.rumblecore.prologue.OpeningTutorial;
import com.p1nero.cobblemon.rumblecore.prologue.PersonalDimension;
import com.p1nero.cobblemon.rumblecore.prologue.ProfessorEntity;
import com.p1nero.cobblemon.rumblecore.prologue.ProfessorStockMessage;
import com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.cobblemon.rumblecore.prologue.TrialMerchantEntity;
import com.p1nero.cobblemon.rumblecore.prologue.TrialWaystones;
import com.p1nero.cobblemon.rumblecore.shop.RecyclingPackets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@Mod(CobblemonRumbleCore.MOD_ID)
public final class CobblemonRumbleCore {
    public static final String MOD_ID = "cobblemon_rumble_core";

    public CobblemonRumbleCore(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, RumbleConfig.SPEC);
        PhoneItems.register(modBus);
        PokeBrick.register(modBus);
        MayMegaDevice.register(modBus);
        PersonalDimension.registerItems(modBus);
        PrologueWorld.initialize();
        PrologueTraining.initialize();
        ArenaMatch.initialize();
        modBus.addListener(ArenaPackets::register);
        modBus.addListener(ArenaIntro::register);
        modBus.addListener(FrontierKnockout::register);
        modBus.addListener(FrontierRooms::register);
        modBus.addListener(HallOfFame::register);
        TrialWaystones.initialize();
        InstancePokemonDamage.initialize();
        WildCapture.initialize();
        DailyCommissions.initialize();
        modBus.addListener((FMLCommonSetupEvent event) -> {
            if (ModList.get().isLoaded("mine_chat")) event.enqueueWork(ProfessorStockMessage::register);
        });
        HallQuestMarkers.register();
        ProfessorEntity.register(modBus);
        TrialMerchantEntity.register(modBus);
        MegaSimulator.register(modBus);
        modBus.addListener(ProloguePackets::register);
        modBus.addListener(OpeningTutorial::register);
        modBus.addListener(MerchantPanels::register);
        modBus.addListener(BattleMusic::register);
        modBus.addListener(RealtimeMoveWindupPacket::register);
        modBus.addListener(RealtimeImpactPacket::register);
        modBus.addListener(RealtimeBeamClashPacket::register);
        modBus.addListener(RealtimeReleaseAim::register);
        modBus.addListener(QuestAcceptedNotice::register);
        modBus.addListener(PhoneSync::register);
        modBus.addListener(RecyclingPackets::register);
        NeoForge.EVENT_BUS.addListener(PhoneState::onLogin);
        NeoForge.EVENT_BUS.addListener(PhoneState::onRespawn);
        NeoForge.EVENT_BUS.addListener(PhoneState::onDimensionChange);
        modBus.addListener(CobblemonRumbleCore::addTranslations);
        modBus.addListener(CobblemonRumbleCore::addModelFixes);
        modBus.addListener(CobblemonRumbleCore::addPoseFixes);
        modBus.addListener(CobblemonRumbleCore::addAdditionsDistribution);
        modBus.addListener(CobblemonRumbleCore::addAdditionsDialogues);
        if (AdditionsNpcNames.additionsLoaded()) NeoForge.EVENT_BUS.addListener(AdditionsNpcNames::onJoin);
        modBus.addListener(CobblemonRumbleCore::addLegacyTrainerTeams);
        modBus.addListener(CobblemonRumbleCore::addExternalPacks);
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

    private static void addPoseFixes(AddPackFindersEvent event) {
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(MOD_ID, "packs/pose_fixes"),
                PackType.CLIENT_RESOURCES, Component.literal("Cobblemon Rumble Core – Pose Compatibility"),
                PackSource.BUILT_IN, true, Pack.Position.TOP);
    }

    private static void addAdditionsDistribution(AddPackFindersEvent event) {
        if (!ModList.get().isLoaded("cobblemon-additions")) return;
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(MOD_ID, "packs/bca_distribution"),
                PackType.SERVER_DATA, Component.literal("Rumble - Cobblemon Additions Distribution"),
                PackSource.BUILT_IN, true, Pack.Position.TOP);
    }

    private static void addAdditionsDialogues(AddPackFindersEvent event) {
        if (!AdditionsNpcNames.additionsLoaded()) return;
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(MOD_ID, "packs/bca_dialogues"),
                PackType.SERVER_DATA, Component.literal("Rumble - Cobblemon Additions NPC Dialogues"),
                PackSource.BUILT_IN, true, Pack.Position.TOP);
    }

    private static void addLegacyTrainerTeams(AddPackFindersEvent event) {
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(MOD_ID, "packs/legacy_trainer_teams"),
                PackType.SERVER_DATA, Component.literal("Rumble - Incandescent Verity Trainer Teams"),
                PackSource.BUILT_IN, true, Pack.Position.TOP);
    }

    private static void addExternalPacks(AddPackFindersEvent event) {
        if (Boolean.getBoolean("rumble.disableExternalPacks")) return;
        Path root = FMLPaths.GAMEDIR.get().resolve("global_packs");
        boolean installed = Files.isDirectory(root);
        if (!installed) {
            String development = System.getProperty("rumble.developmentPacks");
            if (development == null || development.isBlank()) return;
            root = Path.of(development);
        }

        addPackDirectory(event, root.resolve("both"));
        if (event.getPackType() == PackType.SERVER_DATA) {
            addPackDirectory(event, root.resolve("datapacks"));
        } else if (!installed) {
            // Installed client-only packs are already discovered by Minecraft in resourcepacks/.
            addPackDirectory(event, root.resolve("resourcepacks"));
        }
    }

    private static void addPackDirectory(AddPackFindersEvent event, Path directory) {
        if (Files.isDirectory(directory)) {
            var validator = new DirectoryValidator(target -> false);
            event.addRepositorySource(new FolderRepositorySource(directory, event.getPackType(), PackSource.DEFAULT, validator));
        }
    }
}
