package com.p1nero.cobblemon.rumblecore.prologue;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.logging.LogUtils;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

/** The hall owns one saved entity per role. Arena trainers never enter this registry. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class FixedStoryActors {
    public static final String PROFESSOR = "professor", MAY = "may", MERCHANT = "trial_merchant";
    private static final String JESSIE = "restock_jessie", JAMES = "restock_james", MEOWTH = "restock_meowth";
    private static final List<String> ROCKET = List.of(JESSIE, JAMES, MEOWTH);
    private static final List<String> TERA_ROCKET = List.of(TeraChapter.CASSIDY.id(), TeraChapter.BUTCH.id());
    private static final List<String> GATED = List.of(MAY, MERCHANT, JESSIE, JAMES, MEOWTH,
            TeraChapter.CASSIDY.id(), TeraChapter.BUTCH.id());
    private static final String TAG = CobblemonRumbleCore.MOD_ID + ":fixed_story_actor";
    private static final String HOME = TAG + "_home";
    private static final String SCENE_ASPECT = "rumble-scene-actor";
    private static final String COMMAND = "rumble.command.refreshnpc.";
    private static final Logger LOG = LogUtils.getLogger();
    private static final TicketType<ChunkPos> TICKET = TicketType.create("rumble_fixed_story_actors", Comparator.comparingLong(ChunkPos::toLong));
    private static final int LOAD_RANGE = 2, TIMEOUT = 20 * 60;
    private static final SavedData.Factory<Placement> PLACEMENT = new SavedData.Factory<>(Placement::new, (tag, lookup) -> {
        var result = new Placement();
        result.initialized = tag.getBoolean("initialized");
        return result;
    });
    private static final Map<MinecraftServer, Batch> BATCHES = new LinkedHashMap<>();

    private record Role(String id, UUID uuid, Class<? extends Mob> type, BlockPos pos, Function<ServerLevel, Mob> factory) {
        Role(String id, Class<? extends Mob> type, BlockPos pos, Function<ServerLevel, Mob> factory) {
            this(id, identity(id), type, pos, factory);
        }
    }

    private static final class Catalog {
        private static final Vec3 ORIGIN = Vec3.atBottomCenterOf(PrologueWorld.PROFESSOR);
        private static final Map<String, Role> ROLES = new LinkedHashMap<>();
        static {
            add(new Role(PROFESSOR, ProfessorEntity.class, PrologueWorld.PROFESSOR, FixedStoryActors::createProfessor));
            add(new Role(MAY, NPCEntity.class, MegaIntroduction.MAY_POS, MegaIntroduction::createActor));
            add(new Role(MERCHANT, TrialMerchantEntity.class, WanderingMerchantQuest.POS, level -> {
                var merchant = TrialMerchantEntity.TYPE.get().create(level);
                if (merchant != null) merchant.moveTo(WanderingMerchantQuest.POS.getX() + .5, WanderingMerchantQuest.POS.getY(),
                        WanderingMerchantQuest.POS.getZ() + .5, 180, 0);
                return merchant;
            }));
            var left = ORIGIN.add(-2.15, 0, -0.65);
            var right = ORIGIN.add(2.15, 0, -0.65);
            var center = ORIGIN.add(0, 0, -2);
            add(new Role(JESSIE, NPCEntity.class, BlockPos.containing(left),
                    level -> GymStory.newTrainer(level, ProfessorRestock.jessie(), left, 0)));
            add(new Role(JAMES, NPCEntity.class, BlockPos.containing(right),
                    level -> GymStory.newTrainer(level, ProfessorRestock.james(), right, 0)));
            add(new Role(MEOWTH, PokemonEntity.class, BlockPos.containing(center), level -> createMeowth(level, center)));
            for (var actor : List.of(TeraChapter.CASSIDY, TeraChapter.BUTCH))
                add(new Role(actor.id(), NPCEntity.class, actor.pos(),
                        level -> GymStory.newTrainer(level, actor, Vec3.atBottomCenterOf(actor.pos()), 180)));
            for (var actor : GymStory.hallActors())
                add(new Role(actor.id(), NPCEntity.class, actor.pos(), level -> GymStory.createHallTrainer(level, actor)));
        }

        private static void add(Role role) {
            if (ROLES.putIfAbsent(role.id(), role) != null) throw new IllegalStateException("Duplicate hall role: " + role.id());
        }
    }

    private static final class Placement extends SavedData {
        private boolean initialized;

        @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
            tag.putBoolean("initialized", initialized);
            return tag;
        }
    }

    private record Batch(ServerLevel level, Placement placement, Set<ChunkPos> chunks, int timeout, CommandSourceStack requester) {
        void ticket(boolean acquire) {
            var source = level.getChunkSource();
            for (var chunk : chunks) {
                if (acquire) source.addRegionTicket(TICKET, chunk, LOAD_RANGE, chunk);
                else source.removeRegionTicket(TICKET, chunk, LOAD_RANGE, chunk);
            }
        }
    }

    public interface Tracking {
        void rumble$updateStoryAudience(Entity entity, ServerPlayer player);
    }

    public interface TrackedActor {
        void rumble$updateViewer(ServerPlayer player);
    }

    private FixedStoryActors() {}

    static void initialize(ServerLevel level) {
        var placement = placement(level);
        if (!placement.initialized) start(level, placement, null);
    }

    private static Placement placement(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(PLACEMENT, CobblemonRumbleCore.MOD_ID + "_fixed_story_actors");
    }

    private static boolean start(ServerLevel level, Placement placement, CommandSourceStack requester) {
        var server = level.getServer();
        if (BATCHES.containsKey(server)) return false;
        var chunks = Catalog.ROLES.values().stream().map(r -> new ChunkPos(r.pos())).collect(Collectors.toSet());
        var batch = new Batch(level, placement, chunks, server.getTickCount() + TIMEOUT, requester);
        BATCHES.put(server, batch);
        batch.ticket(true);
        return true;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rumble")
                .then(Commands.literal("refreshnpc").requires(source -> source.hasPermission(2)).executes(ctx -> {
                    var source = ctx.getSource();
                    var level = source.getServer().getLevel(PrologueWorld.GYMS);
                    if (level == null) {
                        source.sendFailure(Component.translatable(COMMAND + "dimension"));
                        return 0;
                    }
                    if (!start(level, placement(level), source)) {
                        source.sendFailure(Component.translatable(COMMAND + "busy"));
                        return 0;
                    }
                    source.sendSuccess(() -> Component.translatable(COMMAND + "queued"), false);
                    return 1;
                })));
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        var batch = BATCHES.get(server);
        if (batch == null) return;
        // Full terrain chunks are not proof that entity data has arrived from disk.
        var level = batch.level();
        if (!batch.chunks().stream().allMatch(chunk -> level.areEntitiesLoaded(chunk.toLong())
                && level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null)) {
            if (server.getTickCount() - batch.timeout() >= 0) {
                release(server, batch);
                LOG.error("Timed out loading Trial Hall NPC chunks; /rumble refreshnpc can retry");
                if (batch.requester() != null) batch.requester().sendFailure(Component.translatable(COMMAND + "timeout"));
            }
            return;
        }
        try {
            int kept = 0, added = 0, failed = 0;
            for (var role : Catalog.ROLES.values()) {
                if (find(level, role.id()) != null) {
                    kept++;
                } else if (place(level, role)) {
                    added++;
                } else {
                    failed++;
                }
            }
            batch.placement().initialized = true;
            batch.placement().setDirty();
            LOG.info("Trial Hall NPC check: {} kept, {} placed, {} failed", kept, added, failed);
            if (batch.requester() != null) {
                var summary = Component.translatable(COMMAND + "result", kept, added, failed);
                if (failed > 0) batch.requester().sendFailure(summary);
                else batch.requester().sendSuccess(() -> summary, false);
            }
        } finally {
            release(server, batch);
        }
    }

    private static boolean place(ServerLevel level, Role role) {
        try {
            var actor = role.factory().apply(level);
            if (actor != null) {
                actor.setUUID(role.uuid());
                actor.getPersistentData().putString(TAG, role.id());
                var home = new CompoundTag();
                home.putDouble("x", actor.getX());
                home.putDouble("y", actor.getY());
                home.putDouble("z", actor.getZ());
                actor.getPersistentData().put(HOME, home);
                if (level.addFreshEntity(actor)) return true;
            }
            LOG.error("Could not place Trial Hall NPC {} at {}", role.id(), role.pos());
        } catch (RuntimeException e) {
            LOG.error("Could not place Trial Hall NPC {} at {}", role.id(), role.pos(), e);
        }
        return false;
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        var batch = BATCHES.get(event.getServer());
        if (batch != null) release(event.getServer(), batch);
    }

    private static void release(MinecraftServer server, Batch batch) {
        BATCHES.remove(server);
        batch.ticket(false);
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !PrologueWorld.GYMS.equals(level.dimension())) return;
        var entity = event.getEntity();
        var id = role(entity);
        if (id.isEmpty()) return;
        var role = Catalog.ROLES.get(id);
        // Vanilla rejects repeated UUIDs; a clone with an altered UUID gets no second identity.
        if (role == null || !role.type().isInstance(entity) || !role.uuid().equals(entity.getUUID())) {
            event.setCanceled(true);
            return;
        }
        var actor = (Mob) entity;
        if (GymCatalog.GRASS_SECOND_ATTENDANT.id().equals(id)
                && actor.chunkPosition().equals(new ChunkPos(role.pos()))) {
            // Both positions are in the same chunk, so the existing saved actor keeps its identity.
            var feet = Vec3.atBottomCenterOf(role.pos());
            if (actor.distanceToSqr(feet) > 1.0E-4 && level.loadedAndEntityCanStandOn(role.pos().below(), actor)
                    && level.noCollision(actor, actor.getBoundingBox().move(feet.subtract(actor.position())))) {
                actor.setPos(feet);
                actor.setDeltaMovement(Vec3.ZERO);
            }
        }
        actor.setPersistenceRequired();
        actor.setNoAi(true);
        actor.setNoGravity(false);
        actor.setInvulnerable(true);
        actor.noPhysics = false;
        if (actor instanceof PokemonEntity body) {
            body.getEntityData().set(PokemonEntity.getUNBATTLEABLE(), true);
            var pokemon = body.getPokemon();
            if (!pokemon.getForcedAspects().contains(SCENE_ASPECT)) {
                var aspects = new HashSet<>(pokemon.getForcedAspects());
                aspects.add(SCENE_ASPECT);
                pokemon.setForcedAspects(aspects);
                pokemon.updateAspects();
            }
        }
    }

    /** Cobblemon synchronizes aspects to clients, unlike the fixed actor role stored in persistent NBT. */
    public static boolean protectedFromCombat(Entity entity) {
        return entity instanceof NPCEntity || entity instanceof ProfessorEntity || entity instanceof TrialMerchantEntity
                || entity instanceof PokemonEntity body && (body.getAspects().contains(SCENE_ASPECT) || isFixedInHall(body));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protect(LivingIncomingDamageEvent event) {
        if (protectedFromCombat(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void reset(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob actor) || actor.tickCount % 100 != 0
                || !(actor.level() instanceof ServerLevel level) || !isFixedInHall(actor)) return;
        var role = Catalog.ROLES.get(role(actor));
        if (role == null || !role.uuid().equals(actor.getUUID())) return;
        var saved = actor.getPersistentData().getCompound(HOME);
        var feet = saved.contains("x") ? new Vec3(saved.getDouble("x"), saved.getDouble("y"), saved.getDouble("z"))
                : Vec3.atBottomCenterOf(role.pos());
        var home = BlockPos.containing(feet);
        boolean verticallyDisplaced = Math.abs(actor.getY() - home.getY()) > 5;
        if (!verticallyDisplaced && actor.getBlockX() == home.getX() && actor.getBlockZ() == home.getZ()) return;
        if (!level.hasChunkAt(home) || !level.noCollision(actor, actor.getBoundingBox().move(feet.subtract(actor.position())))) return;
        actor.teleportTo(feet.x, feet.y, feet.z);
        actor.setDeltaMovement(Vec3.ZERO);
        actor.resetFallDistance();
    }

    private static ProfessorEntity createProfessor(ServerLevel level) {
        var actor = ProfessorEntity.TYPE.get().create(level);
        if (actor == null) return null;
        var pos = Vec3.atBottomCenterOf(PrologueWorld.PROFESSOR);
        actor.moveTo(pos.x, pos.y, pos.z, 45, 0);
        actor.setYBodyRot(45);
        actor.setYHeadRot(45);
        return actor;
    }

    private static PokemonEntity createMeowth(ServerLevel level, Vec3 pos) {
        var actor = PokemonProperties.Companion.parse("meowth level=20 uncatchable=true").createEntity(level);
        actor.setCustomName(Component.translatable(ProfessorRestock.MEOWTH));
        actor.setSilent(true);
        actor.moveTo(pos.x, pos.y, pos.z, 0, 0);
        return actor;
    }

    private static UUID identity(String id) {
        return UUID.nameUUIDFromBytes((TAG + "/" + id).getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isFixedInHall(Entity actor) {
        return PrologueWorld.GYMS.equals(actor.level().dimension()) && !role(actor).isEmpty();
    }

    public static String role(Entity entity) {
        return entity == null ? "" : entity.getPersistentData().getString(TAG);
    }

    static Entity find(ServerLevel level, String id) {
        if (!PrologueWorld.GYMS.equals(level.dimension())) return null;
        var role = Catalog.ROLES.get(id);
        if (role == null) return null;
        var entity = level.getEntity(role.uuid());
        return entity != null && entity.isAlive() && role.id().equals(role(entity)) ? entity : null;
    }

    public static boolean mayTrack(Entity entity, ServerPlayer player) {
        if (!PrologueWorld.GYMS.equals(entity.level().dimension())) return true;
        var id = role(entity);
        if (MAY.equals(id)) return MegaIntroduction.visible(player);
        if (MERCHANT.equals(id)) return WanderingMerchantQuest.visible(player);
        if (TERA_ROCKET.contains(id)) return TeraChapter.visible(player);
        return !ROCKET.contains(id) || RestockScene.visible(player);
    }

    @SubscribeEvent
    public static void audience(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !PrologueWorld.GYMS.equals(player.level().dimension())) return;
        // Vanilla doesn't reevaluate stationary tracking immediately when just a quest/session changes.
        var level = player.serverLevel();
        var tracker = (Tracking) level.getChunkSource().chunkMap;
        for (var id : GATED) {
            var actor = find(level, id);
            if (actor != null) tracker.rumble$updateStoryAudience(actor, player);
        }
    }

    private static boolean blockVanillaInteraction(Entity entity) {
        return PrologueWorld.GYMS.equals(entity.level().dimension()) && GATED.contains(role(entity));
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (blockVanillaInteraction(event.getTarget())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void interactSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (blockVanillaInteraction(event.getTarget())) event.setCanceled(true);
    }

    static List<Entity> rocketActors(ServerLevel level) {
        return ROCKET.stream().map(id -> find(level, id)).filter(java.util.Objects::nonNull).toList();
    }
}
