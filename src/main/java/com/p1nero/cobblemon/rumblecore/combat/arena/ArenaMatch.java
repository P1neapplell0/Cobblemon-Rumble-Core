package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.api.storage.party.NPCPartyStore;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.SmashConfig;
import com.johnn.realtimecobblemon.network.SmashPayloads;
import com.johnn.realtimecobblemon.duel.Duels;
import com.johnn.realtimecobblemon.moves.Brawl;
import com.johnn.realtimecobblemon.moves.Conditions;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.RumbleConfig;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.gym.MegaTrials;
import com.p1nero.cobblemon.rumblecore.prologue.MegaSimulator;
import com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets;
import com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets.Cue;
import com.p1nero.misc.custom_instances.api.ExitReason;
import com.p1nero.misc.custom_instances.api.InstanceApi;
import com.p1nero.misc.custom_instances.api.InstanceEvent;
import com.p1nero.misc.custom_instances.instance.InstanceDefinition;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.instance.InstanceSession;
import com.p1nero.misc.custom_instances.network.InstanceNetwork;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import kotlin.Unit;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

/** One isolated, server-authoritative single battle. The real party is never mutated by competitive moves. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class ArenaMatch extends InstanceSession {
    private static final String GYM_WIN = "rumble:arena_win";
    private static final String PARTY_TUTORIAL = "rumble.arena.party_tutorial";
    public static final String TEMPLATE = "rumble_battle";
    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<UUID, Invitation> INVITES = new HashMap<>();
    private static final Map<UUID, LeaderLobby> LEADER_LOBBIES = new HashMap<>();
    private static final int RECALL_TICKS = 55;
    private static final ChunkPos ARENA_CENTER = new ChunkPos(0, 0);
    private static final TicketType<UUID> COMBAT_TICKET = TicketType.create("rumble_arena_combat", UUID::compareTo);
    private static final int COMBAT_TICKET_DISTANCE = 5;
    private static final Vec3 WEST = new Vec3(-23.5, 64, 0.5), EAST = new Vec3(24.5, 64, 0.5);

    // Battle arena template: west blue concrete occupies x=-24..-21, z=-3..3.
    private static final int TEAM_HINT_INTERVAL = 25;
    private static final double BLUE_MIN_X = -24, BLUE_MAX_X = -20, BLUE_MIN_Z = -3, BLUE_MAX_Z = 4;
    private static final Vec3 GYM_WEST_LEAD = new Vec3(-8.5, 64, 0.5), GYM_EAST_LEAD = new Vec3(9.5, 64, 0.5);

    // Scoped to the synchronous InstanceManager.create call; never populated by a client or public room command.
    private static Setup creating;
    private Setup setup;
    private final GymCatalog.Encounter leader;
    private final MegaTrials.Trial megaTrial;
    private final Map<UUID, Fighter> team = new LinkedHashMap<>();
    private Fighter a, b;
    private final List<Trainer> trainers = new ArrayList<>();
    private int battleGate, trainerResultAt;
    private boolean trainerWon, fighting, trainerResolved;
    private int lessonBattleAt;

    private record Invitation(UUID id, UUID challenger, UUID recipient, UUID lead, int expires) {}
    private record LeaderLobby(GymCatalog.Encounter encounter, UUID speaker, int expires) {}
    public interface StoryChallenge {
        List<GymCatalog.Actor> actors();
        boolean eligible(ServerPlayer player);
        void result(ServerPlayer player, boolean won);
        default UUID megaLessonLead() { return null; }
        default UUID teraLessonLead() { return null; }
        default ArenaTheme arena(ServerPlayer player) { return ArenaTheme.DEFAULT; }
        default void closed() { }
        default boolean canSkip() { return true; }
    }

    private record Setup(UUID owner, UUID guest, GymCatalog.Encounter encounter, UUID speaker, StoryChallenge story) {
        boolean includes(UUID id) { return owner.equals(id) || id.equals(guest); }
        boolean hasTrainers() { return encounter != null || story != null; }
        List<GymCatalog.Actor> actors() { return story == null ? encounter.actors() : story.actors(); }
    }

    private ArenaMatch(UUID id, InstanceDefinition definition, ResourceKey<Level> dimension, UUID owner, String password) {
        this(id, definition, dimension, owner, password, null, null);
    }

    private ArenaMatch(UUID id, InstanceDefinition definition, ResourceKey<Level> dimension, UUID owner, String password,
                       GymCatalog.Encounter leader, MegaTrials.Trial megaTrial) {
        super(id, definition, dimension, owner, password);
        this.leader = leader;
        this.megaTrial = megaTrial;
        this.setup = creating != null && creating.owner().equals(owner) ? creating : null;
    }

    public static String leaderTemplate(GymCatalog.Gym gym) { return "rumble_gym_" + gym.path(); }

    public static void initialize() {
        InstanceApi.registerSessionFactory(TEMPLATE, ArenaMatch::new);
        for (ArenaTheme theme : ArenaTheme.values()) {
            if (theme != ArenaTheme.DEFAULT) InstanceApi.registerSessionFactory(theme.template(), ArenaMatch::new);
        }
        for (var gym : GymCatalog.Gym.values()) {
            var leader = GymCatalog.leader(gym);
            InstanceApi.registerSessionFactory(leaderTemplate(gym), (id, definition, dimension, owner, password) ->
                    new ArenaMatch(id, definition, dimension, owner, password, leader, null));
            var actor = leader.actors().getFirst();
            var portrait = gym == GymCatalog.Gym.ROCK
                    ? ResourceLocation.parse("rctmod:textures/trainers/single/leader_brock_0038.png")
                    : PrologueWorld.id("textures/entity/npc/" + actor.id() + ".png");
            InstanceApi.registerLobbyPresentation(leaderTemplate(gym), player -> new InstanceApi.LobbyPresentation(
                    actor.key(), portrait, !ArcQuestPlayerManager.getOrCreate(player).hasFlag(PARTY_TUTORIAL)));
        }
    }
    public static void registerMegaTrial(MegaTrials.Trial trial) {
        InstanceApi.registerSessionFactory(trial.id(), (id, definition, dimension, owner, password) ->
                new ArenaMatch(id, definition, dimension, owner, password, null, trial));
    }

    private boolean partyBattle() { return leader != null || megaTrial != null; }
    private boolean hasTrainers() { return partyBattle() || setup != null && setup.hasTrainers(); }
    private UUID lessonLead() {
        if (setup == null || setup.story() == null) return null;
        var tera = setup.story().teraLessonLead();
        return tera != null ? tera : setup.story().megaLessonLead();
    }

    private boolean gimmickLesson() { return lessonLead() != null; }
    private boolean teraLesson() { return setup != null && setup.story() != null && setup.story().teraLessonLead() != null; }

    @Override public boolean isFriendlyFireEnabled() { return !partyBattle() || super.isFriendlyFireEnabled(); }
    @Override public boolean showDefaultResult() { return false; }
    @Override public boolean showDefaultBattleReport() { return false; }
    @Override public boolean showStatusHud() { return partyBattle() && (getState() == State.WAITING || getState() == State.PREPARING); }
    @Override public int resultDelayTicks() { return 75; }

    public boolean musicActive(ServerPlayer player) {
        return getState() == State.ACTIVE && getLevel() == player.serverLevel()
                && fighting && trainerResultAt == 0 && fighter(player) != null;
    }

    public static void challenge(ServerPlayer challenger, ServerPlayer recipient, UUID lead) {
        if (!canInvite(challenger, recipient) || healthy(challenger).stream().noneMatch(p -> p.getUuid().equals(lead))) return;
        int now = challenger.server.getTickCount();
        if (INVITES.values().stream().anyMatch(i -> i.expires() > now && (i.challenger().equals(challenger.getUUID())
                || i.recipient().equals(recipient.getUUID())))) return;
        var invite = new Invitation(UUID.randomUUID(), challenger.getUUID(), recipient.getUUID(), lead, now + 300);
        INVITES.put(invite.recipient(), invite);
        PacketDistributor.sendToPlayer(recipient, new ArenaPackets.Invite(invite.id(), challenger.getDisplayName()));
        challenger.displayClientMessage(Component.translatable("rumble.arena.sent", recipient.getDisplayName()), true);
    }

    static void reply(ServerPlayer recipient, ArenaPackets.Reply packet) {
        var invite = INVITES.get(recipient.getUUID());
        if (invite == null || !invite.id().equals(packet.invitation())) return;
        INVITES.remove(invite.recipient());
        var challenger = recipient.server.getPlayerList().getPlayer(invite.challenger());
        if (!packet.accept()) {
            if (challenger != null) challenger.displayClientMessage(Component.translatable("rumble.arena.declined", recipient.getDisplayName()), true);
            return;
        }
        if (challenger == null || invite.expires() <= recipient.server.getTickCount() || !canInvite(challenger, recipient)
                || healthy(challenger).stream().noneMatch(p -> p.getUuid().equals(invite.lead()))) {
            recipient.displayClientMessage(Component.translatable("rumble.arena.invite_unavailable"), true);
            return;
        }
        start(challenger, recipient, null, null, null);
    }

    public static boolean available(ServerPlayer p) {
        return p.isAlive() && !p.isSpectator() && !p.isSleeping() && !p.isPassenger()
                && !Smash.active(p) && !Smash.inBattle(p) && Duels.groupOf(p) == null && InstanceApi.find(p).isEmpty();
    }

    private static boolean canInvite(ServerPlayer from, ServerPlayer to) {
        return from != to && from.serverLevel() == to.serverLevel() && from.distanceToSqr(to) <= 36
                && from.hasLineOfSight(to) && from.server.isPvpAllowed() && SmashConfig.<Boolean>get(SmashConfig.ENABLED)
                && available(from) && available(to) && !healthy(to).isEmpty();
    }

    private static List<Pokemon> healthy(ServerPlayer player) {
        return Cobblemon.INSTANCE.getStorage().getParty(player).toGappyList().stream()
                .filter(p -> p != null && !p.isFainted() && p.getTetheringId() == null).toList();
    }

    public static void challengeTrainer(ServerPlayer player, GymCatalog.Encounter encounter, UUID speaker) {
        if (!available(player) || !GymStory.canChallenge(player, encounter) || !SmashConfig.<Boolean>get(SmashConfig.ENABLED)) return;
        if (healthy(player).isEmpty()) {
            player.displayClientMessage(Component.translatable("rumble.gym.no_party"), true);
            return;
        }
        if (!encounter.leader()) {
            start(player, null, encounter, speaker, null);
            return;
        }
        LEADER_LOBBIES.put(player.getUUID(), new LeaderLobby(encounter, speaker, player.server.getTickCount() + 12000));
        InstanceNetwork.openLobby(player, leaderTemplate(encounter.gym()));
    }

    public static boolean challengeStory(ServerPlayer player, StoryChallenge story) {
        if (!available(player) || !story.eligible(player) || !SmashConfig.<Boolean>get(SmashConfig.ENABLED)) return false;
        if (healthy(player).isEmpty()) {
            player.displayClientMessage(Component.translatable("rumble.gym.no_party"), true);
            return false;
        }
        return start(player, null, null, null, story);
    }

    private static boolean start(ServerPlayer owner, ServerPlayer guest, GymCatalog.Encounter encounter, UUID speaker, StoryChallenge story) {
        var manager = InstanceManager.get(owner.server);
        var theme = encounter != null ? ArenaTheme.gym(encounter.gym().path())
                : story != null ? story.arena(owner) : guest != null
                        ? ArenaTheme.biome(owner.serverLevel(), owner.blockPosition()) : ArenaTheme.DEFAULT;
        String template = theme.available().template();
        String password = String.format(java.util.Locale.ROOT, "%04d", ThreadLocalRandom.current().nextInt(10000));
        try {
            UUID room;
            creating = new Setup(owner.getUUID(), guest == null ? null : guest.getUUID(), encounter, speaker, story);
            try { room = manager.create(owner, template, password); }
            finally { creating = null; }
            if (guest != null) manager.join(guest, template, room, password);
            manager.setReady(owner, room, true);
            if (guest != null) manager.setReady(guest, room, true);
            manager.start(owner, template);
            return true;
        } catch (IOException | RuntimeException e) {
            if (InstanceApi.find(owner).orElse(null) instanceof ArenaMatch match) manager.end(match, ExitReason.ERROR, true);
            InstanceManager.error(owner, e);
            if (guest != null) guest.displayClientMessage(Component.translatable("rumble.arena.invite_unavailable"), true);
            return false;
        }
    }

    @SubscribeEvent
    public static void beforeJoin(InstanceEvent.BeforeJoin event) {
        if (!(event.getSession() instanceof ArenaMatch match)) return;
        var player = event.getPlayer();
        if (match.megaTrial != null) {
            if (MegaTrials.get(match.megaTrial.id()) != match.megaTrial || !MegaTrials.unlocked(player)
                    || !MegaSimulator.atTerminal(player) || !match.megaTrial.supported()
                    || !SmashConfig.<Boolean>get(SmashConfig.ENABLED) || !available(player) || healthy(player).isEmpty()) event.setCanceled(true);
            return;
        }
        if (match.leader == null) {
            if (match.setup == null || !match.setup.includes(player.getUUID())) event.setCanceled(true);
            return;
        }
        if (!available(player) || !player.level().dimension().equals(PrologueWorld.GYMS)
                || !GymStory.canChallenge(player, match.leader) || healthy(player).isEmpty()) {
            event.setCanceled(true);
            return;
        }
        if (match.setup == null) {
            var grant = LEADER_LOBBIES.get(player.getUUID());
            if (!player.getUUID().equals(match.getOwner()) || grant == null || grant.encounter() != match.leader
                    || grant.expires() <= player.server.getTickCount() || !GymStory.atTrainer(player, grant.speaker(), match.leader)) {
                event.setCanceled(true);
                return;
            }
            match.setup = new Setup(player.getUUID(), null, match.leader, grant.speaker(), null);
        }
    }

    @SubscribeEvent
    public static void started(InstanceEvent.Started event) {
        if (!(event.getSession() instanceof ArenaMatch match)) return;
        var server = match.getLevel().getServer();
        if (match.setup == null && match.megaTrial == null) throw new IllegalStateException("Unregistered arena challenge");
        // The eastern throw animation needs entity ticks even with a small server simulation distance.
        match.getLevel().getChunkSource().addRegionTicket(COMBAT_TICKET, ARENA_CENTER, COMBAT_TICKET_DISTANCE, match.getId());
        if (match.partyBattle()) {
            int index = 0;
            for (UUID id : match.getMembers().keySet()) {
                var participant = server.getPlayerList().getPlayer(id);
                if (participant == null || !(match.megaTrial != null ? MegaTrials.unlocked(participant) : GymStory.canChallenge(participant, match.leader)))
                    throw new IllegalStateException("Challenge party changed before setup");
                double lane = laneOffset(index++);
                var fighter = new Fighter(participant, WEST.add(0, 0, lane), -90);
                match.team.put(id, fighter);
                if (!fighter.hasReserve()) throw new IllegalStateException("Gym participant has no usable Pokémon");
                match.positionAndPrompt(fighter);
            }
            var actor = match.megaTrial != null ? match.megaTrial.actor() : match.leader.actors().getFirst();
            match.trainers.add(new Trainer(match.getLevel(), actor, 0, match.team.size()));
            match.battleGate = server.getTickCount() + 30;
            // Reaching Started means the world and roster are ready, not merely that Start was clicked.
            var captain = server.getPlayerList().getPlayer(match.getOwner());
            if (match.leader != null && captain != null) ArcQuestPlayerManager.getOrCreate(captain).setFlag(PARTY_TUTORIAL);
            return;
        }
        var owner = server.getPlayerList().getPlayer(match.setup.owner());
        var guest = match.setup.guest() == null ? null : server.getPlayerList().getPlayer(match.setup.guest());
        if (owner == null || match.setup.guest() != null && guest == null) throw new IllegalStateException("Arena participant left before setup");
        if (match.setup.encounter() != null && !GymStory.active(owner, match.setup.encounter())) throw new IllegalStateException("Gym phase changed before setup");
        if (match.setup.story() != null && !match.setup.story().eligible(owner)) throw new IllegalStateException("Story phase changed before setup");
        match.a = new Fighter(owner, WEST, -90);
        match.b = guest == null ? null : new Fighter(guest, EAST, 90);
        if (!match.a.hasReserve() || match.b != null && !match.b.hasReserve()) throw new IllegalStateException("Arena participant has no usable Pokémon");
        if (match.gimmickLesson()) {
            var original = Cobblemon.INSTANCE.getStorage().getParty(owner);
            for (int slot = 0; slot < match.a.slotToRoster.length; slot++) {
                var pokemon = original.get(slot);
                if (pokemon != null && pokemon.getUuid().equals(match.lessonLead())) {
                    match.a.active = match.a.slotToRoster[slot];
                    break;
                }
            }
            if (match.a.current() == null || !match.teraLesson() && !MegaCombat.hasStone(match.a.current()))
                throw new IllegalStateException("Lesson lead changed");
        }
        match.positionAndPrompt(match.a);
        if (match.b != null) match.positionAndPrompt(match.b);
        if (match.setup.hasTrainers()) {
            var actors = match.setup.actors();
            for (int i = 0; i < actors.size(); i++) {
                double lane = actors.size() == 1 ? 0 : i == 0 ? -4 : 4;
                match.trainers.add(new Trainer(match.getLevel(), actors.get(i), lane, 1));
            }
        }
        // Let the dimension and entity tracking initialize before taking over a player's camera.
        match.battleGate = server.getTickCount() + 30;
    }

    private static double laneOffset(int index) {
        // Stagger across the blue pad, then reuse positions for larger parties.
        return switch (index % 5) { case 1 -> -1.15; case 2 -> 1.15; case 3 -> -2.3; case 4 -> 2.3; default -> 0; };
    }

    private void positionAndPrompt(Fighter fighter) {
        var at = fighter.spawn;
        fighter.player.connection.teleport(at.x, at.y, at.z, fighter.yaw, 0);
        PacketDistributor.sendToPlayer(fighter.player, new ArenaPackets.Prepare(fighter.player.level().dimension().location()));
        if (!gimmickLesson()) prepareHint(fighter);
    }

    private static final class Trainer {
        final GymCatalog.Actor actor;
        final Vec3 spawn;
        final NPCEntity npc;
        final List<Pokemon> roster;
        final int healthScale;
        int index, nextSend, pendingSince;
        int megaAt;
        boolean knockedOut, megaUsed;
        PokemonEntity body;
        CompletableFuture<PokemonEntity> sending;
        Pokemon recalling;

        Trainer(ServerLevel level, GymCatalog.Actor actor, double lane, int healthScale) {
            this.actor = actor;
            this.healthScale = healthScale;
            spawn = GYM_EAST_LEAD.add(0, 0, lane);
            roster = actor.team().get();
            if (roster.isEmpty()) throw new IllegalStateException("Missing challenge team for " + actor.id());
            npc = GymStory.newTrainer(level, actor, EAST.add(0, 0, lane), 90);
            npc.setNoAi(true);
            var party = new NPCPartyStore(npc);
            npc.setParty(party);
            for (Pokemon pokemon : roster) {
                UncatchableProperty.INSTANCE.uncatchable().apply(pokemon);
                if (!party.add(pokemon)) throw new IllegalStateException("Gym team exceeds party capacity: " + actor.id());
            }
            if (!level.addFreshEntity(npc)) throw new IllegalStateException("Could not create trainer " + actor.id());
        }

        boolean out() { return index >= roster.size(); }

        void tick(ServerLevel level, int now, Brawl.Difficulty difficulty) {
            if (body != null && (knockedOut || !body.isAlive() || body.getPokemon().isFainted())) {
                recallActive(now);
                index++;
                knockedOut = false;
            }
            if (recalling != null && now >= nextSend) {
                // The recall animation is over. Also handles a body that disappeared during the KO.
                recalling.recall();
                recalling = null;
            }
            if (body != null) {
                if (megaAt != 0 && now >= megaAt) {
                    megaAt = 0;
                    MegaCombat.evolve(body);
                    body.setNoAi(false);
                    Brawl.train(body, difficulty);
                } else if (megaAt == 0 && body.getHealth() <= body.getMaxHealth() / 2) MegaCombat.heal(body);
                return;
            }
            if (out() || now < nextSend) return;
            if (sending == null) {
                npc.setInvisible(false);
                pendingSince = now;
                sending = roster.get(index).sendOutWithAnimation(npc, level, spawn, null, true, null, e -> {
                    e.setYRot(90);
                    e.setYBodyRot(90);
                    e.setYHeadRot(90);
                    e.getEntityData().set(PokemonEntity.getUNBATTLEABLE(), true);
                    return Unit.INSTANCE;
                });
            } else if (sending.isDone()) {
                body = sending.join();
                sending = null;
                if (body == null || body.isRemoved()) throw new IllegalStateException("Failed to send out " + actor.id());
                if (healthScale > 1) {
                    var attribute = body.getAttribute(Attributes.MAX_HEALTH);
                    if (attribute != null) {
                        attribute.addTransientModifier(new AttributeModifier(PrologueWorld.id("gym_team_health"),
                                healthScale - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                        body.setHealth(body.getMaxHealth());
                    }
                }
                if (!megaUsed && MegaCombat.canMega(body.getPokemon())) {
                    megaUsed = true;
                    megaAt = now + 60;
                    body.setNoAi(true);
                    MegaCombat.begin(level, body);
                } else Brawl.train(body, difficulty);
                npc.setInvisible(true);
            } else if (now - pendingSince > 140) {
                throw new IllegalStateException("Send-out timed out for " + actor.id());
            }
        }

        void recallActive(int now) {
            if (body == null) return;
            PokemonEntity oldBody = body;
            megaAt = 0;
            body = null; // Disarm targeting and permit Cobblemon's native recall event.
            oldBody.setNoAi(true);
            oldBody.getNavigation().stop();
            npc.setInvisible(false);
            recalling = roster.get(index);
            if (!oldBody.isRemoved()) oldBody.recallWithAnimation();
            nextSend = now + RECALL_TICKS;
        }

        void result(boolean playerWon) {
            if (body != null) body.setNoAi(true);
            npc.setInvisible(false);
            npc.playAnimation(playerWon ? NPCEntity.LOSE_ANIMATION : NPCEntity.WIN_ANIMATION, List.of());
        }

        void clean() {
            for (Pokemon pokemon : roster) pokemon.recall();
            npc.discard();
            if (sending != null) sending.thenAccept(e -> { if (e != null) e.getPokemon().recall(); });
        }
    }

    private static final class Fighter {
        final ServerPlayer player;
        final PartyStore detached = new PartyStore(UUID.randomUUID());
        final List<Pokemon> roster = new ArrayList<>();
        final int[] slotToRoster = {-1, -1, -1, -1, -1, -1};
        final Vec3 spawn;
        final float yaw;
        int active = -1, round, unsent, nextHint;
        long megaReady;
        boolean controlled, choosing, startRequested, megaUsed;
        Pokemon teraSlot;
        PokemonEntity body;

        Fighter(ServerPlayer player, Vec3 spawn, float yaw) {
            this.player = player;
            this.spawn = spawn;
            this.yaw = yaw;
            var original = Cobblemon.INSTANCE.getStorage().getParty(player);
            for (int slot = 0; slot < slotToRoster.length; slot++) {
                var source = original.get(slot);
                if (source == null || source.isFainted() || source.getTetheringId() != null) continue;
                if (source.getEntity() != null) source.recall();
                var copy = source.clone(true, player.registryAccess());
                UncatchableProperty.INSTANCE.uncatchable().apply(copy);
                detached.add(copy);
                slotToRoster[slot] = roster.size();
                roster.add(copy);
            }
        }

        boolean hasReserve() { return roster.stream().anyMatch(p -> !p.isFainted()); }
        Pokemon current() { return active < 0 ? null : roster.get(active); }
    }

    @Override protected void onTick(InstanceManager manager) {
        try {
            if (partyBattle()) { tickLeader(manager); return; }
            if (a == null) return;
            if (gone(a) || b != null && gone(b)) {
                manager.end(this, ExitReason.DISCONNECTED, true);
                return;
            }
            int now = manager.getServer().getTickCount();
            if (waitingInBlue(a)) {
                keepInBlue(a.player);
                if (now >= a.nextHint) prepareHint(a);
            }
            if (now < battleGate) return;
            if (gimmickLesson() && !tickGimmickLesson(now)) return;
            if (!fighting) {
                tryEnter(a);
                if (b != null) tryEnter(b);
                if (!Smash.active(a.player) || b != null && !Smash.active(b.player)) return;
                fighting = true;
            }
            if (trainerResultAt != 0) {
                if (now >= trainerResultAt) {
                    trainers.forEach(t -> t.result(trainerWon));
                    trainerResolved = true;
                    manager.end(this, trainerWon ? ExitReason.VICTORY : ExitReason.DEFEAT, false);
                }
                return;
            }
            tickHeld(a);
            if (b != null) tickHeld(b);
            check(a);
            if (b != null) {
                check(b);
                boolean outA = !a.hasReserve(), outB = !b.hasReserve();
                if (outA || outB) {
                    announce(outA == outB ? null : outA ? b : a);
                    manager.end(this, ExitReason.VICTORY, false);
                    return;
                }
                sendIn(b);
            } else {
                if (hasTrainers() && (setup.story() == null || setup.story().canSkip()) && RumbleConfig.skipNpcBattles(manager.getServer())) {
                    resolveTrainers(now, true, List.of(a));
                    return;
                }
                boolean playerOut = !a.hasReserve();
                if (!playerOut) for (var trainer : trainers) trainer.tick(getLevel(), now, Brawl.Difficulty.NORMAL);
                boolean gymOut = trainers.stream().allMatch(Trainer::out);
                if (playerOut || gymOut) {
                    // Resolve simultaneous knockouts against the player, and finish recalls before the result pose.
                    resolveTrainers(now, gymOut && !playerOut, List.of(a));
                    return;
                }
            }
            sendIn(a);
        } catch (RuntimeException e) {
            LOG.error("Battle arena {} failed", getId(), e);
            manager.end(this, ExitReason.ERROR, true);
        }
    }

    private boolean tickGimmickLesson(int now) {
        if (fighting) return true;
        if (!Smash.active(a.player)) {
            sendIn(a);
            if (!Smash.active(a.player)) return false;
            ProloguePackets.show(a.player, teraLesson() ? Cue.TERA_ITEM : Cue.MEGA_ITEM, a.body.getId(), true, 0);
        }
        tickHeld(a);
        if (lessonBattleAt == 0 || now < lessonBattleAt) return false;
        fighting = true;
        ProloguePackets.show(a.player, teraLesson() ? Cue.TERA_BATTLE : Cue.MEGA_BATTLE, a.body.getId(), false, 0);
        return true;
    }

    private void tickLeader(InstanceManager manager) {
        int now = manager.getServer().getTickCount();
        if (team.isEmpty()) { manager.end(this, ExitReason.EMPTY, true); return; }
        if (trainerResultAt != 0) {
            if (now >= trainerResultAt) {
                trainers.forEach(t -> t.result(trainerWon));
                trainerResolved = true;
                if (!trainerWon) manager.end(this, ExitReason.DEFEAT, false);
                else if (!manager.win(getLevel(), GYM_WIN)) manager.end(this, ExitReason.ERROR, false);
            }
            return;
        }
        for (var f : team.values()) {
            if (gone(f)) continue;
            if (waitingInBlue(f)) {
                keepInBlue(f.player);
                if (now >= f.nextHint) prepareHint(f);
            }
            if (!fighting && now >= battleGate && !Smash.active(f.player)) tryEnter(f);
        }
        if (!fighting) {
            if (now < battleGate || team.values().stream().anyMatch(f -> gone(f) || !Smash.active(f.player))) return;
            fighting = true;
        }
        var standing = team.values().stream().filter(f -> !gone(f)).toList();
        if (RumbleConfig.skipNpcBattles(manager.getServer())) {
            resolveTrainers(now, true, standing);
            return;
        }
        standing.forEach(this::tickHeld);
        standing.forEach(this::check);
        boolean teamOut = standing.stream().noneMatch(Fighter::hasReserve);
        if (!teamOut) trainers.forEach(trainer -> trainer.tick(getLevel(), now, Brawl.Difficulty.HARD));
        boolean gymOut = trainers.stream().allMatch(Trainer::out);
        if (teamOut || gymOut) {
            resolveTrainers(now, gymOut && !teamOut, standing);
            return;
        }
        standing.forEach(this::sendIn);
    }

    private void resolveTrainers(int now, boolean playerWon, List<Fighter> fighters) {
        trainerWon = playerWon;
        fighters.forEach(this::closeRoster);
        trainers.forEach(trainer -> trainer.recallActive(now));
        trainerResultAt = Math.max(now + 1, trainers.stream().mapToInt(trainer -> trainer.nextSend).max().orElse(now));
    }

    private boolean gone(Fighter f) {
        return !f.player.isAlive() || f.player.hasDisconnected() || f.player.serverLevel() != getLevel();
    }

    private void check(Fighter f) {
        if (f.current() != null && f.current().isFainted()) {
            if (Smash.active(f.player)) Smash.end(f.player, null, false);
            if (f.body != null) f.body.discard();
            f.current().recall();
            f.body = null;
            f.megaReady = 0;
            f.active = -1;
            if (f.hasReserve()) {
                f.round++;
                f.choosing = true;
                showRoster(f);
                f.player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 0.83F);
            }
        }
    }

    private void sendIn(Fighter f) {
        if (f.current() == null || Smash.active(f.player)) return;
        if (f.body == null || !f.body.isAlive()) f.body = deploy(f, hasTrainers() ? leadPosition(f) : f.spawn);
        if (f.body != null && Smash.start(f.player, f.body) == null) {
            f.unsent = 0;
            f.controlled = true;
            closeRoster(f);
        } else if (++f.unsent > 100) {
            throw new IllegalStateException("Could not send in arena Pokémon for " + f.player.getUUID());
        }
    }

    private PokemonEntity deploy(Fighter f, Vec3 at) {
        return f.current().sendOut(getLevel(), at, null, e -> {
            e.moveTo(at.x, at.y, at.z, f.yaw, 0);
            e.setPersistenceRequired();
            e.getEntityData().set(PokemonEntity.getUNBATTLEABLE(), true);
            return Unit.INSTANCE;
        });
    }

    private boolean waitingInBlue(Fighter f) {
        return hasTrainers() && !gimmickLesson() && !fighting && !f.controlled;
    }

    private static Vec3 leadPosition(Fighter f) { return GYM_WEST_LEAD.add(0, 0, f.spawn.z - WEST.z); }

    private static boolean inBlue(ServerPlayer player) {
        double margin = player.getBbWidth() * .5;
        return player.getX() >= BLUE_MIN_X + margin && player.getX() <= BLUE_MAX_X - margin
                && player.getZ() >= BLUE_MIN_Z + margin && player.getZ() <= BLUE_MAX_Z - margin;
    }

    private static void keepInBlue(ServerPlayer player) {
        if (inBlue(player)) return;
        double margin = player.getBbWidth() * .5 + .025;
        player.connection.teleport(
                Mth.clamp(player.getX(), BLUE_MIN_X + margin, BLUE_MAX_X - margin), player.getY(),
                Mth.clamp(player.getZ(), BLUE_MIN_Z + margin, BLUE_MAX_Z - margin),
                player.getYRot(), player.getXRot());
    }

    private void prepareHint(Fighter f) {
        var send = Component.keybind("key.cobblemon.throwpartypokemon");
        var enter = Component.keybind("key.realtimecobblemon.smash");
        Component hint = !hasTrainers()
                ? Component.translatable("rumble.arena.prepare", send, enter)
                : f.body == null || !f.body.isAlive()
                        ? Component.translatable("rumble.arena.prepare_blue", send)
                        : Component.translatable("rumble.arena.prepare_enter", enter);
        f.player.displayClientMessage(hint, true);
        f.nextHint = getLevel().getServer().getTickCount() + TEAM_HINT_INTERVAL;
    }

    /** Handles the usual Cobblemon send/recall key without touching the real party. */
    public static boolean partySend(ServerPlayer player, int slot) {
        var match = inArena(player);
        if (match == null) return false;
        var f = match.fighter(player);
        if (f == null || match.fighting || match.gimmickLesson() || Smash.active(player) || player.serverLevel() != match.getLevel()
                || match.getState() != State.ACTIVE || slot < 0 || slot >= f.slotToRoster.length) return true;
        if (match.waitingInBlue(f) && !inBlue(player)) {
            keepInBlue(player);
            match.prepareHint(f);
            return true;
        }
        int index = f.slotToRoster[slot];
        if (index < 0 || f.roster.get(index).isFainted()) return true;
        boolean recallOnly = f.active == index && f.body != null && f.body.isAlive();
        if (f.current() != null) f.current().recall();
        f.body = null;
        f.active = recallOnly ? -1 : index;
        f.startRequested = false;
        if (!recallOnly) {
            var level = match.getLevel();
            var at = f.spawn.add(f.yaw < 0 ? 4 : -4, 0, 0);
            // Use Cobblemon's cast/beam. The clone is detached, so it needs the throw sound and hand swing here.
            player.swing(InteractionHand.MAIN_HAND, true);
            level.playSound(null, player.blockPosition(), CobblemonSounds.POKE_BALL_THROW, SoundSource.PLAYERS, .6F, 1F);
            f.current().sendOutWithAnimation(player, level, at, null, true, null, e -> {
                e.setYRot(f.yaw);
                e.setYBodyRot(f.yaw);
                e.setYHeadRot(f.yaw);
                e.getEntityData().set(PokemonEntity.getUNBATTLEABLE(), true);
                return Unit.INSTANCE;
            });
            f.body = f.current().getEntity();
        }
        if (match.waitingInBlue(f)) match.prepareHint(f);
        return true;
    }

    public static boolean toggle(ServerPlayer player) {
        var match = inArena(player);
        if (match == null) return false;
        var f = match.fighter(player);
        if (match.fighting || match.gimmickLesson()) {
            player.displayClientMessage(Component.translatable("rumble.arena.mode_locked"), true);
        } else if (f != null && !Smash.active(player) && player.serverLevel() == match.getLevel()
                && match.getState() == State.ACTIVE) {
            if (f.body == null || !f.body.isAlive()) match.prepareHint(f);
            else f.startRequested = true;
        }
        return true;
    }

    private void tryEnter(Fighter f) {
        if (!f.startRequested || f.body == null || !f.body.isAlive() || f.body.getBeamMode() != 0) return;
        f.startRequested = false;
        var bodyStart = f.body.position();
        boolean gymBattle = hasTrainers();
        if (gymBattle) {
            var at = leadPosition(f);
            f.body.teleportTo(at.x, at.y, at.z);
            f.body.setDeltaMovement(Vec3.ZERO);
        }
        var error = Smash.start(f.player, f.body);
        if (error != null) {
            if (gymBattle) f.body.teleportTo(bodyStart.x, bodyStart.y, bodyStart.z);
            f.player.displayClientMessage(error, true);
        } else {
            if (gymBattle) f.player.displayClientMessage(Component.empty(), true);
            f.controlled = true;
            if (b != null) f.player.displayClientMessage(Component.translatable("rumble.arena.wait_opponent"), true);
        }
    }

    private void showRoster(Fighter f) {
        var choices = java.util.stream.IntStream.range(0, f.roster.size()).mapToObj(i -> {
            Pokemon mon = f.roster.get(i);
            return new ArenaPackets.Option(i, mon);
        }).toList();
        PacketDistributor.sendToPlayer(f.player, new ArenaPackets.Switch(getId(), f.round, choices));
    }

    static void pick(ServerPlayer player, ArenaPackets.Pick packet) {
        ArenaMatch match = inArena(player);
        if (match == null || match.getState() != State.ACTIVE || match.getLevel() != player.serverLevel()
                || !match.getId().equals(packet.match())) return;
        Fighter f = match.fighter(player);
        if (f == null || !f.choosing || match.trainerResultAt != 0 || f.round != packet.round()) return;
        if (packet.index() < 0 || packet.index() >= f.roster.size() || f.roster.get(packet.index()).isFainted()) {
            match.showRoster(f);
            return;
        }
        f.active = packet.index();
        f.choosing = false;
    }

    private void closeRoster(Fighter f) {
        f.choosing = false;
        if (!f.player.hasDisconnected()) PacketDistributor.sendToPlayer(f.player, new ArenaPackets.Switch(getId(), f.round, List.of()));
    }

    private void announce(Fighter winner) {
        for (var f : List.of(a, b)) f.player.sendSystemMessage(Component.translatable(winner == null ? "rumble.arena.draw"
                : f == winner ? "rumble.arena.win" : "rumble.arena.lose"));
    }

    @SubscribeEvent
    public static void ending(InstanceEvent.Ending event) {
        if (event.getSession() instanceof ArenaMatch match) {
            match.clean(match.a);
            match.clean(match.b);
            match.team.values().forEach(match::clean);
            // Keep the NPCs alive in the result window so their native win/lose animation can finish.
            for (var trainer : match.trainers) for (Pokemon pokemon : trainer.roster) pokemon.recall();
            if (match.getLevel() != null) match.getLevel().getChunkSource()
                    .removeRegionTicket(COMBAT_TICKET, ARENA_CENTER, COMBAT_TICKET_DISTANCE, match.getId());
        }
    }

    @SubscribeEvent
    public static void destroyed(InstanceEvent.Destroyed event) {
        if (event.getSession() instanceof ArenaMatch match) {
            match.trainers.forEach(Trainer::clean);
            if (match.setup != null && match.setup.story() != null) match.setup.story().closed();
        }
    }

    @SubscribeEvent
    public static void returned(InstanceEvent.Left event) {
        if (!(event.getSession() instanceof ArenaMatch match)) return;
        if (match.partyBattle()) {
            var fighter = match.team.remove(event.getPlayer().getUUID());
            if (fighter == null) return;
            if (match.leader != null && match.trainerResolved && (event.getReason() == ExitReason.DEFEAT
                    || event.getReason() == ExitReason.VICTORY && match.getCompletedPlayers().contains(event.getPlayer().getUUID())))
                GymStory.battleResult(event.getPlayer(), match.leader, match.setup.speaker(), event.getReason() == ExitReason.VICTORY);
            else if (match.getState() == State.ACTIVE) match.clean(fighter);
        } else if (match.trainerResolved && (event.getReason() == ExitReason.VICTORY || event.getReason() == ExitReason.DEFEAT)
                && match.a.player == event.getPlayer()) {
            match.trainerResolved = false;
            if (match.setup.story() != null) match.setup.story().result(event.getPlayer(), match.trainerWon);
            else GymStory.battleResult(event.getPlayer(), match.setup.encounter(), match.setup.speaker(), match.trainerWon);
        }
    }

    @SubscribeEvent
    public static void rejectExternalVictory(InstanceEvent.Victory event) {
        if (event.getSession() instanceof ArenaMatch match
                && !(match.partyBattle() && match.trainerResolved && match.trainerWon && GYM_WIN.equals(event.getTrigger()))) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        INVITES.values().removeIf(i -> i.challenger().equals(player.getUUID()) || i.recipient().equals(player.getUUID()));
        LEADER_LOBBIES.remove(player.getUUID());
        ArenaMatch match = inArena(player);
        if (match != null && !match.partyBattle() && match.getState() != State.ENDING)
            InstanceManager.get(player.server).end(match, ExitReason.DISCONNECTED, true);
    }

    @SubscribeEvent
    public static void expire(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var id = player.getUUID();
        int now = player.server.getTickCount();
        INVITES.computeIfPresent(id, (key, invite) -> invite.expires() <= now ? null : invite);
        LEADER_LOBBIES.computeIfPresent(id, (key, lobby) -> lobby.expires() <= now ? null : lobby);
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { INVITES.clear(); LEADER_LOBBIES.clear(); }

    private void clean(Fighter f) {
        if (f == null) return;
        if (gimmickLesson() && !f.player.hasDisconnected()) ProloguePackets.show(f.player, Cue.NONE, -1, false, 0);
        if (!f.player.hasDisconnected() && f.teraSlot != null) PacketDistributor.sendToPlayer(f.player, new ArenaPackets.TeraUsed(false));
        if (Smash.active(f.player)) Smash.end(f.player, null, false);
        Conditions.forget(f.player);
        if (!f.player.hasDisconnected() && f.controlled) Smash.send(f.player, new SmashPayloads.State(f.player.getId(), -1, false));
        closeRoster(f);
        for (Pokemon mon : f.roster) {
            if (mon.getEntity() != null) mon.getEntity().discard();
            mon.recall();
            f.detached.remove(mon);
        }
    }

    private Fighter fighter(ServerPlayer p) {
        if (partyBattle()) {
            var f = team.get(p.getUUID());
            return f != null && f.player == p ? f : null;
        }
        return a != null && a.player == p ? a : b != null && b.player == p ? b : null;
    }

    public static ArenaMatch inArena(ServerPlayer p) { return InstanceApi.find(p).orElse(null) instanceof ArenaMatch match ? match : null; }
    public static boolean inGimmickLesson(ServerPlayer player) {
        var match = inArena(player);
        return match != null && match.gimmickLesson();
    }
    public static boolean isArena(ServerLevel level) { return InstanceApi.find(level).orElse(null) instanceof ArenaMatch; }
    public static ArenaMatch group(Entity e) {
        if (e == null || !(e.level() instanceof ServerLevel level)
                || !(InstanceApi.find(level).orElse(null) instanceof ArenaMatch match) || match.getState() != State.ACTIVE) return null;
        if (e instanceof ServerPlayer p) return match.fighter(p) == null ? null : match;
        if (e instanceof PokemonEntity) {
            if (match.trainerOf(e) != null) return match;
            for (var s : Smash.sessions()) if ((s.pokemon == e || s.combat.decoys.contains(e)) && match.fighter(s.player) != null) return match;
        }
        return null;
    }

    private Trainer trainerOf(Entity e) {
        for (var trainer : trainers) if (trainer.body == e && !trainer.knockedOut) return trainer;
        return null;
    }

    public static boolean isTrainerFighter(PokemonEntity entity) {
        return group(entity) != null && entity.getPokemon().isNPCOwned();
    }

    public static List<Pokemon> roster(ServerPlayer p) {
        ArenaMatch match = group(p);
        Fighter f = match == null ? null : match.fighter(p);
        return f == null ? null : f.roster;
    }

    public static boolean owns(ServerPlayer p, Pokemon mon) {
        ArenaMatch match = group(p);
        return match != null && match.fighter(p).current() == mon;
    }

    static void useHeld(ServerPlayer player) {
        var match = group(player);
        var session = Smash.session(player);
        if (session == null || session.pokemon.getPokemon().isFainted()) return;
        if (match == null) {
            if (!com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining.restrict(player) && !MegaCombat.heal(session.pokemon))
                com.johnn.realtimecobblemon.MegaEvo.onKey(player);
            return;
        }
        var f = match.fighter(player);
        if (f == null || f.choosing || f.body != session.pokemon || match.trainerResultAt != 0
                || !match.fighting && (!match.gimmickLesson() || match.teraLesson()) || f.megaReady != 0 || !f.body.isAlive()) return;
        if (MegaCombat.heal(f.body)) { Smash.sendHud(session); return; }
        if (f.megaUsed || !MegaCombat.equipped(player) || !MegaCombat.canMega(f.current())) return;
        long now = match.getLevel().getGameTime();
        if (session.combat.lockedUntil > now) return;
        f.megaReady = now + 60;
        session.combat.lockedUntil = f.megaReady;
        session.combat.lockReason = "is Mega Evolving!";
        MegaCombat.begin(match.getLevel(), f.body);
    }

    void useTera(Smash.Session session) {
        var f = fighter(session.player);
        if (group(session.player) != this || f == null || f.choosing || f.teraSlot != null || f.body != session.pokemon
                || trainerResultAt != 0 || !fighting && !teraLesson() || !TeraCombat.apply(session, true)) return;
        f.teraSlot = f.current();
        PacketDistributor.sendToPlayer(f.player, new ArenaPackets.TeraUsed(true));
        if (teraLesson() && !fighting) {
            lessonBattleAt = getLevel().getServer().getTickCount() + 26;
            ProloguePackets.show(f.player, Cue.TERA_ITEM, f.body.getId(), false, 0, true);
        }
    }

    private void tickHeld(Fighter f) {
        if (f.megaReady == 0 || getLevel().getGameTime() < f.megaReady) return;
        f.megaReady = 0;
        var session = Smash.session(f.player);
        if (session == null || session.pokemon != f.body || f.current().isFainted() || !MegaCombat.evolve(f.body)) return;
        f.megaUsed = true;
        Smash.refreshBody(session);
        Smash.sendHud(session);
        if (gimmickLesson() && !teraLesson() && !fighting && lessonBattleAt == 0) {
            lessonBattleAt = getLevel().getServer().getTickCount() + 26;
            ProloguePackets.show(f.player, Cue.MEGA_ITEM, f.body.getId(), false, 0, true);
        }
    }

    public static Boolean canHit(Smash.Session attacker, LivingEntity target) {
        ArenaMatch match = group(attacker.player);
        if (match == null) return null;
        if (!match.fighting || match.trainerResultAt != 0 || target == attacker.player || target == attacker.pokemon || !target.isAlive() || target.isSpectator()
                || attacker.combat.decoys.contains(target) || group(target) != match) return false;
        return target instanceof ServerPlayer p ? match.isFriendlyFireEnabled() && Smash.active(p) : match.trainerOf(target) != null
                || match.isFriendlyFireEnabled() && com.johnn.realtimecobblemon.moves.Decoys.isDecoy(target);
    }

    public static boolean retainClientControl(ServerPlayer player) {
        ArenaMatch match = group(player);
        Fighter f = match == null ? null : match.fighter(player);
        return f != null && f.current() != null && f.current().isFainted() && f.hasReserve();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protectUnsafeDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || !(InstanceApi.find(level).orElse(null) instanceof ArenaMatch arena)) return;
        if (!arena.fighting || arena.trainerResultAt != 0) { event.setCanceled(true); return; }
        var by = group(event.getSource().getEntity());
        if (victim instanceof ServerPlayer p && (!Smash.active(p) || by == null || group(p) != by)) event.setCanceled(true);
        if (victim instanceof PokemonEntity pokemon && pokemon.getPokemon().isNPCOwned()
                && (group(pokemon) == null || by == null && !event.getSource().is(DamageTypes.MAGIC))) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void retainBodyForRecall(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof PokemonEntity pokemon) || !isTrainerFighter(pokemon)) return;
        if (event.getNewDamage() < pokemon.getHealth()) return;
        var arena = group(pokemon);
        var foe = arena.trainerOf(pokemon);
        foe.knockedOut = true;
        pokemon.setNoAi(true);
        ((ArenaKnockoutVisual) pokemon).rumble$markKnockedOut();
        // Vanilla would run the death animation and remove the body. Preserve it just long enough for recall.
        event.setNewDamage(Math.max(0, pokemon.getHealth() - 0.01F));
    }

    @SubscribeEvent
    public static void preventNormalAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && inArena(p) != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void preventNormalUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer p && inArena(p) != null) event.setCanceled(true);
    }
}
