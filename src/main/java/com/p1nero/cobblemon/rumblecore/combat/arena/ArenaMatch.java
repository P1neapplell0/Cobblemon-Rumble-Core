package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.api.storage.party.NPCPartyStore;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.item.PokeBallItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import com.johnn.realtimecobblemon.MegaEvo;
import com.johnn.realtimecobblemon.Smash;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeKnockout;
import com.johnn.realtimecobblemon.SmashConfig;
import com.johnn.realtimecobblemon.duel.Duels;
import com.johnn.realtimecobblemon.moves.Brawl;
import com.johnn.realtimecobblemon.moves.Conditions;
import com.johnn.realtimecobblemon.moves.Decoys;
import com.johnn.realtimecobblemon.network.SmashPayloads;
import com.mojang.logging.LogUtils;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.RumbleConfig;
import com.p1nero.cobblemon.rumblecore.combat.BattleMusic;
import com.p1nero.cobblemon.rumblecore.combat.reward.WildCapture;
import com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierRules;
import com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierSpectators;
import com.p1nero.cobblemon.rumblecore.gym.CinemaTrials;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog;
import com.p1nero.cobblemon.rumblecore.gym.GymLeaderTeams;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.gym.GymTeamPokemon;
import com.p1nero.cobblemon.rumblecore.gym.LegendaryTrials;
import com.p1nero.cobblemon.rumblecore.gym.MegaTrials;
import com.p1nero.cobblemon.rumblecore.prologue.ChampionStory;
import com.p1nero.cobblemon.rumblecore.prologue.CinemaQuest;
import com.p1nero.cobblemon.rumblecore.prologue.LegendaryCommission;
import com.p1nero.cobblemon.rumblecore.prologue.MegaSimulator;
import com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets.Cue;
import com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.misc.custom_instances.api.ExitReason;
import com.p1nero.misc.custom_instances.api.InstanceApi;
import com.p1nero.misc.custom_instances.api.InstanceEvent;
import com.p1nero.misc.custom_instances.instance.InstanceDefinition;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.instance.InstanceSession;
import com.p1nero.misc.custom_instances.network.InstanceNetwork;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import kotlin.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;
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
    private static FrontierGame creatingFrontier;
    private final FrontierGame frontier;
    private boolean admittedSpectators;
    private static TeamGame creatingTeamGame;
    private final TeamGame teamGame = creatingTeamGame;
    public record TeamEntry(int side, List<Pokemon> roster) {}
    private record TeamGame(Map<UUID, TeamEntry> entries, int minutes, List<UUID> spectators, Consumer<Integer> result) {}
    private final Map<UUID, Integer> respawn = new HashMap<>();
    private final Map<UUID, Integer> teamRecall = new HashMap<>();
    private int teamResultAt, teamWinner;
    private final int[] teamScore = new int[2];
    private int teamDeadline, lastTeamSecond = -1;
    private final Map<UUID, ArenaPackets.Opponents> opponents = new HashMap<>();
    private net.minecraft.server.level.ServerBossEvent teamClock;
    private final Map<UUID, ArenaPackets.Allies> teamAllies = new HashMap<>();
    private int knockoutUntil;
    private int knockoutSlowUntil;
    private final Set<Entity> knockoutActors = new HashSet<>();
    private final Set<Integer> shownKnockouts = new HashSet<>();
    private int knockoutRecallAt;

    private record FrontierGame(List<Pokemon> host, List<Pokemon> guest, List<UUID> spectators, Consumer<UUID> result, Runnable failed) {}
    private final GymCatalog.Encounter leader;
    private final boolean hardLeader;
    private final MegaTrials.Trial megaTrial;
    private final LegendaryTrials.Trial legendary;
    private CinemaBattle cinema;
    private ArenaPackets.Allies lastAllies;
    private final List<PokemonEntity> wildFoes = new ArrayList<>();
    private boolean capturePhase;
    private final Map<UUID, String> departedRosters = new LinkedHashMap<>();
    private final Map<UUID, Fighter> team = new LinkedHashMap<>();
    private Fighter a, b;
    private final List<Trainer> trainers = new ArrayList<>();
    private Integer introAt;
    private int battleGate, trainerResultAt, gymPreviewUntil;
    private final GymReplacements gymReplacements = new GymReplacements();
    private boolean pvpResultAnnounced, frontierCombatStarted;
    private boolean frontierResolved;
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
        default void configureTrainer(NPCEntity npc) { }
        default void closed() { }
        default boolean canSkip() { return true; }
        default boolean usesGymMusic() { return false; }
    }

    private record Setup(UUID owner, UUID guest, GymCatalog.Encounter encounter, UUID speaker, StoryChallenge story) {
        boolean includes(UUID id) { return owner.equals(id) || id.equals(guest); }
        boolean hasTrainers() { return encounter != null || story != null; }
        List<GymCatalog.Actor> actors() { return story == null ? encounter.actors() : story.actors(); }
    }

    private ArenaMatch(UUID id, InstanceDefinition definition, ResourceKey<Level> dimension, UUID owner, String password) {
        this(id, definition, dimension, owner, password, null, null, null);
    }

    private ArenaMatch(UUID id, InstanceDefinition definition, ResourceKey<Level> dimension, UUID owner, String password,
                       GymCatalog.Encounter leader, MegaTrials.Trial megaTrial, LegendaryTrials.Trial legendary) {
        this(id, definition, dimension, owner, password, leader, megaTrial, legendary, false);
    }

    private ArenaMatch(UUID id, InstanceDefinition definition, ResourceKey<Level> dimension, UUID owner, String password,
                       GymCatalog.Encounter leader, MegaTrials.Trial megaTrial, LegendaryTrials.Trial legendary,
                       boolean hardLeader) {
        super(id, definition, dimension, owner, password);
        this.leader = leader;
        this.hardLeader = hardLeader;
        this.megaTrial = megaTrial;
        this.legendary = legendary;
        this.setup = creating != null && creating.owner().equals(owner) ? creating : null;
        this.frontier = this.setup == null ? null : creatingFrontier;
    }

    public static String leaderTemplate(GymCatalog.Gym gym) { return "rumble_gym_" + gym.path(); }
    public static String leaderTemplate(GymCatalog.Gym gym, boolean hard) {
        return leaderTemplate(gym) + (hard ? "_hard" : "");
    }

    private static final GymCatalog.Actor CHAMPION = new GymCatalog.Actor("champion", BlockPos.ZERO, GymTeamPokemon::champion);

    private boolean champion() { return getDefinition().id().equals(ChampionStory.TEMPLATE); }

    public static void initialize() {
        InstanceApi.registerSessionFactory(ChampionStory.TEMPLATE, ArenaMatch::new);
        InstanceApi.registerLobbyPresentation(ChampionStory.TEMPLATE, player -> new InstanceApi.LobbyPresentation(
                CHAMPION.key(), PrologueWorld.id("textures/entity/npc/prof_p1nero.png"), false));
        InstanceApi.registerSessionFactory(TEMPLATE, ArenaMatch::new);
        for (ArenaTheme theme : ArenaTheme.values()) {
            if (theme != ArenaTheme.DEFAULT) InstanceApi.registerSessionFactory(theme.template(), ArenaMatch::new);
        }
        for (var gym : GymCatalog.Gym.values()) {
            for (boolean hard : new boolean[]{false, true}) {
                String template = leaderTemplate(gym, hard);
                var leader = GymCatalog.leader(gym);
                InstanceApi.registerSessionFactory(template, (id, definition, dimension, owner, password) ->
                        new ArenaMatch(id, definition, dimension, owner, password, leader, null, null, hard));
                var actor = leader.actors().getFirst();
                var portrait = GymCatalog.leaderTexture(gym);
                InstanceApi.registerLobbyPresentation(template, player -> new InstanceApi.LobbyPresentation(
                        actor.key(), portrait, !ArcQuestPlayerManager.getOrCreate(player).hasFlag(PARTY_TUTORIAL)));
            }
        }
    }
    public static void registerMegaTrial(MegaTrials.Trial trial) {
        InstanceApi.registerSessionFactory(trial.id(), (id, definition, dimension, owner, password) ->
                new ArenaMatch(id, definition, dimension, owner, password, null, trial, null));
    }

    public static void registerLegendaryTrial(LegendaryTrials.Trial trial) {
        InstanceApi.registerSessionFactory(trial.template(), (id, definition, dimension, owner, password) ->
                new ArenaMatch(id, definition, dimension, owner, password, null, null, trial));
    }

    public static void registerCinemaTrial(CinemaTrials.Trial trial) {
        InstanceApi.registerSessionFactory(trial.template(), (id, definition, dimension, owner, password) -> {
            var match = new ArenaMatch(id, definition, dimension, owner, password);
            match.cinema = new CinemaBattle(match, trial);
            return match;
        });
    }

    private boolean partyBattle() { return teamGame != null || leader != null || megaTrial != null || legendary != null || cinema != null || champion(); }
    /** Used by the cinematic bridge to skip presentation-only cutscenes in team matches. */
    boolean isTeamGame() { return teamGame != null; }
    private boolean hasTrainers() { return champion() || leader != null || megaTrial != null || setup != null && setup.hasTrainers(); }
    private UUID lessonLead() {
        if (setup == null || setup.story() == null) return null;
        var tera = setup.story().teraLessonLead();
        return tera != null ? tera : setup.story().megaLessonLead();
    }

    private boolean gimmickLesson() { return lessonLead() != null; }
    private boolean teraLesson() { return setup != null && setup.story() != null && setup.story().teraLessonLead() != null; }

    @Override public boolean isFriendlyFireEnabled() { return teamGame == null && leader == null && legendary == null && cinema == null && (!partyBattle() || super.isFriendlyFireEnabled()); }
    @Override public int getMaxPlayers() {
        if (teamGame != null) return Integer.MAX_VALUE;
        if (leader != null) return Math.min(GYM_SLOTS, super.getMaxPlayers());
        return cinema == null ? super.getMaxPlayers() : Math.min(super.getMaxPlayers(), cinema.trial.capacity());
    }
    public static final int GYM_SLOTS = 6;

    public static boolean gymTemplate(String id) { return id.startsWith("rumble_gym_"); }

    public boolean isGymObserver(ServerPlayer player) {
        var fighter = team.get(player.getUUID());
        return leader != null && fighter != null && fighter.eliminated && player.serverLevel() == getLevel();
    }

    public boolean gymPreviewActive() { return gymPreviewUntil != 0; }

    @Override public Map<UUID, String> snapshotPlayerDetails() {
        var details = new LinkedHashMap<>(departedRosters);
        team.forEach((id, fighter) -> details.put(id, fighter.snapshot()));
        if (a != null) details.put(a.player.getUUID(), a.snapshot());
        if (b != null) details.put(b.player.getUUID(), b.snapshot());
        return details;
    }

    @Override public boolean showDefaultResult() { return false; }
    @Override public boolean showDefaultBattleReport() { return false; }
    @Override public boolean showStatusHud() { return partyBattle() && (getState() == State.WAITING || getState() == State.PREPARING); }
    @Override public int resultDelayTicks() { return gymPreviewActive() ? 0 : 75; }

    public boolean musicActive(ServerPlayer player) {
        return getState() == State.ACTIVE && getLevel() == player.serverLevel()
                && (introAt != null || fighting) && !capturePhase && trainerResultAt == 0 && fighter(player) != null;
    }

    public boolean usesGymMusic() {
        return gymEncounter() || champion() || (setup != null && setup.story() != null && setup.story().usesGymMusic());
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
        InstanceNetwork.openLobby(player, leaderTemplate(encounter.gym(), RumbleConfig.hardGymBattles()));
    }

    public static void challengeChampion(ServerPlayer player) {
        if (!available(player) || !ChampionStory.eligible(player) || !SmashConfig.<Boolean>get(SmashConfig.ENABLED)) return;
        if (healthy(player).isEmpty()) {
            player.displayClientMessage(Component.translatable("rumble.gym.no_party"), true);
            return;
        }
        start(player, null, null, null, null, ChampionStory.TEMPLATE);
    }

    public static boolean challengeStory(ServerPlayer player, StoryChallenge story) {
        if (!available(player) || !story.eligible(player) || !SmashConfig.<Boolean>get(SmashConfig.ENABLED)) return false;
        if (healthy(player).isEmpty()) {
            player.displayClientMessage(Component.translatable("rumble.gym.no_party"), true);
            return false;
        }
        return start(player, null, null, null, story);
    }

    public static boolean startFrontier(ServerPlayer owner, ServerPlayer guest, Supplier<List<Pokemon>> host,
                                        Supplier<List<Pokemon>> challenger, List<UUID> spectators, Consumer<UUID> result,
                                        Runnable failed) {
        if (owner == guest || !available(owner) || !available(guest) || !owner.server.isPvpAllowed()) return false;
        try {
            creatingFrontier = new FrontierGame(host.get(), challenger.get(), List.copyOf(spectators), result, failed);
            return start(owner, guest, null, null, null);
        } catch (RuntimeException e) {
            LOG.error("Could not prepare Battle Frontier rosters", e);
            return false;
        } finally {
            creatingFrontier = null;
        }
    }

    public static boolean startFrontierTeams(ServerPlayer host, Map<UUID, TeamEntry> entries, int minutes, List<UUID> spectators, Consumer<Integer> result) {
        if (!host.server.isPvpAllowed() || !entries.containsKey(host.getUUID())
                || entries.values().stream().anyMatch(e -> e.roster().size() != 1 || e.side() < 0 || e.side() > 1)
                || entries.values().stream().map(TeamEntry::side).distinct().count() != 2
                || !FrontierRules.isValidTeamDuration(minutes)) return false;
        var players = entries.keySet().stream().map(host.server.getPlayerList()::getPlayer).toList();
        if (players.stream().anyMatch(p -> p == null || !available(p))) return false;
        var manager = InstanceManager.get(host.server);
        String password = String.format(Locale.ROOT, "%04d", ThreadLocalRandom.current().nextInt(10000));
        try {
            UUID room;
            creatingTeamGame = new TeamGame(Collections.unmodifiableMap(new LinkedHashMap<>(entries)), minutes, List.copyOf(spectators), result);
            try { room = manager.create(host, TEMPLATE, password); }
            finally { creatingTeamGame = null; }
            for (var player : players) if (player != host) manager.join(player, TEMPLATE, room, password);
            // Joining clears prior readiness in Custom Instances. Set it only once everyone has joined.
            for (var player : players) manager.setReady(player, room, true);
            manager.start(host, TEMPLATE);
            return true;
        } catch (IOException | RuntimeException e) {
            if (InstanceApi.find(host).orElse(null) instanceof ArenaMatch match) manager.end(match, ExitReason.ERROR, true);
            LOG.warn("Could not start Battle Frontier team arena", e);
            InstanceManager.error(host, e);
            return false;
        }
    }

    private static boolean start(ServerPlayer owner, ServerPlayer guest, GymCatalog.Encounter encounter, UUID speaker, StoryChallenge story) {
        var theme = encounter != null ? ArenaTheme.gym(encounter.gym().path())
                : story != null ? story.arena(owner) : guest != null
                        ? ArenaTheme.biome(owner.serverLevel(), owner.blockPosition()) : ArenaTheme.DEFAULT;
        if (encounter != null && encounter.leader() && RumbleConfig.hardGymBattles())
            return start(owner, guest, encounter, speaker, story, leaderTemplate(encounter.gym(), true));
        return start(owner, guest, encounter, speaker, story, theme.available().template());
    }

    private static boolean start(ServerPlayer owner, ServerPlayer guest, GymCatalog.Encounter encounter, UUID speaker,
                                 StoryChallenge story, String template) {
        var manager = InstanceManager.get(owner.server);
        String password = String.format(Locale.ROOT, "%04d", ThreadLocalRandom.current().nextInt(10000));
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
        if (match.cinema != null) {
            if (!CinemaQuest.unlocked(player) || !MegaSimulator.atTerminal(player) || !match.cinema.trial.supported()
                    || !SmashConfig.<Boolean>get(SmashConfig.ENABLED) || !available(player)) event.setCanceled(true);
            return;
        }
        if (match.legendary != null) {
            if (!LegendaryCommission.unlocked(player) || !player.level().dimension().equals(PrologueWorld.GYMS)
                    || !match.legendary.supported() || !SmashConfig.<Boolean>get(SmashConfig.ENABLED)
                    || !available(player) || healthy(player).isEmpty()) event.setCanceled(true);
            return;
        }
        if (match.megaTrial != null) {
            if (MegaTrials.get(match.megaTrial.id()) != match.megaTrial || !MegaTrials.unlocked(player)
                    || !MegaSimulator.atTerminal(player) || !match.megaTrial.supported()
                    || !SmashConfig.<Boolean>get(SmashConfig.ENABLED) || !available(player) || healthy(player).isEmpty()) event.setCanceled(true);
            return;
        }
        if (match.champion()) {
            if (match.setup == null || !match.setup.owner().equals(player.getUUID()) || !ChampionStory.eligible(player))
                event.setCanceled(true);
            return;
        }
        if (match.teamGame != null) {
            if (!match.teamGame.entries().containsKey(player.getUUID()) || !available(player)) event.setCanceled(true);
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

    public void playIntro(MinecraftServer server) {
        if (introAt != null) return;
        boolean pvp = setup != null && setup.guest() != null;
        if (cinema != null) {
            cinema.playIntro(server);
            introAt = server.getTickCount();
            startIntroMusic(server);
            return;
        }
        var opponents = leader != null ? leader.actors() : champion() ? List.of(CHAMPION)
                : setup != null && setup.encounter() != null ? setup.encounter().actors() : List.<GymCatalog.Actor>of();
        if (!pvp && megaTrial == null && legendary == null && opponents.isEmpty()) return;
        ArenaIntro.Portrait boss = null;
        if (megaTrial != null) {
            boss = ArenaIntro.Portrait.pokemon(ResourceLocation.fromNamespaceAndPath("cobblemon", megaTrial.species()),
                    Component.translatable("rumble.arena.intro.mega_name", Component.translatable(megaTrial.speciesKey()),
                            Component.translatable("rumble.mega.form." + megaTrial.form())), List.copyOf(megaTrial.aspects()));
        } else if (legendary != null) {
            var lead = legendary.foe();
            boss = ArenaIntro.Portrait.pokemon(lead.speciesId(), Component.translatable(legendary.titleKey()), List.copyOf(lead.aspects()));
        } else if (!opponents.isEmpty()) {
            var lead = opponents.getFirst();
            Component name = lead.name();
            for (var other : opponents.subList(1, opponents.size())) name = name.copy().append(" / ").append(other.name());
            boss = ArenaIntro.Portrait.trainer(name, lead.aspect());
        }
        var support = opponents.stream().skip(1)
                .map(actor -> ArenaIntro.Portrait.trainer(actor.name(), actor.aspect())).toList();
        for (UUID id : getMembers().keySet()) {
            var viewer = server.getPlayerList().getPlayer(id);
            if (viewer == null) continue;
            var opponent = boss;
            if (pvp) {
                var rival = server.getPlayerList().getPlayer(id.equals(setup.owner()) ? setup.guest() : setup.owner());
                if (rival == null) continue;
                opponent = ArenaIntro.Portrait.player(rival.getUUID(), rival.getDisplayName());
            }
            var own = ArenaIntro.Portrait.player(id, viewer.getDisplayName());
            if (frontier != null) {
                boolean host = id.equals(setup.owner());
                PacketDistributor.sendToPlayer(viewer, new ArenaIntro.Start(getId(), getDimension().location(), own, opponent,
                        introRoster(host ? frontier.host() : frontier.guest()), introRoster(host ? frontier.guest() : frontier.host())));
            } else {
                PacketDistributor.sendToPlayer(viewer, new ArenaIntro.Start(getId(), getDimension().location(), own, opponent, List.of(), support));
            }
        }
        introAt = server.getTickCount();
        startIntroMusic(server);
    }

    private void startIntroMusic(MinecraftServer server) {
        for (UUID id : getMembers().keySet()) {
            var viewer = server.getPlayerList().getPlayer(id);
            if (viewer != null) BattleMusic.arenaIntro(viewer, this);
        }
    }

    private static List<ArenaIntro.Portrait> introRoster(List<Pokemon> roster) {
        return roster.stream().map(pokemon -> ArenaIntro.Portrait.pokemon(pokemon.getSpecies().getResourceIdentifier(),
                pokemon.getSpecies().getTranslatedName(), pokemon.getAspects().stream().sorted().limit(32).toList())).toList();
    }

    private void readyForBattle(MinecraftServer server) {
        // Keep the scene available even if the preparation path did not start it.
        playIntro(server);
        int now = server.getTickCount();
        battleGate = now + ArenaIntro.READY_TICKS;
        if (introAt == null) return;
        int elapsed = now - introAt;
        battleGate = now + Math.max(ArenaIntro.READY_TICKS, ArenaIntro.MINIMUM_TICKS - elapsed);
        closeIntro(server, ArenaIntro.READY);
    }

    private void closeIntro(MinecraftServer server, byte state) {
        if (introAt == null) return;
        for (UUID id : getMembers().keySet()) {
            var player = server.getPlayerList().getPlayer(id);
            if (player != null) PacketDistributor.sendToPlayer(player, new ArenaIntro.Transition(getId(), state));
        }
    }

    @SubscribeEvent
    public static void started(InstanceEvent.Started event) {
        if (!(event.getSession() instanceof ArenaMatch match)) return;
        var server = match.getLevel().getServer();
        if (match.teamGame == null && match.setup == null && match.megaTrial == null && match.legendary == null && match.cinema == null) throw new IllegalStateException("Unregistered arena challenge");
        // The eastern throw animation needs entity ticks even with a small server simulation distance.
        match.getLevel().getChunkSource().addRegionTicket(COMBAT_TICKET, ARENA_CENTER, COMBAT_TICKET_DISTANCE, match.getId());
        if (match.cinema != null) {
            match.cinema.prepare(server);
            match.readyForBattle(server);
            return;
        }
        if (match.legendary != null) {
            match.prepareLegendary();
            return;
        }
        if (match.teamGame != null) {
            int[] lanes = new int[2];
            for (var entry : match.teamGame.entries().entrySet()) {
                var player = server.getPlayerList().getPlayer(entry.getKey());
                if (player == null || !match.getMembers().containsKey(entry.getKey())) throw new IllegalStateException("Frontier team changed during startup");
                int side = entry.getValue().side();
                var pos = frontierSpawn(side, lanes[side]++);
                var fighter = Fighter.frontier(player, pos, side == 0 ? -90 : 90, entry.getValue().roster());
                match.team.put(entry.getKey(), fighter);
                match.placeFrontier(fighter);
            }
            match.readyForBattle(server);
            return;
        }
        if (match.partyBattle()) {
            int index = 0;
            for (UUID id : match.getMembers().keySet()) {
                var participant = server.getPlayerList().getPlayer(id);
                if (participant == null || !(match.champion() ? ChampionStory.eligible(participant)
                        : match.megaTrial != null ? MegaTrials.unlocked(participant) : GymStory.canChallenge(participant, match.leader)))
                    throw new IllegalStateException("Challenge party changed before setup");
                double lane = laneOffset(index++);
                var fighter = new Fighter(participant, WEST.add(index == 6 ? 2 : 0, 0, lane), -90);
                match.team.put(id, fighter);
                if (!fighter.hasReserve()) throw new IllegalStateException("Gym participant has no usable Pokémon");
                match.positionAndPrompt(fighter);
            }
            var actor = match.champion() ? CHAMPION : match.megaTrial != null ? match.megaTrial.actor() : match.leader.actors().getFirst();
            match.trainers.add(new Trainer(match, actor, 0, match.team.size()));
            match.readyForBattle(server);
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
        if (match.frontier != null) {
            match.a = Fighter.frontier(owner, WEST, -90, match.frontier.host());
            match.b = Fighter.frontier(guest, EAST, 90, match.frontier.guest());
            match.placeFrontier(match.a);
            match.placeFrontier(match.b);
            match.readyForBattle(server);
            return;
        }
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
                match.trainers.add(new Trainer(match, actors.get(i), lane, 1));
            }
        }
        // Let the dimension and entity tracking initialize before taking over a player's camera.
        match.readyForBattle(server);
    }

    private void prepareLegendary() {
        var server = getLevel().getServer();
        int index = 0;
        for (UUID id : getMembers().keySet()) {
            var player = server.getPlayerList().getPlayer(id);
            if (player == null || !LegendaryCommission.unlocked(player))
                throw new IllegalStateException("Legendary party changed before setup");
            var fighter = new Fighter(player, WEST.add(0, 0, laneOffset(index++)), -90);
            team.put(id, fighter);
            if (!fighter.hasReserve()) throw new IllegalStateException("Legendary participant has no usable Pokémon");
            fighter.active = 0;
            positionAndPrompt(fighter);
            LegendaryCommission.acceptTrial(player, legendary);
        }
        readyForBattle(server);
    }

    private void spawnLegendaryFoe() {
        var foe = legendary.foe();
        var pokemon = foe.create(legendary.id());
        // RTC normalizes incoming damage against the Pokémon's own HP, not just the entity attribute.
        ArenaPokemonHealth.apply(pokemon, 2);
        PokemonEntity body = pokemon.sendOut(getLevel(), GYM_EAST_LEAD, null, mon -> {
            mon.setYRot(90);
            mon.setYBodyRot(90);
            mon.setYHeadRot(90);
            mon.setPersistenceRequired();
            mon.getEntityData().set(PokemonEntity.getUNBATTLEABLE(), true);
            return Unit.INSTANCE;
        });
        if (body == null || body.isRemoved()) throw new IllegalStateException("Failed to deploy " + foe.species());
        wildFoes.add(body);
        var health = body.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.addTransientModifier(new AttributeModifier(
                PrologueWorld.id("legendary_boss_health"), 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        body.setHealth(body.getMaxHealth());
        Brawl.train(body, Brawl.Difficulty.HARD);
    }

    private static Vec3 frontierSpawn(int side, int index) {
        var base = side == 0 ? WEST : EAST;
        if (index < 5) return base.add(0, 0, laneOffset(index));
        // Spread larger teams across their halves rather than stacking all extra players on the pads.
        int cell = (index - 5) % 99;
        int lane = cell % 11, row = cell / 11;
        double depth = 3 + row * 1.8;
        double lateral = (lane == 0 ? 0 : (lane % 2 == 0 ? lane / 2 : -(lane + 1) / 2)) * 2.5;
        return base.add(side == 0 ? depth : -depth, 0, lateral);
    }

    private static double laneOffset(int index) {
        // Stagger across the blue pad, then reuse positions for larger parties.
        return switch (index % 5) { case 1 -> -1.15; case 2 -> 1.15; case 3 -> -2.3; case 4 -> 2.3; default -> 0; };
    }

    private void positionAndPrompt(Fighter fighter) {
        var at = fighter.spawn;
        fighter.player.connection.teleport(at.x, at.y, at.z, fighter.yaw, 0);
        PacketDistributor.sendToPlayer(fighter.player, new ArenaPackets.Prepare(fighter.player.level().dimension().location()));
        if (!gimmickLesson() && legendary == null) prepareHint(fighter);
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

        Trainer(ArenaMatch match, GymCatalog.Actor actor, double lane, int healthScale) {
            ServerLevel level = match.getLevel();
            this.actor = actor;
            this.healthScale = healthScale;
            spawn = GYM_EAST_LEAD.add(0, 0, lane);
            List<Pokemon> source = match.hardLeader && match.leader != null
                    && match.leader.actors().getFirst().id().equals(actor.id())
                    ? GymLeaderTeams.hard(match.leader.gym()) : actor.team().get();
            roster = source.stream().map(mon -> mon.clone(false, level.registryAccess()))
                    .collect(Collectors.toCollection(ArrayList::new));
            if (roster.isEmpty()) throw new IllegalStateException("Missing challenge team for " + actor.id());
            for (Pokemon mon : roster) {
                GymTeamPokemon.prepareArenaMoves(mon, match.megaTrial == null ? actor.id() : match.megaTrial.id(),
                        match.megaTrial != null || match.champion() || match.setup != null && match.setup.story() != null
                                || "ground_leader".equals(actor.id()));
                UncatchableProperty.INSTANCE.uncatchable().apply(mon);
            }
            if (match.megaTrial != null) {
                npc = null;
            } else {
                npc = GymStory.newTrainer(level, actor, EAST.add(0, 0, lane), 90);
                npc.setNoAi(true);
                if (match.setup != null && match.setup.story() != null) match.setup.story().configureTrainer(npc);
                var party = new NPCPartyStore(npc);
                npc.setParty(party);
                for (Pokemon pokemon : roster) {
                    if (!party.add(pokemon)) throw new IllegalStateException("Gym team exceeds party capacity: " + actor.id());
                }
                if (!level.addFreshEntity(npc)) throw new IllegalStateException("Could not create trainer " + actor.id());
            }
        }

        boolean out() { return index >= roster.size(); }

        boolean onLastKnockout() {
            return out() || body != null && index + 1 >= roster.size()
                    && (knockedOut || !body.isAlive() || body.getPokemon().isFainted());
        }

        void tick(ArenaMatch match, ServerLevel level, int now, Brawl.Difficulty difficulty) {
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
                if (now % 2 == 0) {
                    Fighter target = match.team.values().stream().filter(f -> !match.gone(f) && f.body != null)
                            .min(Comparator.comparingDouble(f -> body.distanceToSqr(f.body))).orElse(null);
                    if (target != null && body.distanceToSqr(target.body) <= 28 * 28)
                        body.getLookControl().setLookAt(target.body, 28, 24);
                }
                if (megaAt != 0 && now >= megaAt) {
                    finishMega(body, difficulty);
                } else if (megaAt == 0 && body.getHealth() <= body.getMaxHealth() / 2) MegaCombat.heal(body);
                return;
            }
            if (out() || now < nextSend) return;
            if (npc == null) {
                deployed(level, now, difficulty, roster.get(index).sendOut(level, spawn, null, this::configureBody));
            } else if (sending == null) {
                npc.setInvisible(false);
                pendingSince = now;
                sending = roster.get(index).sendOutWithAnimation(npc, level, spawn, null, true, null, this::configureBody);
            } else if (sending.isDone()) {
                var sent = sending.join();
                sending = null;
                deployed(level, now, difficulty, sent);
                npc.setInvisible(true);
            } else if (now - pendingSince > 140) {
                throw new IllegalStateException("Send-out timed out for " + actor.id());
            }
        }

        private Unit configureBody(PokemonEntity entity) {
            entity.setYRot(90);
            entity.setYBodyRot(90);
            entity.setYHeadRot(90);
            entity.setPersistenceRequired();
            entity.getEntityData().set(PokemonEntity.getUNBATTLEABLE(), true);
            return Unit.INSTANCE;
        }

        private void deployed(ServerLevel level, int now, Brawl.Difficulty difficulty, PokemonEntity sent) {
            if (sent == null || sent.isRemoved()) throw new IllegalStateException("Failed to send out " + actor.id());
            body = sent;
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
                megaAt = now + RealtimeProtection.MEGA_TICKS;
                body.setNoAi(true);
                if (GimmickCinematics.start(body, npc, null, GimmickCinematics.Kind.MEGA_EVOLUTION,
                        () -> finishMega(sent, difficulty))) return;
                MegaCombat.begin(level, body);
            } else Brawl.train(body, difficulty);
        }

        private void finishMega(PokemonEntity expected, Brawl.Difficulty difficulty) {
            if (body != expected || !body.isAlive()) return;
            megaAt = 0;
            MegaCombat.evolve(body);
            body.setNoAi(false);
            Brawl.train(body, difficulty);
        }

        void recallActive(int now) {
            if (body == null) return;
            PokemonEntity oldBody = body;
            megaAt = 0;
            body = null; // Disarm targeting and permit Cobblemon's native recall event.
            oldBody.setNoAi(true);
            oldBody.getNavigation().stop();
            if (npc == null) {
                roster.get(index).recall();
                nextSend = now + 20;
            } else {
                npc.setInvisible(false);
                recalling = roster.get(index);
                if (!oldBody.isRemoved()) oldBody.recallWithAnimation();
                nextSend = now + RECALL_TICKS;
            }
        }

        void result(boolean playerWon) {
            if (body != null) body.setNoAi(true);
            if (npc == null) return;
            npc.setInvisible(false);
            npc.playAnimation(playerWon ? NPCEntity.LOSE_ANIMATION : NPCEntity.WIN_ANIMATION, List.of());
        }

        void clean() {
            for (Pokemon pokemon : roster) pokemon.recall();
            if (npc != null) npc.discard();
            if (sending != null) sending.thenAccept(e -> { if (e != null) e.getPokemon().recall(); });
        }
    }

    private static final class Fighter {
        final ServerPlayer player;
        final PartyStore detached = new PartyStore(UUID.randomUUID());
        final List<Pokemon> roster = new ArrayList<>();
        final Map<Integer, CompoundTag> startingRoster = new LinkedHashMap<>();
        final Map<Integer, CompoundTag> used = new LinkedHashMap<>();
        final Map<UUID, Pokemon> personalCopies = new LinkedHashMap<>();
        final int[] slotToRoster = {-1, -1, -1, -1, -1, -1};
        final Vec3 spawn;
        final float yaw;
        int active = -1, round, unsent, nextHint;
        long megaReady;
        boolean controlled, choosing, startRequested, megaUsed;
        boolean catching;
        boolean eliminated;
        Pokemon teraSlot;
        PokemonEntity body;
        Vec3 replacementAt;

        Fighter(ServerPlayer player, Vec3 spawn, float yaw) {
            this(player, spawn, yaw, null);
            var original = Cobblemon.INSTANCE.getStorage().getParty(player);
            for (int slot = 0; slot < slotToRoster.length; slot++) {
                var source = original.get(slot);
                if (source == null) continue;
                startingRoster.put(slot, ArenaRoster.pokemon(source));
                if (source.isFainted() || source.getTetheringId() != null) continue;
                if (source.getEntity() != null) source.recall();
                var copy = source.clone(true, player.registryAccess());
                personalCopies.put(source.getUuid(), copy);
                UncatchableProperty.INSTANCE.uncatchable().apply(copy);
                detached.add(copy);
                slotToRoster[slot] = roster.size();
                roster.add(copy);
            }
        }

        Fighter(ServerPlayer player, Vec3 spawn, float yaw, Pokemon prescribed) {
            this.player = player;
            this.spawn = spawn;
            this.yaw = yaw;
            if (prescribed != null) {
                UncatchableProperty.INSTANCE.uncatchable().apply(prescribed);
                detached.add(prescribed);
                roster.add(prescribed);
                startingRoster.put(0, ArenaRoster.pokemon(prescribed));
                slotToRoster[0] = 0;
                active = 0;
            }
        }

        static Fighter frontier(ServerPlayer player, Vec3 spawn, float yaw, List<Pokemon> roster) {
            var fighter = new Fighter(player, spawn, yaw, (Pokemon) null);
            var party = Cobblemon.INSTANCE.getStorage().getParty(player);
            for (var real : party.toGappyList()) if (real != null && real.getEntity() != null) real.recall();
            for (int i = 0; i < roster.size(); i++) {
                var pokemon = roster.get(i);
                UncatchableProperty.INSTANCE.uncatchable().apply(pokemon);
                fighter.detached.add(pokemon);
                fighter.roster.add(pokemon);
                fighter.startingRoster.put(i, ArenaRoster.pokemon(pokemon));
                fighter.slotToRoster[i] = i;
            }
            fighter.active = 0;
            return fighter;
        }

        boolean canSelect(int index) {
            return index >= 0 && index < roster.size() && !roster.get(index).isFainted();
        }

        boolean hasReserve() { return roster.stream().anyMatch(p -> !p.isFainted()); }
        boolean knockedOut() { return body != null && current() != null && current().isFainted(); }

        void writeBackHealth() {
            if (personalCopies.isEmpty()) return;
            var party = Cobblemon.INSTANCE.getStorage().getParty(player);
            for (var source : party.toGappyList()) {
                if (source == null) continue;
                var copy = personalCopies.get(source.getUuid());
                if (copy == null) continue;
                source.setCurrentHealth(copy.isFainted() ? 0 : Math.min(source.getMaxHealth(), copy.getCurrentHealth()));
                source.setFaintedTimer(copy.getFaintedTimer());
            }
            personalCopies.clear();
        }
        void recordDeployment() {
            if (active >= 0 && current() != null) used.put(active, ArenaRoster.pokemon(current()));
        }

        String snapshot() {
            return ArenaRoster.snapshot(player.getGameProfile().getName(), startingRoster,
                    slot -> used.get(slotToRoster[slot]));
        }

        Pokemon current() { return active < 0 ? null : roster.get(active); }
    }

    private static final class CinemaBattle {
        private static final Vec3 CENTER = new Vec3(0.5, 64, 0.5);
        private final ArenaMatch match;
        private final CinemaTrials.Trial trial;
        private record Role(int side, int slot) {}
        private record Actor(int side, CinemaAi ai) {}
        private final Map<UUID, Role> roles = new HashMap<>();
        private final Map<PokemonEntity, Actor> actors = new LinkedHashMap<>();
        private final Map<UUID, ArenaPackets.Allies> alliesByViewer = new HashMap<>();
        private boolean playerHit;
        private int winningSide = -1;

        CinemaBattle(ArenaMatch match, CinemaTrials.Trial trial) {
            this.match = match;
            this.trial = trial;
        }

        void pruneRoles() {
            roles.keySet().retainAll(match.getMembers().keySet());
        }

        boolean pick(UUID player, int faction, int slot) {
            pruneRoles();
            if (trial.castAt(faction, slot) == null) return false;
            // Keep duplicate picks visible to everyone; readiness and start reject them.
            roles.put(player, new Role(faction, slot));
            return true;
        }

        Set<UUID> conflicts() {
            var firstBySpecies = new HashMap<String, UUID>();
            Set<UUID> conflicts = new HashSet<>();
            roles.forEach((id, role) -> {
                if (!match.getMembers().containsKey(id)) return;
                var first = firstBySpecies.putIfAbsent(trial.castAt(role.side(), role.slot()).species(), id);
                if (first != null) {
                    conflicts.add(first);
                    conflicts.add(id);
                }
            });
            return conflicts;
        }

        private int playerSide(UUID id) {
            var role = roles.get(id);
            return role == null ? -1 : role.side();
        }

        void playIntro(MinecraftServer server) {
            for (var id : match.getMembers().keySet()) {
                var player = server.getPlayerList().getPlayer(id);
                if (player == null) continue;
                var role = roles.get(id);
                var partners = new ArrayList<ArenaIntro.Portrait>();
                var rivals = new ArrayList<ArenaIntro.Portrait>();
                for (int side = 0; side < trial.sides().size(); side++) {
                    var lineup = trial.sides().get(side);
                    for (int slot = 0; slot < lineup.size(); slot++) {
                        if (side != role.side()) rivals.add(lineup.get(slot).portrait());
                        else if (slot != role.slot()) partners.add(lineup.get(slot).portrait());
                    }
                }
                PacketDistributor.sendToPlayer(player, new ArenaIntro.Start(match.getId(), match.getDimension().location(),
                        trial.castAt(role.side(), role.slot()).portrait(), rivals.getFirst(), partners, List.copyOf(rivals.subList(1, rivals.size()))));
            }
        }

        void prepare(MinecraftServer server) {
            var players = new HashMap<Role, UUID>();
            match.getMembers().keySet().forEach(id -> players.put(roles.get(id), id));
            for (int faction = 0; faction < trial.sides().size(); faction++) {
                var roster = trial.sides().get(faction);
                // Place opposing factions on distinct arcs and offset large teams along the tangent.
                double angle = Math.PI + faction * Math.PI * 2 / trial.sides().size();
                var radial = new Vec3(Math.cos(angle), 0, Math.sin(angle));
                var tangent = new Vec3(-radial.z, 0, radial.x);
                float yaw = (float) Math.toDegrees(Math.atan2(-radial.z, -radial.x)) - 90;
                for (int slot = 0; slot < roster.size(); slot++) {
                    Vec3 position = CENTER.add(radial.scale(12)).add(tangent.scale((slot - (roster.size() - 1) / 2.0) * 8));
                    var pokemon = roster.get(slot).create(trial.id());
                    ArenaPokemonHealth.apply(pokemon, trial.healthScale(faction));
                    var member = players.get(new Role(faction, slot));
                    if (member != null) {
                        var player = server.getPlayerList().getPlayer(member);
                        if (player == null || !CinemaQuest.unlocked(player)) throw new IllegalStateException("Cinema party changed before setup");
                        var fighter = new Fighter(player, position, yaw, pokemon);
                        match.team.put(player.getUUID(), fighter);
                        player.connection.teleport(position.x, position.y, position.z, yaw, 0);
                    } else {
                        UncatchableProperty.INSTANCE.uncatchable().apply(pokemon);
                        var body = pokemon.sendOut(match.getLevel(), position, null, entity -> {
                            entity.moveTo(position.x, position.y, position.z, yaw, 0);
                            entity.setPersistenceRequired();
                            entity.setNoAi(true);
                            entity.getEntityData().set(PokemonEntity.getUNBATTLEABLE(), true);
                            return Unit.INSTANCE;
                        });
                        if (body == null || body.isRemoved()) throw new IllegalStateException("Cannot deploy cinema Pokémon " + pokemon.getSpecies());
                        actors.put(body, new Actor(faction, new CinemaAi(body)));
                    }
                }
            }
        }

        void tick(InstanceManager manager) {
            int now = manager.getServer().getTickCount();
            if (now < match.battleGate) return;
            if (!match.fighting) {
                for (var fighter : match.team.values()) if (!match.gone(fighter)) match.sendIn(fighter);
                if (match.team.values().stream().anyMatch(f -> !match.gone(f) && !Smash.active(f.player))) return;
                match.fighting = true;
                actors.keySet().forEach(body -> body.setNoAi(false));
            }
            var standing = match.team.values().stream().filter(f -> !match.gone(f) && f.hasReserve() && Smash.active(f.player)).toList();
            if (standing.isEmpty()) { manager.end(match, ExitReason.DEFEAT, false); return; }
            for (var fighter : match.team.values()) if (!standing.contains(fighter) && fighter.body != null) {
                if (Smash.active(fighter.player)) Smash.end(fighter.player, null, false);
                fighter.body.discard();
                fighter.body = null;
            }
            int survivingPlayerSide = playerSide(standing.getFirst().player.getUUID());
            if (standing.stream().allMatch(f -> playerSide(f.player.getUUID()) == survivingPlayerSide)
                    && actors.entrySet().stream().noneMatch(e -> e.getValue().side() != survivingPlayerSide && alive(e.getKey()))) {
                winningSide = survivingPlayerSide;
                match.trainerResolved = match.trainerWon = true;
                if (!manager.win(match.getLevel(), GYM_WIN)) manager.end(match, ExitReason.ERROR, false);
                return;
            }
            for (var entry : actors.entrySet()) {
                var pokemon = entry.getKey();
                var actor = entry.getValue();
                if (!alive(pokemon)) {
                    if (((ArenaKnockoutVisual) pokemon).rumble$isKnockedOut() && !pokemon.isRemoved()) pokemon.discard();
                    continue;
                }
                var target = closestOpponent(pokemon, actor.side(), standing);
                actor.ai().tick(match.getLevel(), target, this::opponents, playerHit);
            }
        }

        private LivingEntity closestOpponent(PokemonEntity pokemon, int faction, List<Fighter> standing) {
            LivingEntity closest = null;
            double distance = Double.MAX_VALUE;
            for (var fighter : standing) {
                if (playerSide(fighter.player.getUUID()) == faction) continue;
                double next = pokemon.distanceToSqr(fighter.player);
                if (next < distance) { closest = fighter.player; distance = next; }
            }
            for (var entry : actors.entrySet()) {
                if (entry.getValue().side() == faction || !alive(entry.getKey())) continue;
                double next = pokemon.distanceToSqr(entry.getKey());
                if (next < distance) { closest = entry.getKey(); distance = next; }
            }
            return closest;
        }

        private static boolean alive(PokemonEntity pokemon) {
            return pokemon.isAlive() && !pokemon.getPokemon().isFainted() && !((ArenaKnockoutVisual) pokemon).rumble$isKnockedOut();
        }

        private int factionOf(Entity entity) {
            var actor = actors.get(entity);
            if (actor != null) return actor.side();
            if (entity == null || entity.level() != match.getLevel()) return -1;
            var session = entity instanceof ServerPlayer player ? Smash.session(player) : Smash.sessionOf(entity);
            if (session == null && entity instanceof PokemonEntity) {
                for (var candidate : Smash.sessions()) {
                    if (candidate.combat.decoys.contains(entity)) {
                        session = candidate;
                        break;
                    }
                }
            }
            return session != null && match.fighter(session.player) != null ? playerSide(session.player.getUUID()) : -1;
        }

        boolean opponents(Entity attacker, Entity target) {
            int from = factionOf(attacker), to = factionOf(target);
            return from >= 0 && to >= 0 && from != to;
        }

        boolean canDamage(LivingIncomingDamageEvent event) {
            var source = event.getSource();
            Entity attacker = source.getEntity();
            if (factionOf(attacker) < 0) attacker = source.getDirectEntity();
            Entity target = event.getEntity();
            return opponents(attacker, target) && (playerHit || !actors.containsKey(attacker) || !actors.containsKey(target));
        }

        void clean() { actors.keySet().forEach(Entity::discard); actors.clear(); }
    }

    public boolean cinemaRoom() { return cinema != null; }

    public boolean cinemaRoleSelected(ServerPlayer player) {
        return cinema == null || cinema.roles.containsKey(player.getUUID());
    }

    public boolean cinemaRoleConflicts() {
        return cinema != null && !cinema.conflicts().isEmpty();
    }

    public boolean cinemaRoleConflicts(ServerPlayer player) {
        return cinema != null && cinema.conflicts().contains(player.getUUID());
    }

    public boolean cinemaRolesReady() {
        return cinema == null || !getMembers().isEmpty() && !cinemaRoleConflicts()
                && getMembers().keySet().stream().allMatch(cinema.roles::containsKey);
    }

    public void selectCinemaRole(ServerPlayer player, int side, int slot) {
        if (cinema == null || getState() != State.WAITING || !getMembers().containsKey(player.getUUID())
                || !CinemaQuest.unlocked(player)) return;
        if (!cinema.pick(player.getUUID(), side, slot)) {
            player.displayClientMessage(Component.translatable("rumble.cinema.role_unavailable"), false);
            InstanceNetwork.refreshParty(player, this);
            return;
        }
        getMembers().get(player.getUUID()).ready = false;
        cinema.conflicts().forEach(id -> getMembers().get(id).ready = false);
        InstanceManager.get(player.server).syncParty(this);
        InstanceNetwork.openParty(player);
    }

    public void writeCinemaRoles(CompoundTag data) {
        cinema.pruneRoles();
        var conflicts = cinema.conflicts();
        data.putBoolean("conflict", !conflicts.isEmpty());
        var selections = new CompoundTag();
        cinema.roles.forEach((id, role) -> {
            var entry = new CompoundTag();
            entry.putInt("side", role.side());
            entry.putInt("slot", role.slot());
            entry.putString("name", getMembers().get(id).name);
            entry.putBoolean("conflict", conflicts.contains(id));
            selections.put(id.toString(), entry);
        });
        data.put("selections", selections);
    }

    public static boolean isCinemaActor(PokemonEntity pokemon) {
        var match = group(pokemon);
        return match != null && match.cinema != null && match.cinema.actors.containsKey(pokemon);
    }

    public static boolean cinemaActorDisallowsBrawl(PokemonEntity pokemon) {
        return isCinemaActor(pokemon);
    }

    public static void cinemaPlayerHit(LivingEntity target, DamageSource source, float damage) {
        if (damage <= 0 || !(target.level() instanceof ServerLevel level)
                || !(InstanceApi.find(level).orElse(null) instanceof ArenaMatch match)
                || match.cinema == null || match.cinema.playerHit || !match.fighting) return;
        var attacker = source.getEntity();
        var session = attacker instanceof ServerPlayer player ? Smash.session(player) : Smash.sessionOf(attacker);
        if (session != null && match.cinema.opponents(session.player, target)) match.cinema.playerHit = true;
    }

    @SubscribeEvent
    public static void cinemaActorDamaged(LivingDamageEvent.Post event) {
        cinemaPlayerHit(event.getEntity(), event.getSource(), event.getNewDamage());
    }

    public boolean cinemaRewardEligible(UUID id) {
        return cinema == null || cinema.winningSide >= 0 && cinema.playerSide(id) == cinema.winningSide;
    }

    @Override protected void onTick(InstanceManager manager) {
        try {
            if (GimmickCinematics.paused(this)) {
                if (teamGame != null && teamGame.minutes() > 0 && teamDeadline != 0) teamDeadline++;
                return;
            }
            if (!admittedSpectators && (frontier != null || teamGame != null)) {
                admittedSpectators = true;
                FrontierSpectators.enter(this, teamGame != null ? teamGame.spectators() : frontier.spectators());
            }
            syncOpponents();
            if (teamGame != null) { tickFrontierTeams(manager); return; }
            if (partyBattle()) syncAllies();
            if (frontier == null && teamGame == null && fighting && trainerResultAt == 0 && playPendingKnockouts(manager.getServer().getTickCount())) return;
            if (cinema != null) { cinema.tick(manager); return; }
            if (legendary != null) { tickLegendary(manager); return; }
            if (partyBattle()) { tickLeader(manager); return; }
            if (a == null) return;
            if (frontier != null) { tickFrontier(manager); return; }
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
                tickKnockoutRecall(now);
                if (now >= trainerResultAt) {
                    trainers.forEach(t -> t.result(trainerWon));
                    trainerResolved = true;
                    manager.end(this, trainerWon ? ExitReason.VICTORY : ExitReason.DEFEAT, false);
                }
                return;
            }
            if (gymEncounter() && b == null) {
                boolean playerOut = !a.hasReserve();
                boolean gymOut = trainers.stream().allMatch(Trainer::onLastKnockout);
                if (playerOut || gymOut) {
                    resolveTrainers(now, gymOut && !playerOut, List.of(a));
                    return;
                }
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
                if (!playerOut) for (var trainer : trainers) trainer.tick(this, getLevel(), now, Brawl.Difficulty.NORMAL);
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

    private void syncOpponents() {
        var fighters = partyBattle() ? List.copyOf(team.values()) : a == null ? List.<Fighter>of() : b == null ? List.of(a) : List.of(a, b);
        for (var viewer : fighters) {
            if (gone(viewer)) continue;
            var rivals = new ArrayList<ArenaPackets.Ally>();
            var actors = new ArrayList<Integer>();
            for (var candidate : fighters) {
                if (viewer == candidate || gone(candidate)) continue;
                if (teamGame != null ? side(viewer) == side(candidate)
                        : cinema != null ? cinema.playerSide(viewer.player.getUUID()) == cinema.playerSide(candidate.player.getUUID())
                        : b == null) continue;
                var session = Smash.session(candidate.player);
                int body = session != null && !session.pokemon.getPokemon().isFainted() ? session.pokemon.getId() : -1;
                rivals.add(new ArenaPackets.Ally(candidate.player.getUUID(), "", candidate.player.getId(), body));
            }
            for (var trainer : trainers) if (trainer.body != null && !trainer.knockedOut) actors.add(trainer.body.getId());
            for (var foe : wildFoes) if (foe.isAlive() && !foe.getPokemon().isFainted()) actors.add(foe.getId());
            if (cinema != null) cinema.actors.forEach((pokemon, actor) -> {
                if (actor.side() != cinema.playerSide(viewer.player.getUUID()) && CinemaBattle.alive(pokemon)) actors.add(pokemon.getId());
            });
            actors.sort(Integer::compare);
            var update = new ArenaPackets.Opponents(getDimension().location(), List.copyOf(rivals), List.copyOf(actors));
            if (!update.equals(opponents.put(viewer.player.getUUID(), update))) PacketDistributor.sendToPlayer(viewer.player, update);
        }
    }

    private void syncAllies() {
        var players = new ArrayList<ArenaPackets.Ally>();
        var actors = new ArrayList<Integer>();
        for (var f : team.values()) {
            if (gone(f)) continue;
            var control = Smash.session(f.player);
            int body = control != null && control.pokemon.isAlive() && !control.pokemon.getPokemon().isFainted()
                    ? control.pokemon.getId() : -1;
            players.add(new ArenaPackets.Ally(f.player.getUUID(), f.player.getGameProfile().getName(), f.player.getId(), body));
            if (cinema == null && control != null) control.combat.decoys.forEach(decoy -> actors.add(decoy.getId()));
        }
        if (cinema != null) {
            for (var viewer : team.values()) {
                if (gone(viewer)) continue;
                UUID id = viewer.player.getUUID();
                int side = cinema.playerSide(id);
                var teammates = players.stream().filter(p -> cinema.playerSide(p.player()) == side).toList();
                var friendly = new ArrayList<Integer>();
                cinema.actors.forEach((pokemon, actor) -> {
                    if (actor.side() == side && CinemaBattle.alive(pokemon)) friendly.add(pokemon.getId());
                });
                teammates.forEach(ally -> {
                    var control = Smash.session(team.get(ally.player()).player);
                    if (control != null) control.combat.decoys.forEach(decoy -> friendly.add(decoy.getId()));
                });
                friendly.sort(Integer::compare);
                var update = new ArenaPackets.Allies(getDimension().location(), teammates, List.copyOf(friendly));
                if (!update.equals(cinema.alliesByViewer.put(id, update))) PacketDistributor.sendToPlayer(viewer.player, update);
            }
            return;
        }
        actors.sort(Integer::compare);
        var update = new ArenaPackets.Allies(getDimension().location(), List.copyOf(players), List.copyOf(actors));
        if (update.equals(lastAllies)) return;
        lastAllies = update;
        for (var f : team.values()) if (!gone(f)) PacketDistributor.sendToPlayer(f.player, update);
    }

    private static final int KO_TICKS = FrontierKnockout.CUE_TICKS;

    private void placeFrontier(Fighter f) {
        var pos = f.spawn;
        f.player.setDeltaMovement(Vec3.ZERO);
        f.player.connection.teleport(pos.x, pos.y, pos.z, f.yaw, 0);
    }

    private int side(Fighter fighter) { return teamGame.entries().get(fighter.player.getUUID()).side(); }

    private void tickFrontierTeams(InstanceManager manager) {
        int now = manager.getServer().getTickCount();
        if (now < battleGate) return;
        if (teamResultAt != 0) {
            if (now >= teamResultAt) finishFrontierTeams(manager);
            return;
        }
        if (teamDeadline == 0) {
            teamDeadline = now + teamGame.minutes() * 60 * 20;
            if (teamGame.minutes() > 0) {
                teamClock = new net.minecraft.server.level.ServerBossEvent(Component.empty(),
                        net.minecraft.world.BossEvent.BossBarColor.BLUE, net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
                team.values().forEach(fighter -> teamClock.addPlayer(fighter.player));
            }
        }
        boolean timed = teamGame.minutes() > 0;
        boolean[] standing = new boolean[2];
        var allies = new ArrayList<ArenaPackets.Ally>();
        for (var fighter : team.values()) {
            if (gone(fighter)) continue;
            UUID id = fighter.player.getUUID();
            int side = side(fighter);
            if (!respawn.containsKey(id) && fighter.current().isFainted()) {
                teamScore[1 - side]++;
                respawn.put(id, timed ? now + 100 : Integer.MAX_VALUE);
                teamRecall.put(id, now + RealtimeKnockout.FLIGHT_TICKS);
                fighter.megaReady = 0;
            }
            if (respawn.containsKey(id)) {
                if (now >= teamRecall.getOrDefault(id, Integer.MAX_VALUE)) {
                    teamRecall.remove(id);
                    if (Smash.active(fighter.player)) Smash.end(fighter.player, null, false);
                    fighter.current().recall();
                    if (fighter.body != null) fighter.body.discard();
                    fighter.body = null;
                    placeFrontier(fighter);
                }
                if (!timed) continue;
                if (now % 20 == 0) fighter.player.displayClientMessage(Component.translatable("rumble.frontier.respawn",
                        Math.max(0, (respawn.get(id) - now + 19) / 20)), true);
                standing[side] = true;
                if (now < respawn.get(id)) continue;
                respawn.remove(id);
                fighter.current().heal();
                fighter.replacementAt = fighter.spawn;
            }
            tickHeld(fighter);
            sendIn(fighter);
            standing[side] = true;
            allies.add(new ArenaPackets.Ally(id, fighter.player.getGameProfile().getName(), fighter.player.getId(),
                    Smash.active(fighter.player) && fighter.body != null ? fighter.body.getId() : -1));
        }
        fighting = standing[0] && standing[1];
        frontierCombatStarted |= fighting;
        int seconds = timed ? Math.max(0, (teamDeadline - now + 19) / 20) : -1;
        for (var fighter : team.values()) {
            if (gone(fighter)) continue;
            int side = side(fighter);
            var friendly = allies.stream().filter(ally -> side(team.get(ally.player())) == side).toList();
            var decoys = new ArrayList<Integer>();
            friendly.forEach(ally -> {
                var control = Smash.session(team.get(ally.player()).player);
                if (control != null) control.combat.decoys.forEach(d -> decoys.add(d.getId()));
            });
            var update = new ArenaPackets.Allies(getDimension().location(), friendly, decoys);
            if (!update.equals(teamAllies.put(fighter.player.getUUID(), update))) PacketDistributor.sendToPlayer(fighter.player, update);
            if (timed && seconds != lastTeamSecond && seconds > 0 && seconds <= 10) {
                    fighter.player.playNotifySound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), net.minecraft.sounds.SoundSource.PLAYERS, 1F, 1.2F);
                    fighter.player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(0, 12, 6));
                    fighter.player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(Integer.toString(seconds))));
            }
        }
        if (teamClock != null) {
            teamClock.setName(Component.translatable("rumble.frontier.team_score", teamScore[0], teamScore[1], seconds / 60,
                    String.format(Locale.ROOT, "%02d", seconds % 60)));
            teamClock.setProgress(seconds / (teamGame.minutes() * 60F));
        }
        lastTeamSecond = seconds;
        if (!fighting || timed && seconds == 0) {
            fighting = false;
            teamWinner = FrontierRules.winner(timed, standing[0], standing[1], teamScore[0], teamScore[1]);
            teamResultAt = Math.max(now + 10, teamRecall.values().stream().mapToInt(Integer::intValue).max().orElse(now));
        }
    }

    private void finishFrontierTeams(InstanceManager manager) {
        frontierResolved = true;
        teamGame.result().accept(teamWinner);
        for (var fighter : team.values()) if (!gone(fighter)) fighter.player.sendSystemMessage(Component.translatable(
                teamWinner < 0 ? "rumble.frontier.team_draw" : teamWinner == side(fighter) ? "rumble.frontier.team_win" : "rumble.frontier.team_loss"));
        manager.end(this, teamWinner < 0 ? ExitReason.DEFEAT : ExitReason.VICTORY, true);
    }

    private void tickFrontier(InstanceManager manager) {
        boolean absentA = gone(a), absentB = gone(b);
        if (absentA || absentB) {
            settleFrontier(manager, absentA == absentB ? null : absentA ? b : a, true);
            return;
        }
        int now = manager.getServer().getTickCount();
        if (now < battleGate) return;
        if (knockoutUntil != 0) {
            if (now < knockoutUntil) return;
            knockoutUntil = 0;
            knockoutActors.clear();
            if (a.knockedOut()) resetFrontier(a);
            if (b.knockedOut()) resetFrontier(b);
            boolean outA = a.current() == null, outB = b.current() == null;
            if (outA || outB) {
                settleFrontier(manager, outA == outB ? null : outA ? b : a, false);
                return;
            }
        } else if (a.current().isFainted() || b.current().isFainted()) {
            fighting = false;
            knockoutUntil = now + KO_TICKS;
            showKnockout(List.of(a, b), a.knockedOut() ? a.body : b.body);
            return;
        }
        tickHeld(a);
        tickHeld(b);
        sendIn(a);
        sendIn(b);
        fighting = Smash.active(a.player) && Smash.active(b.player);
        frontierCombatStarted |= fighting;
    }

    private static void resetFrontier(Fighter f) {
        f.replacementAt = f.player.position();
        if (Smash.active(f.player)) Smash.end(f.player, null, false);
        if (f.body != null && !f.body.isRemoved()) f.body.discard();
        f.current().recall();
        f.body = null;
        f.megaReady = 0;
        f.unsent = 0;
        if (f.current().isFainted()) {
            int next = f.active + 1;
            while (next < f.roster.size() && f.roster.get(next).isFainted()) next++;
            f.active = next < f.roster.size() ? next : -1;
        }
    }

    private void settleFrontier(InstanceManager manager, Fighter winner, boolean immediate) {
        resolveFrontier(winner == null ? null : winner.player.getUUID());
        if (a != null && b != null) announce(winner);
        manager.end(this, winner == null ? ExitReason.DEFEAT : ExitReason.VICTORY, immediate);
    }

    private void resolveFrontier(UUID winner) {
        if (frontierResolved) return;
        frontierResolved = true;
        frontier.result().accept(winner);
    }

    public static float knockoutMovement(Entity entity) {
        if (entity instanceof PokemonEntity body && !RealtimeKnockout.inFlight(body) && entity.level() instanceof ServerLevel level
                && InstanceApi.find(level).orElse(null) instanceof ArenaMatch match
                && match.getState() == State.ACTIVE && match.knockoutActors.contains(entity)
                && level.getServer().getTickCount() < match.knockoutSlowUntil) return FrontierKnockout.SPEED;
        return 1;
    }

    public static boolean onKnockoutLaunch(PokemonEntity body) {
        var match = group(body);
        if (match == null || match.teamGame != null) return false;
        if (match.frontier != null) return true; // The draft match drives its own round transition.
        if (!match.fighting || match.trainerResultAt != 0) return false;
        int now = match.getLevel().getServer().getTickCount();
        // Capture the body and attacker at impact, while both are still tracked and before recall or settlement.
        match.beginKnockout(body, now);
        return true;
    }

    // RTC normally recalls immediately when HP reaches zero. Keep the last body in frame until the KO beat ends.
    public static boolean keepKnockoutBody(Smash.Session session) {
        var match = inArena(session.player);
        var fighter = match == null ? null : match.fighter(session.player);
        return fighter != null && fighter.body == session.pokemon && match.getState() == State.ACTIVE;
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

    private void tickLegendary(InstanceManager manager) {
        var standing = team.values().stream().filter(f -> !gone(f)).toList();
        if (standing.isEmpty()) { manager.end(this, ExitReason.EMPTY, true); return; }
        int now = manager.getServer().getTickCount();
        if (now < battleGate || trainerResolved) return;
        if (!fighting) {
            standing.forEach(this::sendIn);
            if (standing.stream().anyMatch(f -> !Smash.active(f.player))) return;
            spawnLegendaryFoe();
            fighting = true;
        }
        if (!capturePhase) {
            standing.forEach(this::check);
            if (standing.stream().noneMatch(Fighter::hasReserve)) { manager.end(this, ExitReason.DEFEAT, false); return; }
            var threats = wildFoes.stream().filter(PokemonEntity::isAlive).filter(f -> !WildCapture.stunned(f)).toList();
            if (threats.isEmpty()) {
                capturePhase = true;
                for (var fighter : standing) {
                    closeRoster(fighter);
                    releaseForCapture(fighter);
                }
            } else {
                if (!hasStunnedFoe()) for (var fighter : standing) fighter.catching = false;
                standing.forEach(this::tickHeld);
                standing.forEach(this::sendIn);
                // Provoke at combat start, and reacquire after a participant leaves or switches.
                for (var foe : threats) if (!Brawl.fighting(foe)) {
                    standing.stream().map(f -> Smash.session(f.player)).filter(Objects::nonNull)
                            .min(Comparator.comparingDouble(s -> foe.distanceToSqr(s.pokemon)))
                            .ifPresent(s -> Brawl.provoke(s, foe));
                }
            }
        }
        // A live but stunned foe can still be caught. Do not tear down a room during the ball animation.
        if (capturePhase && wildFoes.stream().noneMatch(PokemonEntity::isAlive)) {
            trainerResolved = trainerWon = true;
            if (!manager.win(getLevel(), GYM_WIN)) manager.end(this, ExitReason.ERROR, false);
        }
    }

    private boolean hasStunnedFoe() {
        return wildFoes.stream().anyMatch(foe -> foe.isAlive() && WildCapture.stunned(foe));
    }

    private void releaseForCapture(Fighter fighter) {
        fighter.catching = true;
        if (Smash.active(fighter.player)) Smash.end(fighter.player, null, false);
        // A pending forced switch may have suppressed RTC's empty-state packet.
        Smash.send(fighter.player, new SmashPayloads.State(fighter.player.getId(), -1, false));
        if (fighter.current() != null) fighter.current().recall();
        fighter.body = null;
        if (hasStunnedFoe()) fighter.player.displayClientMessage(Component.translatable("rumble.legendary.capture_phase", Component.keybind("key.use")), true);
    }

    public static void legendaryStunned(PokemonEntity foe) {
        var match = group(foe);
        if (match == null || match.legendary == null) return;
        for (var fighter : match.team.values()) if (!match.gone(fighter) && Smash.active(fighter.player))
            fighter.player.displayClientMessage(Component.translatable("rumble.legendary.capture_toggle", Component.keybind("key.realtimecobblemon.smash")), true);
    }

    private void tickLeader(InstanceManager manager) {
        int now = manager.getServer().getTickCount();
        if (team.isEmpty()) { manager.end(this, ExitReason.EMPTY, true); return; }
        if (trainerResultAt != 0) {
            tickKnockoutRecall(now);
            if (now >= trainerResultAt) {
                if (leader != null && getStartingTeam().size() > 1) {
                    if (gymPreviewUntil == 0) {
                        gymPreviewUntil = now + 100;
                        var preview = new ArenaPackets.GymReport(getDimension().location(), getBattleStatistics().snapshot().toTag());
                        for (var fighter : team.values()) if (!gone(fighter)) {
                            if (Smash.active(fighter.player)) Smash.end(fighter.player, null, false);
                            PacketDistributor.sendToPlayer(fighter.player, preview);
                        }
                        return;
                    }
                    if (now < gymPreviewUntil) return;
                }
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
        boolean teamOut = standing.stream().noneMatch(f -> !f.eliminated && f.hasReserve());
        if (!teamOut) trainers.forEach(trainer -> trainer.tick(this, getLevel(), now, Brawl.Difficulty.HARD));
        boolean gymOut = trainers.stream().allMatch(Trainer::out);
        if (teamOut || gymOut) {
            resolveTrainers(now, gymOut && !teamOut, standing);
            return;
        }
        standing.forEach(this::sendIn);
    }

    private boolean gymEncounter() { return leader != null || setup != null && setup.encounter() != null; }

    private void resolveTrainers(int now, boolean playerWon, List<Fighter> fighters) {
        trainerWon = playerWon;
        fighters.forEach(this::closeRoster);
        if (knockoutUntil != 0 && knockoutSlowUntil <= knockoutUntil && now <= knockoutUntil + 1) {
            knockoutRecallAt = now;
            trainerResultAt = now + RECALL_TICKS;
        } else {
            trainers.forEach(trainer -> trainer.recallActive(now));
            trainerResultAt = Math.max(now + 1, trainers.stream().mapToInt(t -> t.nextSend).max().orElse(now));
        }
    }

    private void tickKnockoutRecall(int now) {
        if (knockoutRecallAt == 0 || now < knockoutRecallAt) return;
        knockoutRecallAt = 0;
        knockoutActors.clear();
        trainers.forEach(trainer -> trainer.recallActive(now));
    }

    private void showKnockout(List<Fighter> fighters, PokemonEntity focus) {
        knockoutSlowUntil = getLevel().getServer().getTickCount() + FrontierKnockout.SLOW_TICKS;
        knockoutActors.clear();
        for (var fighter : fighters) {
            if (gone(fighter)) continue;
            knockoutActors.add(fighter.player);
            if (fighter.body != null) knockoutActors.add(fighter.body);
        }
        for (var trainer : trainers) if (trainer.body != null) knockoutActors.add(trainer.body);
        knockoutActors.addAll(wildFoes);
        if (cinema != null) knockoutActors.addAll(cinema.actors.keySet());
        var cue = new FrontierKnockout.Cue(getDimension().location(), KO_TICKS,
                focus == null ? -1 : focus.getId(), RealtimeKnockout.attackerId(focus), knockoutActors.stream().map(Entity::getId).toList());
        for (var fighter : fighters) if (!gone(fighter)) PacketDistributor.sendToPlayer(fighter.player, cue);
    }

    private boolean playPendingKnockouts(int now) {
        if (now < knockoutUntil) return true;
        var defeated = new ArrayList<PokemonEntity>();
        for (var f : knockoutFighters()) if (f.knockedOut()) defeated.add(f.body);
        for (var t : trainers) if (t.body != null && (t.knockedOut || !CinemaBattle.alive(t.body))) defeated.add(t.body);
        for (var foe : wildFoes) if (!CinemaBattle.alive(foe) || WildCapture.stunned(foe)) defeated.add(foe);
        if (cinema != null) for (var foe : cinema.actors.keySet()) if (!CinemaBattle.alive(foe)) defeated.add(foe);
        defeated.removeIf(body -> body.isRemoved() || shownKnockouts.contains(body.getId()));
        if (defeated.isEmpty()) return false;
        var focus = defeated.stream().filter(RealtimeKnockout::inFlight).findFirst().orElse(defeated.getFirst());
        if (focus.isAlive() && !RealtimeKnockout.wasLaunched(focus) && !WildCapture.stunned(focus)) {
            RealtimeKnockout.launch(focus, focus.getLastHurtByMob());
        }
        beginKnockout(focus, now);
        defeated.forEach(body -> shownKnockouts.add(body.getId()));
        return true;
    }

    private void beginKnockout(PokemonEntity focus, int now) {
        if (!shownKnockouts.add(focus.getId()) || now < knockoutUntil) return;
        knockoutUntil = now + KO_TICKS;
        showKnockout(knockoutFighters(), focus);
    }

    private List<Fighter> knockoutFighters() {
        if (partyBattle()) return List.copyOf(team.values());
        if (a == null) return List.of();
        return b == null ? List.of(a) : List.of(a, b);
    }

    private boolean gone(Fighter f) {
        return !f.player.isAlive() || f.player.hasDisconnected() || f.player.serverLevel() != getLevel();
    }

    private boolean canReplace(Fighter f) {
        return f.hasReserve() && (leader == null || gymReplacements.available());
    }

    private void eliminateGymFighter(Fighter f) {
        boolean exhausted = !gymReplacements.available();
        f.eliminated = true;
        closeRoster(f);
        if (f.body != null) f.body.discard();
        if (f.current() != null) f.current().recall();
        f.body = null;
        f.active = -1;
        f.replacementAt = null;
        // The KO path may already have suppressed RTC's empty-state packet while a replacement was available.
        Smash.send(f.player, new SmashPayloads.State(f.player.getId(), -1, false));
        if (getStartingTeam().size() > 1) {
            f.player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            if (exhausted) f.player.displayClientMessage(Component.translatable("rumble.gym.no_replacements"), true);
        }
    }

    private void check(Fighter f) {
        if (f.current() != null && f.current().isFainted()) {
            f.replacementAt = f.player.position();
            if (Smash.active(f.player)) Smash.end(f.player, null, false);
            if (f.body != null) f.body.discard();
            f.current().recall();
            f.body = null;
            f.megaReady = 0;
            f.active = -1;
            if (canReplace(f)) {
                f.round++;
                f.choosing = true;
                showRoster(f);
                f.player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.6F, 0.83F);
            } else if (leader != null) {
                eliminateGymFighter(f);
            }
        }
    }

    private void sendIn(Fighter f) {
        if (f.eliminated || f.current() == null || f.current().isFainted() || f.catching || Smash.active(f.player)) return;
        boolean gymReplacement = leader != null && f.replacementAt != null;
        if (gymReplacement && !gymReplacements.available()) {
            eliminateGymFighter(f);
            return;
        }
        if (f.body == null || !f.body.isAlive()) f.body = deploy(f, f.replacementAt != null ? f.replacementAt
                : f.controlled ? f.player.position() : hasTrainers() || legendary != null ? leadPosition(f) : f.spawn);
        if (f.body != null && Smash.start(f.player, f.body) == null) {
            f.unsent = 0;
            f.replacementAt = null;
            f.controlled = true;
            f.recordDeployment();
            closeRoster(f);
            if (gymReplacement) gymReplacementSucceeded();
        } else if (++f.unsent > 100) {
            throw new IllegalStateException("Could not send in arena Pokémon for " + f.player.getUUID());
        }
    }

    private void gymReplacementSucceeded() {
        gymReplacements.spend();
        for (var teammate : team.values()) {
            if (teammate.replacementAt == null || gone(teammate)) continue;
            if (!gymReplacements.available()) eliminateGymFighter(teammate);
            else if (teammate.choosing) showRoster(teammate);
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
        if (f == null || match.teamGame != null || match.frontier != null || match.legendary != null || match.cinema != null || match.fighting || match.gimmickLesson() || Smash.active(player) || player.serverLevel() != match.getLevel()
                || match.getState() != State.ACTIVE || slot < 0 || slot >= f.slotToRoster.length) return true;
        if (match.waitingInBlue(f) && !inBlue(player)) {
            keepInBlue(player);
            match.prepareHint(f);
            return true;
        }
        int index = f.slotToRoster[slot];
        boolean activeBody = f.body != null && f.body.isAlive();
        if (!f.canSelect(index)) return true;
        boolean recallOnly = f.active == index && activeBody;
        if (f.current() != null) {
            if (f.body != null && f.current().getEntity() != f.body) f.body.recallWithAnimation();
            else f.current().recall();
        }
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
                // Cobblemon can expose the spawned body before its detached party state resolves.
                f.body = e;
                return Unit.INSTANCE;
            });
        }
        if (match.waitingInBlue(f)) match.prepareHint(f);
        return true;
    }

    public static boolean toggle(ServerPlayer player) {
        var match = inArena(player);
        if (match == null) return false;
        var f = match.fighter(player);
        if (match.cinema != null) {
            player.displayClientMessage(Component.translatable("rumble.arena.mode_locked"), true);
            return true;
        }
        if (match.legendary != null && match.getState() == State.ACTIVE) {
            if (!match.capturePhase && match.fighting && f != null && !f.choosing && !match.gone(f)) {
                if (f.catching) f.catching = false;
                else if (Smash.active(player) && match.hasStunnedFoe()) match.releaseForCapture(f);
            }
            return true;
        }
        if (match.frontier != null || match.teamGame != null || match.fighting || match.gimmickLesson()) {
            player.displayClientMessage(Component.translatable("rumble.arena.mode_locked"), true);
        } else if (f != null && !Smash.active(player) && player.serverLevel() == match.getLevel()
                && match.getState() == State.ACTIVE) {
            if (f.body == null || !f.body.isAlive()) match.prepareHint(f);
            else f.startRequested = true;
        }
        return true;
    }

    private void tryEnter(Fighter f) {
        if (!f.startRequested || !f.canSelect(f.active) || f.body == null || f.body.getPokemon() != f.current()
                || !f.body.isAlive() || f.body.getBeamMode() != 0) return;
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
            f.recordDeployment();
            if (b != null) f.player.displayClientMessage(Component.translatable("rumble.arena.wait_opponent"), true);
        }
    }

    private void showRoster(Fighter f) {
        var choices = IntStream.range(0, f.roster.size()).mapToObj(i -> {
            Pokemon mon = f.roster.get(i);
            return new ArenaPackets.Option(i, mon);
        }).toList();
        int remaining = leader == null ? -1 : gymReplacements.remaining();
        PacketDistributor.sendToPlayer(f.player, new ArenaPackets.Switch(getId(), f.round, choices, remaining));
    }

    static void pick(ServerPlayer player, ArenaPackets.Pick packet) {
        ArenaMatch match = inArena(player);
        if (match == null || match.getState() != State.ACTIVE || match.getLevel() != player.serverLevel()
                || !match.getId().equals(packet.match())) return;
        Fighter f = match.fighter(player);
        if (f == null || !f.choosing || match.trainerResultAt != 0 || f.round != packet.round()) return;
        if (match.leader != null && !match.gymReplacements.available()) {
            match.eliminateGymFighter(f);
            return;
        }
        if (!f.canSelect(packet.index())) {
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
        if (pvpResultAnnounced) return;
        pvpResultAnnounced = true;
        if (winner != null && (frontier == null || frontierCombatStarted)) {
            Fighter loser = winner == a ? b : a;
            PvpStandings.get(winner.player.server).record(winner.player, loser.player);
        }
        for (var f : List.of(a, b)) f.player.sendSystemMessage(Component.translatable(winner == null ? "rumble.arena.draw"
                : f == winner ? "rumble.arena.win" : "rumble.arena.lose"));
    }

    @SubscribeEvent
    public static void ending(InstanceEvent.Ending event) {
        if (event.getSession() instanceof ArenaMatch match) {
            if (match.getLevel() != null) FrontierSpectators.end(match.getLevel().getServer(), match);
            if (match.teamClock != null) match.teamClock.removeAllPlayers();
            if (match.teamGame != null && !match.frontierResolved) {
                match.frontierResolved = true;
                match.teamGame.result().accept(-1);
            }
            if (match.frontier != null && !match.frontierResolved) {
                if (event.getReason() == ExitReason.ERROR) {
                    match.frontierResolved = true;
                    // Synchronous startup errors are reported by startFrontier's return value.
                    if (match.frontier != creatingFrontier) match.frontier.failed().run();
                } else match.resolveFrontier(null);
            }
            match.clean(match.a);
            match.clean(match.b);
            match.team.values().forEach(match::clean);
            if (match.cinema != null) match.cinema.clean();
            for (var foe : match.wildFoes) if (!foe.isRemoved()) foe.discard();
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
        if (match.introAt != null) PacketDistributor.sendToPlayer(event.getPlayer(), new ArenaIntro.Transition(match.getId(), ArenaIntro.CANCEL));
        if (match.frontier != null && match.getState() == State.ACTIVE && match.a != null && match.b != null) {
            var quitter = event.getPlayer();
            if (event.getReason() == ExitReason.VOLUNTARY || event.getReason() == ExitReason.DISCONNECTED
                    || event.getReason() == ExitReason.ILLEGAL_TRAVEL || event.getReason() == ExitReason.DEATH_LIMIT) {
                var opponent = match.a.player == quitter ? match.b : match.a;
                match.settleFrontier(InstanceManager.get(quitter.server), match.gone(opponent) ? null : opponent, true);
            }
            return;
        }
        if (match.teamClock != null) match.teamClock.removePlayer(event.getPlayer());
        if (match.partyBattle()) {
            var fighter = match.team.remove(event.getPlayer().getUUID());
            if (fighter == null) return;
            match.departedRosters.put(fighter.player.getUUID(), fighter.snapshot());
            if (match.champion() && event.getReason() == ExitReason.VICTORY
                    && match.getCompletedPlayers().contains(event.getPlayer().getUUID()))
                ChampionStory.returned(event.getPlayer());
            if (match.legendary != null && match.trainerResolved && event.getReason() == ExitReason.VICTORY
                    && match.getCompletedPlayers().contains(event.getPlayer().getUUID()))
                LegendaryCommission.cleared(event.getPlayer(), match.legendary);
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
    public static void cinemaCompleted(InstanceEvent.Completed event) {
        if (!(event.getSession() instanceof ArenaMatch match)) return;
        if (match.champion()) {
            var online = match.getLevel().getServer().getPlayerList();
            for (UUID id : event.getPlayers()) {
                var player = online.getPlayer(id);
                if (player != null) ChampionStory.won(player);
            }
            return;
        }
        if (match.cinema == null) return;
        var players = match.getLevel().getServer().getPlayerList();
        for (var id : event.getPlayers()) {
            var player = players.getPlayer(id);
            if (player != null) CinemaQuest.cleared(player);
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
        if (match != null && match.frontier == null && !match.partyBattle() && match.getState() != State.ENDING)
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
        f.writeBackHealth();
        if (gimmickLesson() && !f.player.hasDisconnected()) ProloguePackets.show(f.player, Cue.NONE, -1, false, 0);
        if (!f.player.hasDisconnected() && f.teraSlot != null) PacketDistributor.sendToPlayer(f.player, new ArenaPackets.TeraUsed(false));
        if (Smash.active(f.player)) Smash.end(f.player, null, false);
        Conditions.forget(f.player);
        if (!f.player.hasDisconnected() && f.controlled) Smash.send(f.player, new SmashPayloads.State(f.player.getId(), -1, false));
        closeRoster(f);
        if (f.body != null && !f.body.isRemoved()) f.body.discard();
        for (Pokemon mon : f.roster) {
            if (mon.getEntity() != null) mon.getEntity().discard();
            mon.recall();
            f.detached.remove(mon);
        }
    }

    /** Runs before InstanceManager removes membership or transfers the player. */
    public void beforeLeave(ServerPlayer player) {
        var fighter = fighter(player);
        if (fighter == null) return;
        GimmickCinematics.leaving(this, player);
        fighter.writeBackHealth();
        if (player.serverLevel() == getLevel() && Smash.active(player)) Smash.end(player, null, false);
    }

    private Fighter fighter(ServerPlayer p) {
        if (partyBattle()) {
            var f = team.get(p.getUUID());
            return f != null && f.player == p ? f : null;
        }
        return a != null && a.player == p ? a : b != null && b.player == p ? b : null;
    }

    public static ArenaMatch inArena(ServerPlayer p) { return InstanceApi.find(p).orElse(null) instanceof ArenaMatch match ? match : null; }
    public static ArenaMatch pvpWarningGroup(ServerPlayer player) {
        var match = inArena(player);
        return match != null && (match.b != null && !match.partyBattle() || match.teamGame != null)
                && match.fighter(player) != null ? match : null;
    }
    public static boolean inFrontier(ServerPlayer p) { var match = inArena(p); return match != null && (match.frontier != null || match.teamGame != null); }
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
            if (match.trainerOf(e) != null || match.wildFoes.contains(e) || match.cinema != null && match.cinema.actors.containsKey(e)) return match;
            for (var s : Smash.sessions()) if ((s.pokemon == e || s.combat.decoys.contains(e)) && match.fighter(s.player) != null) return match;
        }
        return null;
    }

    private Fighter combatant(Entity entity) {
        if (entity instanceof ServerPlayer p) return fighter(p);
        if (entity == null) return null;
        var control = Smash.sessionOf(entity);
        if (control != null) return fighter(control.player);
        for (var s : Smash.sessions()) if (s.combat.decoys.contains(entity)) return fighter(s.player);
        return null;
    }

    private Trainer trainerOf(Entity e) {
        for (var trainer : trainers) if (trainer.body == e && !trainer.knockedOut) return trainer;
        return null;
    }

    public static boolean isLegendaryFoe(PokemonEntity mon) {
        var match = group(mon);
        return match != null && match.legendary != null && match.wildFoes.contains(mon);
    }

    public static boolean canThrowAtLegendary(PokemonEntity mon, Entity thrower) {
        var match = group(mon);
        if (match == null || !(thrower instanceof ServerPlayer p) || p.serverLevel() != mon.level()
                || !p.isAlive() || p.isSpectator()) return false;
        var fighter = match.fighter(p);
        return fighter != null && (match.capturePhase || fighter.catching) && WildCapture.canThrowAt(mon);
    }

    /** Only active instance enemies react; PvP, wild encounters and tutorials retain native behavior. */
    public static int defenseTier(PokemonEntity entity) {
        var match = group(entity);
        if (match == null || !match.fighting || match.b != null || match.capturePhase
                || match.trainerResultAt != 0 || !entity.isAlive() || entity.isNoAi()) return 0;
        if (match.cinema != null) return 0;
        if (match.trainerOf(entity) != null && match.leader != null) return match.hardLeader ? 2 : 1;
        if (match.wildFoes.contains(entity) && match.legendary != null) return 2;
        return 0;
    }

    public static boolean isTrainerFighter(PokemonEntity entity) {
        var match = group(entity);
        return match != null && (entity.getPokemon().isNPCOwned() || match.trainerOf(entity) != null);
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
        if (RealtimeMoveWindup.locked(player)) return;
        var match = group(player);
        var session = Smash.session(player);
        if (session == null || session.pokemon.getPokemon().isFainted()) return;
        if (match != null && match.cinema != null) return;
        if (match == null) {
            if (PrologueTraining.restrict(player) || session.combat.lockedUntil > player.level().getGameTime()) return;
            var pokemon = session.pokemon.getPokemon();
            if (MegaCombat.canMega(pokemon) && MegaCombat.equipped(player) && MegaEvo.refusal(player, pokemon) == null)
                MegaEvo.onKey(player);
            else if (MegaCombat.heal(session.pokemon)) Smash.sendHud(session);
            return;
        }
        var f = match.fighter(player);
        if (f == null || f.choosing || f.body != session.pokemon || match.trainerResultAt != 0
                || !match.fighting && (!match.gimmickLesson() || match.teraLesson()) || f.megaReady != 0 || !f.body.isAlive()) return;
        long now = match.getLevel().getGameTime();
        if (session.combat.lockedUntil > now) return;
        if (!f.megaUsed && (match.frontier != null || match.teamGame != null || match.b != null || MegaCombat.equipped(player))
                && MegaCombat.canMega(f.current())) {
            if (GimmickCinematics.start(session, GimmickCinematics.Kind.MEGA_EVOLUTION,
                    () -> match.finishHeld(f, session))) return;
            f.megaReady = now + RealtimeProtection.MEGA_TICKS;
            session.combat.lockedUntil = f.megaReady;
            session.combat.lockReason = "is Mega Evolving!";
            MegaCombat.begin(match.getLevel(), f.body);
            return;
        }
        if (MegaCombat.heal(f.body)) Smash.sendHud(session);
    }

    void useTera(Smash.Session session) {
        if (frontier != null || teamGame != null) return;
        if (cinema != null) return;
        if (!canUseTera(session)) return;
        if (!GimmickCinematics.start(session, GimmickCinematics.Kind.TERASTALIZATION, () -> finishTera(session))) finishTera(session);
    }

    private boolean canUseTera(Smash.Session session) {
        var f = fighter(session.player);
        return group(session.player) == this && f != null && !f.choosing && f.teraSlot == null && f.body == session.pokemon
                && trainerResultAt == 0 && (fighting || teraLesson()) && TeraCombat.canApply(session);
    }

    private void finishTera(Smash.Session session) {
        if (!canUseTera(session) || !TeraCombat.apply(session, true)) return;
        var f = fighter(session.player);
        f.teraSlot = f.current();
        f.recordDeployment();
        PacketDistributor.sendToPlayer(f.player, new ArenaPackets.TeraUsed(true));
        if (teraLesson() && !fighting) {
            lessonBattleAt = getLevel().getServer().getTickCount() + 26;
            ProloguePackets.show(f.player, Cue.TERA_ITEM, f.body.getId(), false, 0, true);
        }
    }

    private void tickHeld(Fighter f) {
        if (f.megaReady == 0 || getLevel().getGameTime() < f.megaReady) return;
        f.megaReady = 0;
        finishHeld(f, Smash.session(f.player));
    }

    private void finishHeld(Fighter f, Smash.Session session) {
        if (session == null || session.pokemon != f.body || f.current().isFainted()) return;
        if (!MegaCombat.evolve(f.body)) {
            // A failed form transition must not leave the realtime combat state locked.
            session.combat.lockedUntil = 0L;
            session.combat.lockReason = null;
            Smash.sendHud(session);
            return;
        }
        f.megaUsed = true;
        f.recordDeployment();
        // Mega Showdown updates the Pokémon/form asynchronously. Clear the temporary cinematic
        // lock after the committed form is visible so the first post-Mega move is accepted.
        session.combat.lockedUntil = 0L;
        session.combat.lockReason = null;
        Smash.refreshBody(session);
        Smash.sendHud(session);
        if (gimmickLesson() && !teraLesson() && !fighting && lessonBattleAt == 0) {
            lessonBattleAt = getLevel().getServer().getTickCount() + 26;
            ProloguePackets.show(f.player, Cue.MEGA_ITEM, f.body.getId(), false, 0, true);
        }
    }

    /** Everyone actively fighting in this instance shares one pause, including opposing players. */
    void cinematicAudience(Set<Entity> entities, Set<ServerPlayer> viewers) {
        for (var f : knockoutFighters()) {
            if (gone(f) || f.eliminated) continue;
            viewers.add(f.player);
            entities.add(f.player);
            if (f.body != null) entities.add(f.body);
            var session = Smash.session(f.player);
            if (session != null) entities.addAll(session.combat.decoys);
        }
        for (var trainer : trainers) {
            if (trainer.body != null) entities.add(trainer.body);
            if (trainer.npc != null) entities.add(trainer.npc);
        }
        entities.addAll(wildFoes);
        if (cinema != null) entities.addAll(cinema.actors.keySet());
    }

    public static boolean alliedWindup(ServerPlayer viewer, PokemonEntity pokemon) {
        var match = group(viewer);
        if (match == null || group(pokemon) != match) return false;
        if (match.cinema != null) {
            int side = match.cinema.factionOf(viewer);
            return side >= 0 && side == match.cinema.factionOf(pokemon);
        }
        var caster = match.combatant(pokemon);
        var listener = match.fighter(viewer);
        if (caster == null || listener == null) return false;
        if (match.teamGame != null) return match.side(caster) == match.side(listener);
        return match.b == null;
    }

    public static boolean friendlyDamage(Smash.Session victim, DamageSource damage) {
        var match = group(victim.player);
        if (match == null || match.leader == null && match.teamGame == null) return false;
        var source = damage.getEntity();
        var attacker = match.combatant(source != null ? source : damage.getDirectEntity());
        var defender = match.fighter(victim.player);
        return !match.fighting || attacker != null && defender != null && attacker != defender
                && (match.teamGame == null || match.side(attacker) == match.side(defender));
    }

    public static Boolean canHit(Smash.Session attacker, LivingEntity target) {
        ArenaMatch match = group(attacker.player);
        if (match == null) return null;
        if (!match.fighting || match.trainerResultAt != 0 || attacker.player.server.getTickCount() < match.knockoutUntil
                || target == attacker.player || target == attacker.pokemon || !target.isAlive() || target.isSpectator()
                || attacker.combat.decoys.contains(target) || group(target) != match) return false;
        if (match.cinema != null) {
            return match.cinema.opponents(attacker.player, target);
        }
        if (match.teamGame != null) {
            var source = match.fighter(attacker.player);
            var other = match.combatant(target);
            return source != null && other != null && match.side(source) != match.side(other)
                    && Smash.active(other.player);
        }
        if (match.b != null) {
            var opponent = attacker.player == match.a.player ? match.b : match.a;
            var defending = Smash.session(opponent.player);
            return defending != null && (target == opponent.player || target == defending.pokemon
                    || defending.combat.decoys.contains(target));
        }
        if (target instanceof PokemonEntity mon && match.wildFoes.contains(mon)) return !WildCapture.stunned(mon);
        return target instanceof ServerPlayer p ? match.isFriendlyFireEnabled() && Smash.active(p) : match.trainerOf(target) != null
                || match.isFriendlyFireEnabled() && Decoys.isDecoy(target);
    }

    public static boolean retainClientControl(ServerPlayer player) {
        ArenaMatch match = group(player);
        Fighter f = match == null ? null : match.fighter(player);
        return f != null && !match.capturePhase && f.current() != null && f.current().isFainted() && match.canReplace(f);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protectUnsafeDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || !(InstanceApi.find(level).orElse(null) instanceof ArenaMatch arena)) return;
        if (!arena.fighting || arena.trainerResultAt != 0 || victim instanceof PokemonEntity body && RealtimeKnockout.inFlight(body)
                || level.getServer().getTickCount() < arena.knockoutUntil) { event.setCanceled(true); return; }
        if (arena.cinema != null) {
            if (!arena.cinema.canDamage(event)) event.setCanceled(true);
            return;
        }
        var source = event.getSource().getEntity();
        var by = group(source != null ? source : event.getSource().getDirectEntity());
        if (arena.leader != null || arena.teamGame != null) {
            var attacker = arena.combatant(source != null ? source : event.getSource().getDirectEntity());
            var defender = arena.combatant(victim);
            if (attacker != null && defender != null && attacker != defender && (arena.leader != null || arena.side(attacker) == arena.side(defender))) {
                event.setCanceled(true);
                return;
            }
        }
        if (victim instanceof ServerPlayer p && (!Smash.active(p) || by == null || group(p) != by)) event.setCanceled(true);
        if (victim instanceof PokemonEntity pokemon && arena.wildFoes.contains(pokemon)
                && (by != arena && !event.getSource().is(DamageTypes.MAGIC) || arena.capturePhase)) event.setCanceled(true);
        if (victim instanceof PokemonEntity pokemon && pokemon.getPokemon().isNPCOwned()
                && (group(pokemon) == null || by == null && !event.getSource().is(DamageTypes.MAGIC))) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void applyArenaGuard(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof PokemonEntity foe)) return;
        var source = event.getSource().getEntity();
        if (source == null) return;
        var attacker = source instanceof ServerPlayer player ? Smash.session(player) : Smash.sessionOf(source);
        if (attacker == null || group(attacker.player) != group(foe) || !ArenaDefense.guards(foe, attacker)) return;
        event.setAmount(event.getAmount() * .4f);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void retainBodyForRecall(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof PokemonEntity pokemon)) return;
        // NeoForge applies absorption after Pre; only damage that reaches HP can cause a KO.
        float hpDamage = event.getNewDamage() - pokemon.getAbsorptionAmount();
        if (hpDamage <= 0) return;
        float remaining = pokemon.getHealth() - hpDamage;
        // Cobblemon truncates an owned Pokémon's scaled HP to an integer after hurt().
        // This can faint a body that still has a fractional amount of entity health.
        if (remaining > 0 && (pokemon.getOwnerUUID() == null
                || (int) (pokemon.getPokemon().getMaxHealth() * (remaining / pokemon.getMaxHealth())) > 0)) return;
        var arena = group(pokemon);
        if (remaining > 0 && arena == null) return;
        var source = event.getSource().getEntity() != null ? event.getSource().getEntity() : event.getSource().getDirectEntity();
        if (arena == null) {
            boolean realtimeHit = source instanceof ServerPlayer player ? Smash.active(player)
                    : source != null && Smash.sessionOf(source) != null;
            if (realtimeHit && !PrologueTraining.target(pokemon)) RealtimeKnockout.launch(pokemon, source);
            return;
        }
        var foe = arena.trainerOf(pokemon);
        if (foe == null && (arena.cinema == null || !arena.cinema.actors.containsKey(pokemon))) {
            if (arena.wildFoes.contains(pokemon)) RealtimeKnockout.launch(pokemon, source);
            return;
        }
        pokemon.setNoAi(true);
        pokemon.getNavigation().stop();
        RealtimeKnockout.launch(pokemon, source);
        // Keep the body identifiable as this match's trainer until launch has dispatched the cue.
        if (foe != null) foe.knockedOut = true;
        // Vanilla would run the death animation and remove the body. Preserve it just long enough for recall.
        event.setNewDamage(Math.min(event.getNewDamage(), Math.max(0, pokemon.getHealth() - 0.01F) + pokemon.getAbsorptionAmount()));
    }

    @SubscribeEvent
    public static void preventNormalAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && inArena(p) != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void preventNormalUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        var match = inArena(p);
        if (match == null) return;
        var fighter = match.fighter(p);
        if (fighter == null || !(match.capturePhase || fighter.catching) || !(event.getItemStack().getItem() instanceof PokeBallItem))
            event.setCanceled(true);
    }
}
