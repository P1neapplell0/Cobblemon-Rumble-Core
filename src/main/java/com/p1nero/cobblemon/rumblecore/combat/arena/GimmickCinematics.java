package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.logging.LogUtils;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.duel.Duels;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.client.combat.GimmickCinematicScreen;
import com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat.GimmickBrawlAccess;
import com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat.BrawlFoeAccess;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.EventPriority;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Server-owned cutscene transaction. Never pauses the server or trusts a client to commit a gimmick. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class GimmickCinematics {
    public enum Kind { MEGA_EVOLUTION, DYNAMAX, Z_MOVE, TERASTALIZATION }
    // Cinematics 1.4.0: the Pokémon reveal starts at 2020 ms in its 5600 ms timeline.
    public static final int TRAINER_INTRO_MS = 2020;
    public static final int ANIMATION_MS = 5600 - TRAINER_INTRO_MS;
    private static final int MIN_TICKS = (ANIMATION_MS + 49) / 50, TIMEOUT_TICKS = MIN_TICKS + 48;
    private static final Set<UUID> CAPABLE = new HashSet<>(); // Keep the handshake across a vanilla player respawn.
    private static final Map<Entity, Scene> LOCKS = new IdentityHashMap<>();
    private static final Map<ArenaMatch, Scene> ARENAS = new IdentityHashMap<>();
    private static final Map<UUID, Scene> SCENES = new HashMap<>();
    private static ServerPlayer releasingZ;

    private record Scene(UUID id, ServerLevel level, ArenaMatch match, PokemonEntity body, Smash.Session session,
                         long earliest, long timeout, Set<Entity> entities, Set<ServerPlayer> viewers,
                         Set<ServerPlayer> waiting, Runnable commit) {}

    private GimmickCinematics() {}

    public static boolean locked(Entity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel)) return false;
        var scene = LOCKS.get(entity);
        return scene != null && entity.level() == scene.level();
    }

    public static boolean blocksDamage(DamageSource source) {
        return locked(source.getEntity()) || locked(source.getDirectEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protect(LivingIncomingDamageEvent event) {
        if (locked(event.getEntity()) || blocksDamage(event.getSource())) event.setCanceled(true);
    }

    static void leaving(ArenaMatch match, ServerPlayer player) {
        var scene = ARENAS.get(match);
        if (scene != null && (scene.session() != null && scene.session().player == player
                || scene.viewers().stream().noneMatch(p -> p != player && p.level() == scene.level() && !p.hasDisconnected()))) {
            finish(scene, false);
        }
    }

    static boolean paused(ArenaMatch match) { return ARENAS.containsKey(match); }

    public static boolean start(Smash.Session session, Kind kind, Runnable commit) {
        return start(session.pokemon, session.player, session, kind, commit);
    }

    static boolean start(PokemonEntity body, Entity trainer, Smash.Session session, Kind kind, Runnable commit) {
        if (!(body.level() instanceof ServerLevel level) || !body.isAlive() || body.getPokemon().isFainted()) return false;
        var match = ArenaMatch.group(body);
        // Team-vs-team matches can contain many simultaneous players. Skip only the
        // cinematic presentation there; the caller's normal gimmick commit still
        // performs the Mega/Tera transformation. This avoids locking the whole team
        // behind several overlapping cutscenes.
        if (match != null && match.isTeamGame()) return false;
        var entities = new HashSet<Entity>();
        var viewers = new HashSet<ServerPlayer>();
        entities.add(body);
        if (session != null) add(session, entities, viewers);
        if (match != null) match.cinematicAudience(entities, viewers);
        else if (session != null) {
            var duel = Duels.groupOf(session.player);
            if (duel != null) {
                for (var opponent : Smash.sessions()) {
                    if (opponent.player.level() == level && Duels.groupOf(opponent.player) == duel) add(opponent, entities, viewers);
                }
            }
            // Include wild opponents already engaged with this player, even without a lock-on.
            GimmickBrawlAccess.rumble$foes().forEach((pokemon, foe) -> {
                if (pokemon.level() == level && ((BrawlFoeAccess) foe).rumble$player() == session.player) entities.add(pokemon);
            });
        }
        viewers.removeIf(player -> player.level() != level || player.hasDisconnected());
        var waiting = new HashSet<>(viewers);
        waiting.removeIf(player -> !CAPABLE.contains(player.getUUID()));
        if (waiting.isEmpty() || paused(match) || entities.stream().anyMatch(GimmickCinematics::locked)) return false;

        long now = level.getGameTime();
        var scene = new Scene(UUID.randomUUID(), level, match, body, session, now + MIN_TICKS, now + TIMEOUT_TICKS,
                Set.copyOf(entities), Set.copyOf(viewers), waiting, commit);
        SCENES.put(scene.id(), scene);
        if (match != null) ARENAS.put(match, scene);
        for (var entity : entities) {
            LOCKS.put(entity, scene);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.hurtMarked = true;
            if (entity instanceof PokemonEntity pokemon) pokemon.getNavigation().stop();
            if (entity instanceof ServerPlayer player) player.stopUsingItem();
        }
        var packet = new Start(scene.id(), level.dimension().location(), kind.ordinal(), body.getId(),
                trainer == null ? new UUID(0, 0) : trainer.getUUID(), body.getPokemon());
        viewers.forEach(player -> PacketDistributor.sendToPlayer(player, packet));
        return true;
    }

    private static void add(Smash.Session session, Set<Entity> entities, Set<ServerPlayer> viewers) {
        entities.add(session.player);
        entities.add(session.pokemon);
        entities.addAll(session.combat.decoys);
        viewers.add(session.player);
    }

    public static boolean releasingZ(ServerPlayer player) { return releasingZ == player; }

    public static void releaseZ(ServerPlayer player, Runnable action) {
        releasingZ = player;
        try { action.run(); } finally { releasingZ = null; }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        for (var scene : List.copyOf(SCENES.values())) {
            long now = scene.level().getGameTime();
            var session = scene.session();
            boolean valid = scene.body().isAlive() && !scene.body().isRemoved() && !scene.body().getPokemon().isFainted()
                    && scene.body().level() == scene.level() && ArenaMatch.group(scene.body()) == scene.match()
                    && (session == null || session.player.level() == scene.level() && !session.player.hasDisconnected()
                    && session.player.isAlive() && Smash.session(session.player) == session && session.pokemon == scene.body());
            scene.waiting().removeIf(p -> p.hasDisconnected() || p.level() != scene.level());
            if (!valid || now >= scene.timeout() || now >= scene.earliest() && scene.waiting().isEmpty()) finish(scene, valid);
        }
    }

    private static void finish(Scene scene, boolean commit) {
        SCENES.remove(scene.id());
        if (scene.match() != null) ARENAS.remove(scene.match(), scene);
        scene.entities().forEach(entity -> {
            LOCKS.remove(entity, scene);
            if (entity.level() == scene.level()) entity.setDeltaMovement(Vec3.ZERO);
        });
        scene.viewers().stream().filter(p -> !p.hasDisconnected()).forEach(p -> PacketDistributor.sendToPlayer(p, new Finish(scene.id())));
        if (commit) {
            try { scene.commit().run(); }
            catch (RuntimeException e) { LogUtils.getLogger().error("Failed to complete real-time gimmick cinematic {}", scene.id(), e); }
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) CAPABLE.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        SCENES.clear(); ARENAS.clear(); LOCKS.clear(); CAPABLE.clear();
    }

    public static void register(PayloadRegistrar net) {
        net.playToServer(Ready.TYPE, Ready.CODEC, (p, ctx) -> CAPABLE.add(ctx.player().getUUID()));
        net.playToServer(Seen.TYPE, Seen.CODEC, (p, ctx) -> {
            var scene = SCENES.get(p.id());
            if (scene != null) scene.waiting().remove(ctx.player());
        });
        net.playToClient(Start.TYPE, Start.CODEC, (p, ctx) -> GimmickCinematicScreen.start(p));
        net.playToClient(Finish.TYPE, Finish.CODEC, (p, ctx) -> GimmickCinematicScreen.finish(p.id()));
    }

    public record Ready() implements CustomPacketPayload {
        static final Type<Ready> TYPE = new Type<>(PrologueWorld.id("gimmick_ready"));
        static final StreamCodec<RegistryFriendlyByteBuf, Ready> CODEC = StreamCodec.unit(new Ready());
        @Override public Type<Ready> type() { return TYPE; }
    }

    public record Seen(UUID id) implements CustomPacketPayload {
        static final Type<Seen> TYPE = new Type<>(PrologueWorld.id("gimmick_seen"));
        static final StreamCodec<RegistryFriendlyByteBuf, Seen> CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Seen::id, Seen::new);
        @Override public Type<Seen> type() { return TYPE; }
    }

    public record Start(UUID id, ResourceLocation dimension, int kind, int entityId, UUID trainer,
                        Pokemon pokemon) implements CustomPacketPayload {
        static final Type<Start> TYPE = new Type<>(PrologueWorld.id("gimmick_start"));
        static final StreamCodec<RegistryFriendlyByteBuf, Start> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Start::id, ResourceLocation.STREAM_CODEC, Start::dimension,
                ByteBufCodecs.VAR_INT, Start::kind, ByteBufCodecs.VAR_INT, Start::entityId,
                UUIDUtil.STREAM_CODEC, Start::trainer, Pokemon.Companion.getS2C_CODEC(), Start::pokemon, Start::new);
        @Override public Type<Start> type() { return TYPE; }
    }

    public record Finish(UUID id) implements CustomPacketPayload {
        static final Type<Finish> TYPE = new Type<>(PrologueWorld.id("gimmick_finish"));
        static final StreamCodec<RegistryFriendlyByteBuf, Finish> CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Finish::id, Finish::new);
        @Override public Type<Finish> type() { return TYPE; }
    }
}
