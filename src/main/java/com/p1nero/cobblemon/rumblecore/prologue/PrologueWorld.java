package com.p1nero.cobblemon.rumblecore.prologue;

import com.google.gson.JsonParser;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaTheme;
import com.p1nero.cobblemon.rumblecore.gym.CinemaTrials;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog;
import com.p1nero.cobblemon.rumblecore.gym.LegendaryTrials;
import com.p1nero.cobblemon.rumblecore.gym.MegaTrials;
import com.p1nero.misc.custom_instances.instance.InstanceDefinition;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.storage.DefinitionStore;
import com.p1nero.misc.simple_player_data_manager.api.BoolData;
import com.p1nero.misc.simple_player_data_manager.api.PlayerDataRegister;
import com.p1nero.misc.simple_player_data_manager.api.SyncMode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class PrologueWorld {
    public static final ResourceKey<Level> GYMS = ResourceKey.create(Registries.DIMENSION, id("gyms"));
    public static final BlockPos ANCHOR = new BlockPos(-14, 50, 0);
    public static final BlockPos ARRIVAL = ANCHOR.west(2);
    public static final BlockPos PROFESSOR = ANCHOR.north(2);
    // Keep in sync with the prebuilt hall placements in scripts/install-hall-equipment.py.
    public static final BlockPos HEALING_MACHINE = ANCHOR.west(5);
    public static final BlockPos STORAGE_PC = ANCHOR.east(5);
    public static final BoolData ARRIVED = PlayerDataRegister.create(CobblemonRumbleCore.MOD_ID)
            .boolData("arrived_in_gyms", false, SyncMode.SERVER_ONLY);

    private PrologueWorld() {}

    public static void initialize() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CobblemonRumbleCore.MOD_ID, path);
    }

    /** Keep natural wildlife and trainers out of the city and its tutorial copy. */
    public static boolean blocksAmbientSpawns(Level level) {
        return GYMS.equals(level.dimension()) || level instanceof ServerLevel serverLevel
                && InstanceManager.isInstanceDimension(level.dimension()) && (PrologueTraining.isTrainingLevel(serverLevel)
                    || ArenaMatch.isArena(serverLevel));
    }

    @SubscribeEvent
    public static void blockAmbientPosition(MobSpawnEvent.PositionCheck event) {
        var reason = event.getSpawnType();
        if ((reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.PATROL)
                && blocksAmbientSpawns(event.getLevel().getLevel())) event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
    }

    @SubscribeEvent
    public static void blockVanillaWanderingTrader(EntityJoinLevelEvent event) {
        // Custom spawners can bypass PositionCheck. Match the vanilla type, not the fixed merchant subclass.
        if (event.getLevel() instanceof ServerLevel level && GYMS.equals(level.dimension())
                && event.getEntity().getType() == EntityType.WANDERING_TRADER) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void seedWorld(ServerAboutToStartEvent event) {
        try {
            installDefinition(PrologueTraining.TEMPLATE);
            installDefinition(ArenaMatch.TEMPLATE);
            installDefinition(ChampionStory.TEMPLATE);
            Path definitions = DefinitionStore.configRoot().resolve("definitions");
            var store = new DefinitionStore(definitions);
            ArenaTheme.install(store, readDefinition(definitions.resolve(ArenaMatch.TEMPLATE + ".json")));
            for (var gym : GymCatalog.Gym.values()) {
                String id = ArenaMatch.leaderTemplate(gym);
                installDefinition(id);
                installDefinition(ArenaMatch.leaderTemplate(gym, true));
                var definition = readDefinition(definitions.resolve(id + ".json"));
                var theme = ArenaTheme.gym(gym.path());
                if (definition.chunkSource().equals(ArenaTheme.DEFAULT.chunks()) || definition.chunkSource().equals(theme.chunks())) {
                    String chunks = theme.available().chunks();
                    if (!definition.chunkSource().equals(chunks)) store.save(definition.withChunkSource(chunks));
                }
            }
            String megaTable = MegaTrials.TABLE;
            Path table = FMLPaths.CONFIGDIR.get().resolve(megaTable);
            install("/defaults/" + megaTable, table);
            MegaTrials.install(table);
            var arena = readDefinition(definitions.resolve(ArenaMatch.TEMPLATE + ".json"));
            var champion = readDefinition(definitions.resolve(ChampionStory.TEMPLATE + ".json"));
            var championChunks = ArenaTheme.SKY.available().chunks();
            if (!champion.chunkSource().equals(championChunks)) store.save(champion.withChunkSource(championChunks));
            LegendaryTrials.install(store, arena);
            CinemaTrials.install(store, arena);

            Path templates = DefinitionStore.configRoot().resolve("region");
            requireTemplate(templates.resolve("prologue_plaza"));
            requireTemplate(templates.resolve("rumble_battle_arena"));
            Path map = FMLPaths.CONFIGDIR.get().resolve(CobblemonRumbleCore.MOD_ID).resolve("maps/gyms");
            Path root = event.getServer().getWorldPath(LevelResource.ROOT)
                    .resolve("dimensions").resolve(GYMS.location().getNamespace()).resolve(GYMS.location().getPath());
            HallMap.install(map, root);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load Rumble maps. Deploy modpack_override/config: " + e.getMessage(), e);
        }
    }

    private static InstanceDefinition readDefinition(Path file) throws IOException {
        try (var reader = Files.newBufferedReader(file)) {
            return DefinitionStore.fromJson(JsonParser.parseReader(reader).getAsJsonObject());
        }
    }

    private static void installDefinition(String template) throws IOException {
        String definition = "custom_instances/definitions/" + template + ".json";
        install("/defaults/" + definition, DefinitionStore.configRoot().getParent().resolve(definition));
    }

    private static void install(String resource, Path dest) throws IOException {
        if (Files.exists(dest)) return;
        try (var input = PrologueWorld.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Missing resource: " + resource);
            install(input, dest);
        }
    }

    private static void requireTemplate(Path template) throws IOException {
        try (var regions = Files.newDirectoryStream(template.resolve("region"), "r.*.*.mca")) {
            if (!regions.iterator().hasNext()) throw new IOException("No region files in " + template);
        }
    }

    private static void install(InputStream input, Path dest) throws IOException {
        Files.createDirectories(dest.getParent());
        Path tmp = Files.createTempFile(dest.getParent(), ".rumble-", ".tmp");
        try {
            Files.copy(input, tmp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmp, dest, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @SubscribeEvent
    public static void initializeHall(ServerStartedEvent event) {
        // Initialize the pack default once per world; later administrator changes remain effective.
        var server = event.getServer();
        var defaultsId = id("world_defaults");
        var storage = server.getCommandStorage();
        var defaults = storage.get(defaultsId).copy();
        if (!defaults.getBoolean("keep_inventory_initialized")) {
            server.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, server);
            defaults.putBoolean("keep_inventory_initialized", true);
            storage.set(defaultsId, defaults);
        }
        ServerLevel level = event.getServer().getLevel(GYMS);
        if (level == null) throw new IllegalStateException("Missing Rumble Gyms dimension");
        FixedStoryActors.initialize(level);
        TrialWaystones.install(level);
    }

    public static boolean arrive(ServerPlayer player) {
        ServerLevel level = player.server.getLevel(GYMS);
        if (level == null) return false;
        player.teleportTo(level, ARRIVAL.getX() + 0.5, ARRIVAL.getY(), ARRIVAL.getZ() + 0.5, -135, 0);
        player.setRespawnPosition(level.dimension(), ARRIVAL, -135, true, false);
        return player.serverLevel() == level;
    }
}
