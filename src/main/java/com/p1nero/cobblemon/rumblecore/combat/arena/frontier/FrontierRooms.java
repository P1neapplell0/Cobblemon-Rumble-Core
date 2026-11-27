package com.p1nero.cobblemon.rumblecore.combat.arena.frontier;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.SmashConfig;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.client.combat.FrontierScreen;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.phone.PhoneApp;
import com.p1nero.cobblemon.rumblecore.phone.PhoneState;
import com.p1nero.cobblemon.rumblecore.prologue.MegaSimulator;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.misc.custom_instances.instance.InstanceSession;
import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The server owns room membership, editable drafts, readiness and the stake escrow. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class FrontierRooms {
    private static final int MAX_ROOMS = 64, LIFETIME = 20 * 60 * 20;
    private static final ResourceLocation PAYOUTS = PrologueWorld.id("frontier_payouts");
    private static final ResourceLocation DEPOSITS = PrologueWorld.id("frontier_deposits");
    private static final Map<UUID, Room> ROOMS = new LinkedHashMap<>();
    private static final Map<UUID, Room> MEMBERS = new LinkedHashMap<>();
    private record LiveRoom(Room room, ArenaMatch match) {
        boolean active() { return match.getState() == InstanceSession.State.ACTIVE; }
    }
    private static final Map<UUID, LiveRoom> LIVE = new LinkedHashMap<>();
    private static final Map<UUID, LiveRoom> WATCH_SEATS = new HashMap<>();
    private record InviteKey(UUID sender, UUID recipient) {}
    private static final Map<InviteKey, Integer> COOLDOWNS = new HashMap<>();

    private FrontierRooms() {}

    private static final class Seat {
        final UUID id;
        final String name;
        final boolean bot;
        final FrontierRoster.Pick[] picks = {FrontierRoster.EMPTY, FrontierRoster.EMPTY, FrontierRoster.EMPTY};
        boolean ready, away;
        int side, locked;
        FrontierRoster.Pick pendingPick = FrontierRoster.EMPTY;

        Seat(ServerPlayer player) {
            id = player.getUUID();
            name = player.getGameProfile().getName();
            bot = false;
        }

        Seat(int side) {
            id = UUID.randomUUID();
            name = FrontierBotDraft.name();
            bot = true;
            this.side = side;
        }

        List<FrontierRoster.Pick> draft() { return List.copyOf(Arrays.asList(picks)); }
        List<Pokemon> forMatch() { return FrontierRoster.create(draft()); }
    }

    private static final class Room {
        final UUID id = UUID.randomUUID();
        final Seat host;
        final BigInteger stake;
        final Map<UUID, Seat> seats = new LinkedHashMap<>();
        final Map<UUID, Seat> spectators = new LinkedHashMap<>();
        final Map<UUID, Integer> invites = new HashMap<>();
        final int capacity;
        final boolean multiplayer;
        int expires;
        final int minutes;
        boolean starting;
        boolean settled;

        Room(ServerPlayer player, Seat host, BigInteger stake, boolean multiplayer, int minutes) {
            this.host = host;
            seats.put(host.id, host);
            this.stake = stake;
            this.capacity = multiplayer ? 0 : 2;
            this.minutes = minutes;
            this.multiplayer = multiplayer;
            touch(player);
        }

        boolean multi() { return multiplayer; }
        Seat guest() { return seats.values().stream().skip(1).findFirst().orElse(null); }
        boolean open(int now) { return !starting && expires > now; }
        boolean full() { return capacity > 0 && seats.size() >= capacity; }
        List<UUID> members() {
            var ids = new ArrayList<>(seats.keySet());
            ids.addAll(spectators.keySet());
            return ids;
        }
        void touch(ServerPlayer player) { expires = player.server.getTickCount() + LIFETIME; }
    }

    private static void recordDeposits(MinecraftServer server, Room room) {
        var storage = server.getCommandStorage();
        var all = storage.get(DEPOSITS).copy();
        if (room.settled || room.stake.signum() == 0) all.remove(room.id.toString());
        else {
            var deposits = new CompoundTag();
            for (var id : room.seats.keySet()) deposits.putString(id.toString(), room.stake.toString());
            all.put(room.id.toString(), deposits);
        }
        storage.set(DEPOSITS, all);
    }

    @SubscribeEvent
    public static void recover(ServerStartedEvent event) {
        var server = event.getServer();
        var saved = server.getCommandStorage().get(DEPOSITS);
        for (var room : saved.getAllKeys()) {
            var deposits = saved.getCompound(room);
            for (var id : deposits.getAllKeys()) credit(server, UUID.fromString(id), new BigInteger(deposits.getString(id)));
        }
        if (!saved.isEmpty()) server.getCommandStorage().set(DEPOSITS, new CompoundTag());
    }

    private static BigInteger balance(ServerPlayer player) { return PlayerExtensionKt.getCobbleDollars(player); }

    private static boolean debit(ServerPlayer player, BigInteger amount) {
        var current = balance(player);
        if (amount.compareTo(current) > 0) return false;
        if (amount.signum() != 0) PlayerExtensionKt.setCobbleDollars(player, current.subtract(amount));
        return true;
    }

    private static void credit(MinecraftServer server, UUID id, BigInteger amount) {
        if (amount.signum() == 0) return;
        var player = server.getPlayerList().getPlayer(id);
        if (player != null) {
            PlayerExtensionKt.setCobbleDollars(player, balance(player).add(amount));
        } else {
            var storage = server.getCommandStorage();
            var payouts = storage.get(PAYOUTS).copy();
            var key = id.toString();
            var pending = payouts.contains(key, Tag.TAG_STRING) ? new BigInteger(payouts.getString(key)) : BigInteger.ZERO;
            payouts.putString(key, pending.add(amount).toString());
            storage.set(PAYOUTS, payouts);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        resumeAfterInterruption(player);
        var storage = player.server.getCommandStorage();
        var payouts = storage.get(PAYOUTS).copy();
        var key = player.getUUID().toString();
        if (!payouts.contains(key, Tag.TAG_STRING)) return;
        PlayerExtensionKt.setCobbleDollars(player, balance(player).add(new BigInteger(payouts.getString(key))));
        payouts.remove(key);
        storage.set(PAYOUTS, payouts);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        WATCH_SEATS.remove(player.getUUID());
        var room = MEMBERS.get(player.getUUID());
        if (room == null || room.starting) return;
        var seat = room.seats.get(player.getUUID());
        if (seat == null) seat = room.spectators.get(player.getUUID());
        if (seat != null) seat.away = true;
        broadcast(player.server, room, false, player.getUUID());
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) resumeAfterInterruption(player);
    }

    private static void resumeAfterInterruption(ServerPlayer player) {
        var room = MEMBERS.get(player.getUUID());
        if (room == null || room.starting) return;
        var seat = room.seats.get(player.getUUID());
        if (seat == null) seat = room.spectators.get(player.getUUID());
        if (seat != null) seat.away = false;
        // The respawn/login packets must precede the HUD state (the client replaces its player on respawn).
        player.server.tell(new TickTask(player.server.getTickCount(), () -> {
            if (player.hasDisconnected() || MEMBERS.get(player.getUUID()) != room) return;
            send(player, false);
            broadcast(player.server, room, false, player.getUUID());
        }));
    }

    @SubscribeEvent
    public static void updateWaitingRooms(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var room = MEMBERS.get(player.getUUID());
        if (room == null || room.starting) return;
        var seat = room.seats.get(player.getUUID());
        if (seat == null) return;
        boolean unavailable = !ArenaMatch.available(player);
        if (seat.away == unavailable) return;
        seat.away = unavailable;
        broadcast(player.server, room, false, player.getUUID());
    }

    @SubscribeEvent
    public static void stop(ServerStoppingEvent event) {
        for (var room : List.copyOf(ROOMS.values())) if (!room.starting) cancel(event.getServer(), room);
        MEMBERS.clear();
        ROOMS.clear();
        LIVE.clear();
        WATCH_SEATS.clear();
        COOLDOWNS.clear();
    }

    private static boolean hasAccess(ServerPlayer player) {
        return MegaSimulator.atTerminal(player) || PhoneState.hasPhone(player);
    }

    private static boolean eligible(ServerPlayer player) {
        return hasAccess(player) && ArenaMatch.available(player)
                && SmashConfig.<Boolean>get(SmashConfig.ENABLED);
    }

    public static final int PRESET_LIMIT = 10;
    public static final int TEAM_BOT_LIMIT = 8;

    static boolean inWaitingRoom(UUID player) { return MEMBERS.containsKey(player); }

    public static void open(ServerPlayer player) {
        if (!ArenaMatch.available(player) || !MEMBERS.containsKey(player.getUUID()) && !eligible(player)) { problem(player, "unavailable"); return; }
        send(player, true);
    }

    private static void problem(ServerPlayer player, String key) {
        if (!"repeat_species".equals(key) && !"mega_limit".equals(key) && !"bot_stake".equals(key) && !"bot_limit".equals(key))
            player.sendSystemMessage(Component.translatable("rumble.frontier.error." + key));
        send(player, false, key);
    }

    private static void handle(ServerPlayer player, CompoundTag command) {
        String action = command.getString("action");
        if ("open_phone".equals(action)) {
            if (PhoneState.allow(player, PhoneApp.FRONTIER)) open(player);
            return;
        }
        Room room = MEMBERS.get(player.getUUID());
        var watchSeat = WATCH_SEATS.get(player.getUUID());
        if ("resume".equals(action)) {
            if ((room != null || watchSeat != null) && ArenaMatch.available(player)) send(player, true);
            else if (room == null) send(player, false);
            return;
        }
        if ("leave".equals(action)) {
            if (watchSeat != null) { WATCH_SEATS.remove(player.getUUID()); send(player, false); }
            else leave(player);
            return;
        }
        if ("save_presets".equals(action) || "import_presets".equals(action)) {
            // Only edit the sender's private presets. Keep legacy picks even if the current
            // learnset changed; applying one in a room still runs the full team validator.
            if (command.get("presets") instanceof ListTag rows) {
                var picks = decodePresets(rows);
                if (picks != null) FrontierPresetData.get(player.server).update(player.getUUID(),
                        picks.stream().map(FrontierRoster::withoutUnsupportedHeld).toList(), "import_presets".equals(action));
            }
            return;
        }
        if (room == null ? !eligible(player) : !ArenaMatch.available(player)) { problem(player, "unavailable"); return; }
        if (watchSeat != null && !"list".equals(action)) {
            if (!"confirm_watch".equals(action) || !command.hasUUID("room")
                    || !watchSeat.room().id.equals(command.getUUID("room"))) { send(player, false); return; }
            if (!watchSeat.active()) {
                WATCH_SEATS.remove(player.getUUID());
                problem(player, "closed");
                return;
            }
            FrontierSpectators.enter(watchSeat.match(), List.of(player.getUUID()));
            if (FrontierSpectators.isWatching(player, watchSeat.match())) {
                WATCH_SEATS.remove(player.getUUID());
                close(player);
            } else problem(player, "spectator_entry");
            return;
        }
        if (room != null && room.starting) return;
        if (room != null && !room.open(player.server.getTickCount())) {
            cancel(player.server, room);
            notifyClosed(player.server, room, null, "expired");
            return;
        }
        if ("learnset".equals(action)) {
            var id = ResourceLocation.tryParse(command.getString("species"));
            var species = id == null ? null : FrontierRoster.species(id);
            if (species == null) return;
            var result = new CompoundTag();
            var moves = new ListTag();
            FrontierRoster.legalMoves(species).stream().map(move -> move.getName()).sorted()
                    .map(StringTag::valueOf).forEach(moves::add);
            result.putString("learnset", id.toString());
            result.put("moves", moves);
            result.put("initialPick", encode(FrontierRoster.initialPick(species)));
            PacketDistributor.sendToPlayer(player, new View(result));
            return;
        }
        if ("list".equals(action)) { send(player, false); return; }
        if ("create".equals(action)) {
            var raw = command.getString("stake");
            if (room != null || ROOMS.size() >= MAX_ROOMS) { problem(player, "unavailable"); return; }
            if (!raw.matches("[0-9]{1,18}")) { problem(player, "stake"); return; }
            var stake = new BigInteger(raw);
            boolean multi = command.getBoolean("multi");
            boolean practice = !multi && command.getBoolean("practice");
            if (practice && stake.signum() != 0) { problem(player, "bot_stake"); return; }
            int minutes = multi ? (command.contains("minutes", Tag.TAG_INT) ? command.getInt("minutes") : FrontierRules.DEFAULT_TIMED_MINUTES) : 0;
            if (!FrontierRules.isValidTeamDuration(minutes)) { problem(player, "minutes"); return; }
            var host = new Seat(player);
            if (!debit(player, stake)) { problem(player, "funds"); return; }
            room = new Room(player, host, stake, multi, minutes);
            if (practice) {
                var bot = new Seat(1);
                room.seats.put(bot.id, bot);
            }
            ROOMS.put(room.id, room);
            MEMBERS.put(player.getUUID(), room);
            recordDeposits(player.server, room);
            send(player, true);
            return;
        }
        if ("join".equals(action)) {
            if (room != null || !command.hasUUID("room")) return;
            UUID id = command.getUUID("room");
            var live = LIVE.get(id);
            if (live != null && live.active()) {
                WATCH_SEATS.put(player.getUUID(), live);
                send(player, true);
            } else join(player, ROOMS.get(id), false);
            return;
        }
        if (room == null || !command.hasUUID("room") || !room.id.equals(command.getUUID("room"))) { send(player, false); return; }
        var seat = room.seats.get(player.getUUID());
        boolean owner = room.host.id.equals(player.getUUID());
        if ("invite".equals(action) && (seat != null || owner)) { invite(player, room, command); return; }
        if (room.multi() && "side".equals(action)) {
            if (!command.contains("side", Tag.TAG_INT)) { send(player, false); return; }
            int side = command.getInt("side");
            if (side < -1 || side > 1 || !changeSide(player, room, side)) return;
            room.touch(player);
            broadcast(player.server, room);
            return;
        }
        if ("bot_preset".equals(action)) {
            if (!owner || room.stake.signum() != 0 || !command.hasUUID("target")
                    || !command.contains("slot", Tag.TAG_INT)) { send(player, false); return; }
            var bot = room.seats.get(command.getUUID("target"));
            int slot = command.getInt("slot");
            if (bot == null || !bot.bot || slot < 0 || slot >= (room.multi() ? 1 : FrontierRoster.TEAM_SIZE)) {
                send(player, false);
                return;
            }
            if (!applyBotPreset(player, bot, slot, command)) return;
            room.touch(player);
            broadcast(player.server, room);
            return;
        }
        if (room.multi() && owner && ("add_bot".equals(action) || "remove_bot".equals(action))) {
            if (room.stake.signum() != 0) { problem(player, "bot_stake"); return; }
            if ("add_bot".equals(action)) {
                int side = command.getInt("side");
                if (!command.contains("side", Tag.TAG_INT) || side < 0 || side > 1) { send(player, false); return; }
                if (room.seats.values().stream().filter(s -> s.bot).count() >= TEAM_BOT_LIMIT) { problem(player, "bot_limit"); return; }
                var bot = new Seat(side);
                var pick = FrontierBotDraft.pick(bot.draft(), FrontierRoster.EMPTY);
                if (pick == null) { problem(player, "unavailable"); return; }
                bot.picks[0] = pick;
                bot.ready = true;
                room.seats.put(bot.id, bot);
            } else if (command.hasUUID("target")) {
                var bot = room.seats.get(command.getUUID("target"));
                if (bot != null && bot.bot) room.seats.remove(bot.id);
            }
            room.touch(player);
            broadcast(player.server, room);
            return;
        }
        if (seat == null && !(room.multi() && owner && ("start".equals(action) || "shuffle".equals(action)))) {
            send(player, false);
            return;
        }
        if (room.multi()) {
            switch (action) {
                case "shuffle" -> {
                    if (!owner || room.seats.size() < 2) { send(player, false); return; }
                    var fighters = new ArrayList<>(room.seats.values());
                    Collections.shuffle(fighters);
                    for (int i = 0; i < fighters.size(); i++) {
                        fighters.get(i).side = i % 2;
                        fighters.get(i).ready = fighters.get(i).bot;
                    }
                }
                case "preset" -> { if (!selectPreset(player, seat, command, 0)) return; seat.picks[0] = seat.pendingPick; }
                case "ready" -> {
                    if (seat.picks[0].equals(FrontierRoster.EMPTY)) { problem(player, "choose_preset"); return; }
                    seat.ready = !seat.ready;
                }
                case "start" -> {
                    if (!owner || !teamsReady(player.server, room) || !canFight(player.server, room)) {
                        send(player, false); return;
                    }
                    var entries = new LinkedHashMap<UUID, ArenaMatch.TeamEntry>();
                    try {
                        for (var other : room.seats.values()) entries.put(other.id, new ArenaMatch.TeamEntry(other.side, FrontierRoster.create(other.draft()), other.bot ? other.name : null));
                    } catch (RuntimeException failure) { problem(player, "unavailable"); return; }
                    room.starting = true;
                    Room active = room;
                    var server = player.server;
                    var leader = seat != null ? player : room.seats.values().stream().filter(s -> !s.bot)
                            .map(s -> server.getPlayerList().getPlayer(s.id)).filter(Objects::nonNull).findFirst().orElse(player);
                    if (ArenaMatch.startFrontierTeams(leader, entries, room.minutes, List.copyOf(room.spectators.keySet()),
                            winner -> settleTeams(server, active, winner))) registerLive(room, leader);
                    else settleTeams(server, room, -1);
                    closeRoom(server, room);
                    return;
                }
                default -> { send(player, false); return; }
            }
            room.touch(player);
            broadcast(player.server, room);
            return;
        }
        switch (action) {
            case "preset", "confirm" -> {
                var opponent = seat == room.host ? room.guest() : room.host;
                if (opponent == null || !draftTurn(room, seat)) { problem(player, "draft_wait"); return; }
                if ("preset".equals(action)) {
                    if (!selectPreset(player, seat, command, seat.locked)) return;
                } else {
                    if (seat.pendingPick.equals(FrontierRoster.EMPTY)) { problem(player, "choose_preset"); return; }
                    FrontierRoster.Pick counter = null;
                    if (opponent.bot) {
                        counter = opponent.picks[opponent.locked];
                        if (counter.equals(FrontierRoster.EMPTY)) counter = FrontierBotDraft.pick(opponent.draft(), seat.pendingPick);
                    }
                    if (opponent.bot && counter == null) { problem(player, "unavailable"); return; }
                    seat.picks[seat.locked++] = seat.pendingPick;
                    if (opponent.bot) {
                        opponent.picks[opponent.locked++] = counter;
                        opponent.ready = opponent.locked == FrontierRoster.TEAM_SIZE;
                    }
                    seat.pendingPick = FrontierRoster.EMPTY;
                    seat.ready = seat.locked == FrontierRoster.TEAM_SIZE;
                }
            }
            case "start" -> {
                var opponent = room.guest();
                if (seat != room.host || opponent == null || !room.host.ready || !opponent.ready) { send(player, false); return; }
                var guest = player.server.getPlayerList().getPlayer(opponent.id);
                if (!availableInRoom(player.server, opponent) || !canFight(player.server, room)) { problem(player, "player_away"); return; }
                if (FrontierRoster.validate(room.host.draft(), true) != FrontierRoster.Issue.NONE
                        || FrontierRoster.validate(opponent.draft(), true) != FrontierRoster.Issue.NONE) {
                    room.host.ready = opponent.ready = false;
                    broadcast(player.server, room);
                    problem(player, "unavailable");
                    return;
                }
                Room duel = room;
                var server = player.server;
                Consumer<UUID> escrow = winner -> settleDuel(server, duel, winner);
                room.starting = true;
                if (opponent.bot) {
                    try {
                        if (ArenaMatch.startFrontierTeams(player, Map.of(
                                seat.id, new ArenaMatch.TeamEntry(0, seat.forMatch(), null),
                                opponent.id, new ArenaMatch.TeamEntry(1, opponent.forMatch(), opponent.name)),
                                0, List.copyOf(room.spectators.keySet()), winner -> escrow.accept(winner == 0 ? seat.id : null), true)) registerLive(room, player);
                        else escrow.accept(null);
                    } catch (RuntimeException failure) { escrow.accept(null); }
                } else if (ArenaMatch.startFrontier(player, guest, seat::forMatch,
                        opponent::forMatch, List.copyOf(room.spectators.keySet()), escrow, () -> escrow.accept(null))) registerLive(room, player);
                else escrow.accept(null);
                closeRoom(server, room);
                return;
            }
            default -> { send(player, false); return; }
        }
        room.touch(player);
        broadcast(player.server, room);
    }

    private static boolean teamsReady(MinecraftServer server, Room room) {
        return room.seats.values().stream().map(s -> s.side).distinct().count() == 2
                && room.seats.values().stream().allMatch(s -> s.ready && availableInRoom(server, s));
    }

    private static boolean canFight(MinecraftServer server, Room room) {
        return server.isPvpAllowed() || room.seats.values().stream().filter(s -> !s.bot).count() <= 1;
    }

    private static boolean availableInRoom(MinecraftServer server, Seat seat) {
        if (seat.bot) return true;
        var player = server.getPlayerList().getPlayer(seat.id);
        return !seat.away && player != null && !player.hasDisconnected() && ArenaMatch.available(player);
    }

    private static boolean changeSide(ServerPlayer player, Room room, int side) {
        var id = player.getUUID();
        var seat = room.seats.get(id);
        if (side == -1 && seat != null) {
            room.seats.remove(id);
            room.spectators.put(id, seat);
            credit(player.server, id, room.stake);
        } else if (side >= 0 && seat == null) {
            seat = room.spectators.get(id);
            if (seat == null) { send(player, false); return false; }
            if (!debit(player, room.stake)) { problem(player, "funds"); return false; }
            room.spectators.remove(id);
            room.seats.put(id, seat);
        }
        if (seat != null) {
            seat.side = side;
            seat.ready = false;
        }
        recordDeposits(player.server, room);
        return true;
    }

    private static boolean draftTurn(Room room, Seat seat) {
        var guest = room.guest();
        return guest != null && FrontierRules.draftTurn(seat == room.host, room.host.locked, guest.locked);
    }

    private static boolean selectPreset(ServerPlayer player, Seat seat, CompoundTag command, int slot) {
        var pick = decode(command.getCompound("pick"));
        if (seat.ready || slot < 0 || slot >= FrontierRoster.TEAM_SIZE || pick == null || pick.equals(FrontierRoster.EMPTY)) {
            problem(player, "choose_preset"); return false;
        }
        var next = new ArrayList<>(seat.draft());
        next.set(slot, pick);
        if (!validate(player, next, true)) return false;
        seat.pendingPick = pick;
        return true;
    }

    private static boolean applyBotPreset(ServerPlayer player, Seat bot, int slot, CompoundTag command) {
        var saved = FrontierPresetData.get(player.server).get(player.getUUID());
        var selected = decode(command.getCompound("pick"));
        if (saved == null || selected == null || selected.equals(FrontierRoster.EMPTY) || saved.stream()
                .map(FrontierRoster::withoutUnsupportedHeld).noneMatch(selected::equals)) {
            problem(player, "choose_preset");
            return false;
        }
        var next = new ArrayList<>(bot.draft());
        next.set(slot, selected);
        if (!validate(player, next, true)) return false;
        // Future slots reserve their species for auto-drafting, but only confirm advances locked.
        bot.picks[slot] = selected;
        return true;
    }

    private static void settleDuel(MinecraftServer server, Room room, UUID winner) {
        if (room.settled) return;
        room.settled = true;
        if (room.seats.containsKey(winner)) credit(server, winner, room.stake.multiply(BigInteger.TWO));
        else room.seats.keySet().forEach(id -> credit(server, id, room.stake));
        recordDeposits(server, room);
    }

    private static void settleTeams(MinecraftServer server, Room room, int winner) {
        if (room.settled) return;
        var winners = room.seats.values().stream().filter(s -> s.side == winner).toList();
        room.settled = true;
        if (winners.isEmpty()) room.seats.keySet().forEach(id -> credit(server, id, room.stake));
        else {
            var split = FrontierRules.teamPayout(room.stake, room.seats.size(), winners.size());
            for (int i = 0; i < winners.size(); i++) credit(server, winners.get(i).id, split.get(i));
        }
        recordDeposits(server, room);
    }

    private static boolean validate(ServerPlayer player, List<FrontierRoster.Pick> draft, boolean ready) {
        var issue = FrontierRoster.validate(draft, ready);
        if (issue == FrontierRoster.Issue.NONE) return true;
        problem(player, issue.name().toLowerCase(Locale.ROOT));
        return false;
    }

    private static boolean join(ServerPlayer player, Room room, boolean invited) {
        int now = player.server.getTickCount();
        if (!eligible(player) || MEMBERS.containsKey(player.getUUID()) || WATCH_SEATS.containsKey(player.getUUID()) || room == null || !room.open(now)
                || invited && room.invites.getOrDefault(player.getUUID(), 0) <= now) {
            problem(player, "closed"); return false;
        }
        var seat = new Seat(player);
        if (room.full()) {
            seat.side = -1;
            room.spectators.put(seat.id, seat);
        } else {
            if (!debit(player, room.stake)) { problem(player, "funds"); return false; }
            room.seats.put(seat.id, seat);
            if (!room.multi()) room.host.ready = false;
        }
        if (room.multi()) {
            long left = room.seats.values().stream().filter(s -> s != seat && s.side == 0).count();
            seat.side = left > room.seats.size() - 1 - left ? 1 : 0;
        }
        room.invites.remove(seat.id);
        room.touch(player);
        MEMBERS.put(player.getUUID(), room);
        recordDeposits(player.server, room);
        broadcast(player.server, room, false, player.getUUID());
        send(player, true);
        return true;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rumble_frontier_accept")
                .then(Commands.argument("room", UuidArgument.uuid()).executes(ctx ->
                        join(ctx.getSource().getPlayerOrException(), ROOMS.get(UuidArgument.getUuid(ctx, "room")), true) ? 1 : 0)));
    }

    private static void invite(ServerPlayer player, Room room, CompoundTag command) {
        int now = player.server.getTickCount();
        if (!room.open(now) || !command.hasUUID("target")) { problem(player, "closed"); return; }
        var id = command.getUUID("target");
        var target = player.server.getPlayerList().getPlayer(id);
        var key = new InviteKey(player.getUUID(), id);
        if (room.invites.containsKey(id) || COOLDOWNS.getOrDefault(key, 0) > now
                || target == null || MEMBERS.containsKey(id) || !eligible(target)) { send(player, false); return; }
        room.invites.put(id, now + 1200);
        COOLDOWNS.put(key, now + 1200);
        target.sendSystemMessage(Component.translatable(room.full() ? "rumble.frontier.invited_watch" : "rumble.frontier.invited_chat", player.getDisplayName(), room.stake)
                .append(" ").append(Component.translatable("rumble.frontier.accept_invite").withStyle(style -> style
                        .withColor(ChatFormatting.GOLD).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rumble_frontier_accept " + room.id)))));
        broadcast(player.server, room);
    }

    @SubscribeEvent
    public static void pruneInviteCooldowns(ServerTickEvent.Post event) {
        int now = event.getServer().getTickCount();
        if (now % 100 != 0) return;
        COOLDOWNS.values().removeIf(expiry -> expiry <= now);
        var server = event.getServer();
        for (var live : List.copyOf(LIVE.values())) {
            var state = live.match().getState();
            if (state == InstanceSession.State.ENDING || state == InstanceSession.State.CLOSED) endLive(server, live.match());
        }
        for (var room : List.copyOf(ROOMS.values())) {
            room.invites.values().removeIf(expiry -> expiry <= now);
            if (!room.starting && !room.open(now)) {
                cancel(server, room);
                notifyClosed(server, room, null, "expired");
            }
        }
    }

    private static void leave(ServerPlayer player) {
        Room room = MEMBERS.get(player.getUUID());
        if (room == null || room.starting) {
            send(player, false);
            return;
        }
        var id = player.getUUID();
        if (room.host.id.equals(id)) {
            cancel(player.server, room);
            notifyClosed(player.server, room, id, "closed");
        } else {
            MEMBERS.remove(id, room);
            if (room.seats.remove(id) != null) {
                if (!room.multi()) {
                    room.host.ready = false;
                    Arrays.fill(room.host.picks, FrontierRoster.EMPTY);
                    room.host.locked = 0;
                    room.host.pendingPick = FrontierRoster.EMPTY;
                }
                credit(player.server, id, room.stake);
                recordDeposits(player.server, room);
            } else room.spectators.remove(id);
            broadcast(player.server, room);
        }
        if (!player.hasDisconnected()) send(player, false);
    }

    private static void notifyClosed(MinecraftServer server, Room room, UUID except, String reason) {
        for (var id : room.members()) {
            var player = server.getPlayerList().getPlayer(id);
            if (!id.equals(except) && player != null && !player.hasDisconnected()) problem(player, reason);
        }
    }

    private static void closeRoom(MinecraftServer server, Room room) {
        remove(room);
        for (var id : room.members()) {
            var player = server.getPlayerList().getPlayer(id);
            if (player != null && !player.hasDisconnected()) close(player);
        }
    }

    private static void registerLive(Room room, ServerPlayer leader) {
        var match = ArenaMatch.inArena(leader);
        if (match != null) LIVE.put(room.id, new LiveRoom(room, match));
    }

    public static void endLive(MinecraftServer server, ArenaMatch match) {
        if (!LIVE.values().removeIf(live -> live.match() == match)) return;
        for (var entry : List.copyOf(WATCH_SEATS.entrySet())) {
            if (entry.getValue().match() != match) continue;
            WATCH_SEATS.remove(entry.getKey());
            var player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null && !player.hasDisconnected()) problem(player, "closed");
        }
    }

    private static void remove(Room room) {
        ROOMS.remove(room.id);
        room.members().forEach(id -> MEMBERS.remove(id, room));
    }

    private static void cancel(MinecraftServer server, Room room) {
        remove(room);
        if (!room.settled) {
            room.seats.keySet().forEach(id -> credit(server, id, room.stake));
            room.settled = true;
            recordDeposits(server, room);
        }
    }

    private static void broadcast(MinecraftServer server, Room room) {
        broadcast(server, room, false, null);
    }

    private static void broadcast(MinecraftServer server, Room room, boolean open, UUID skip) {
        for (var id : room.members()) {
            if (id.equals(skip) || MEMBERS.get(id) != room) continue;
            var p = server.getPlayerList().getPlayer(id);
            if (p != null && !p.hasDisconnected()) send(p, open && ArenaMatch.available(p), "", true);
        }
    }

    private static void close(ServerPlayer player) {
        var data = new CompoundTag();
        data.putBoolean("close", true);
        PacketDistributor.sendToPlayer(player, new View(data));
    }

    private static void send(ServerPlayer player, boolean open) { send(player, open, ""); }

    private static void send(ServerPlayer player, boolean open, String problem) { send(player, open, problem, false); }

    private static void send(ServerPlayer player, boolean open, String problem, boolean automatic) {
        var data = new CompoundTag();
        data.putBoolean("open", open);
        if (open) {
            var picks = FrontierPresetData.get(player.server).get(player.getUUID());
            data.putBoolean("serverPresets", true);
            // Preserve old species and moves; only clear equipment retired from simulator play.
            if (picks != null) data.put("presets", encodePresets(picks.stream().map(FrontierRoster::withoutUnsupportedHeld).toList()));
        }
        data.putBoolean("automatic", automatic);
        data.putString("problem", problem);
        data.putString("balance", balance(player).toString());
        var room = MEMBERS.get(player.getUUID());
        var watchSeat = WATCH_SEATS.get(player.getUUID());
        if (watchSeat != null) {
            var live = watchSeat.room();
            writeListing(data, live);
            data.putBoolean("live", true);
            var observers = new ListTag();
            for (var observer : watchSeat.match().getLevel().players()) {
                if (FrontierSpectators.isWatching(observer, watchSeat.match()) || watchSeat.match().isFrontierObserverHost(observer))
                    observers.add(publicSeat(new Seat(observer)));
            }
            WATCH_SEATS.forEach((id, seat) -> {
                if (seat == watchSeat) {
                    var waiting = player.server.getPlayerList().getPlayer(id);
                    if (waiting != null) observers.add(publicSeat(new Seat(waiting)));
                }
            });
            data.put("spectators", observers);
        } else if (room == null) {
            var available = new ListTag();
            for (var entry : ROOMS.values().stream().sorted(Comparator.comparing(Room::multi)).toList()) {
                if (entry.starting || entry.expires <= player.server.getTickCount()) continue;
                var row = new CompoundTag();
                writeListing(row, entry);
                available.add(row);
            }
            for (var live : LIVE.values()) {
                if (!live.active()) continue;
                var row = new CompoundTag();
                writeListing(row, live.room());
                row.putBoolean("live", true);
                available.add(row);
            }
            data.put("rooms", available);
        } else {
            data.putUUID("room", room.id);
            data.putString("stake", room.stake.toString());
            data.putBoolean("owner", room.host.id.equals(player.getUUID()));
            data.putBoolean("practice", !room.multi() && room.guest() != null && room.guest().bot);
            data.put("host", encode(room.host));
            if (room.guest() != null) {
                var guest = room.guest();
                data.put("guest", encode(guest, guest.bot && !room.host.id.equals(player.getUUID()) ? guest.locked : guest.picks.length));
            }
            data.putInt("capacity", room.capacity);
            data.putBoolean("multi", room.multi());
            data.putInt("count", room.seats.size());
            var seats = new ListTag();
            room.seats.values().forEach(s -> seats.add(publicSeat(s)));
            data.put("seats", seats);
            var spectators = new ListTag();
            room.spectators.values().forEach(s -> spectators.add(publicSeat(s)));
            data.put("spectators", spectators);
            var self = room.seats.get(player.getUUID());
            boolean watching = self == null;
            data.putBoolean("watching", watching);
            boolean invite = room.open(player.server.getTickCount()) && (!watching || player.getUUID().equals(room.host.id));
            data.putBoolean("canInvite", invite);
            if (room.multi() && self != null) data.put("self", encode(self));
            data.putBoolean("selfReady", self != null && self.ready);
            var guest = room.guest();
            data.putBoolean("canStart", canFight(player.server, room) && (room.multi() ? teamsReady(player.server, room)
                    : guest != null && room.host.ready && guest.ready && availableInRoom(player.server, room.host) && availableInRoom(player.server, guest)));
            data.putInt("minutes", room.minutes);
            data.putInt("selfSide", watching ? -1 : self.side);
            boolean turn = !watching && !room.multi() && draftTurn(room, self);
            data.putBoolean("draftTurn", turn);
            data.putBoolean("hostTurn", !room.multi() && draftTurn(room, room.host));
            if (self != null) {
                data.putInt("locked", self.locked);
                data.put("pendingPick", encode(self.pendingPick));
            }
            if (invite) encodeInvites(player, room, data);
        }
        PacketDistributor.sendToPlayer(player, new View(data));
    }

    private static void writeListing(CompoundTag row, Room room) {
        row.putUUID("room", room.id);
        row.put("host", publicSeat(room.host));
        if (room.guest() != null) row.put("guest", publicSeat(room.guest()));
        row.putInt("capacity", room.capacity);
        row.putInt("minutes", room.minutes);
        row.putBoolean("multi", room.multi());
        row.putInt("count", room.seats.size());
        row.putString("stake", room.stake.toString());
    }

    private static void encodeInvites(ServerPlayer player, Room room, CompoundTag data) {
        var candidates = new ListTag();
        int now = player.server.getTickCount();
        for (var other : player.server.getPlayerList().getPlayers()) {
            if (other == player || candidates.size() >= 256) continue;
            var id = other.getUUID();
            boolean sent = room.invites.containsKey(id);
            if (!sent && (MEMBERS.containsKey(id) || !eligible(other))) continue;
            var row = publicSeat(new Seat(other));
            row.putBoolean("sent", sent);
            row.putBoolean("cooldown", COOLDOWNS.getOrDefault(new InviteKey(player.getUUID(), id), 0) > now);
            candidates.add(row);
        }
        data.put("candidates", candidates);
    }

    private static CompoundTag publicSeat(Seat seat) {
        var data = new CompoundTag();
        data.putString("name", seat.name);
        data.putBoolean("bot", seat.bot);
        data.putUUID("player", seat.id);
        data.putBoolean("ready", seat.ready);
        data.putBoolean("away", seat.away);
        data.putInt("side", seat.side);
        data.putInt("locked", seat.locked);
        var species = new ListTag();
        for (var pick : seat.picks) species.add(StringTag.valueOf(pick.species().toString()));
        data.put("species", species);
        return data;
    }

    private static CompoundTag encode(Seat seat) {
        return encode(seat, seat.picks.length);
    }

    private static CompoundTag encode(Seat seat, int visibleSlots) {
        var data = new CompoundTag();
        data.putString("name", seat.name);
        data.putBoolean("bot", seat.bot);
        data.putUUID("player", seat.id);
        data.putBoolean("ready", seat.ready);
        data.putBoolean("away", seat.away);
        data.putInt("side", seat.side);
        data.putInt("locked", seat.locked);
        var picks = new ListTag();
        for (int i = 0; i < seat.picks.length; i++) picks.add(encode(i < visibleSlots ? seat.picks[i] : FrontierRoster.EMPTY));
        data.put("picks", picks);
        return data;
    }

    public static ListTag encodePresets(List<FrontierRoster.Pick> picks) {
        var rows = new ListTag();
        picks.stream().limit(PRESET_LIMIT).map(FrontierRooms::encode).forEach(rows::add);
        return rows;
    }

    public static List<FrontierRoster.Pick> decodePresets(ListTag rows) {
        if (rows.size() > PRESET_LIMIT) return null;
        var picks = new ArrayList<FrontierRoster.Pick>();
        for (var row : rows) {
            if (!(row instanceof CompoundTag tag)) return null;
            var pick = decode(tag);
            if (pick == null || FrontierRoster.NONE.equals(pick.species())) return null;
            picks.add(pick);
        }
        return List.copyOf(picks);
    }

    public static CompoundTag encode(FrontierRoster.Pick pick) {
        var data = new CompoundTag();
        data.putString("species", pick.species().toString());
        data.putString("held", pick.held().toString());
        var moves = new ListTag();
        for (var name : pick.moves()) moves.add(StringTag.valueOf(name));
        data.put("moves", moves);
        return data;
    }

    public static FrontierRoster.Pick decode(CompoundTag data) {
        String speciesName = data.getString("species"), itemName = data.getString("held");
        if (speciesName.length() > 128 || itemName.length() > 128) return null;
        var species = ResourceLocation.tryParse(speciesName);
        var item = ResourceLocation.tryParse(itemName);
        var moves = data.getList("moves", Tag.TAG_STRING);
        if (species == null || item == null || moves.size() != FrontierRoster.MOVE_SLOTS) return null;
        var names = new ArrayList<String>(FrontierRoster.MOVE_SLOTS);
        for (int i = 0; i < moves.size(); i++) {
            var name = moves.getString(i);
            if (name.length() > 64) return null;
            names.add(name);
        }
        return new FrontierRoster.Pick(species, item, names);
    }

    public record Command(CompoundTag data) implements CustomPacketPayload {
        static final Type<Command> TYPE = new Type<>(PrologueWorld.id("frontier_command"));
        static final StreamCodec<RegistryFriendlyByteBuf, Command> CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Command::data, Command::new);
        @Override public Type<Command> type() { return TYPE; }
    }

    public record View(CompoundTag data) implements CustomPacketPayload {
        static final Type<View> TYPE = new Type<>(PrologueWorld.id("frontier_view"));
        static final StreamCodec<RegistryFriendlyByteBuf, View> CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, View::data, View::new);
        @Override public Type<View> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Command.TYPE, Command.CODEC, (packet, context) -> handle((ServerPlayer) context.player(), packet.data()));
        registrar.playToClient(View.TYPE, View.CODEC, (packet, context) -> FrontierScreen.receive(packet.data()));
    }
}
