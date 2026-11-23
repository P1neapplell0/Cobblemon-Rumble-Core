package com.p1nero.cobblemon.rumblecore.combat.arena.frontier;

import com.johnn.realtimecobblemon.SmashConfig;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.client.combat.FrontierScreen;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.phone.PhoneApp;
import com.p1nero.cobblemon.rumblecore.phone.PhoneState;
import com.p1nero.cobblemon.rumblecore.prologue.MegaSimulator;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
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
    private record InviteKey(UUID sender, UUID recipient) {}
    private static final Map<InviteKey, Integer> COOLDOWNS = new HashMap<>();

    private FrontierRooms() {}

    private static final class Seat {
        final UUID id;
        final String name;
        final FrontierRoster.Pick[] picks = {FrontierRoster.EMPTY, FrontierRoster.EMPTY, FrontierRoster.EMPTY};
        final List<Pokemon> personalParty;
        boolean ready;

        Seat(ServerPlayer player) { this(player, false); }

        Seat(ServerPlayer player, boolean ownParty) {
            id = player.getUUID();
            name = player.getGameProfile().getName();
            personalParty = ownParty ? FrontierRoster.snapshotParty(player) : List.of();
            for (int i = 0; i < personalParty.size(); i++) picks[i] = FrontierRoster.describe(personalParty.get(i));
        }

        List<FrontierRoster.Pick> draft() { return List.copyOf(Arrays.asList(picks)); }

        List<Pokemon> forMatch(ServerPlayer player, boolean ownParty) {
            return ownParty ? FrontierRoster.copyForMatch(player, personalParty) : FrontierRoster.create(draft());
        }
    }

    private static final class Room {
        final UUID id = UUID.randomUUID();
        final Seat host;
        final BigInteger stake;
        final Map<UUID, Seat> seats = new LinkedHashMap<>();
        final Map<UUID, Integer> invites = new HashMap<>();
        final HashSet<UUID> withdrawals = new HashSet<>();
        final int capacity;
        final boolean multiplayer, ownParty;
        FrontierBracket bracket;
        Map<UUID, BigInteger> prizes = Map.of();
        int expires;
        boolean starting;
        boolean roundOpened;
        boolean settled;

        Room(ServerPlayer player, Seat host, BigInteger stake, boolean multiplayer, int capacity, boolean ownParty) {
            this.host = host;
            seats.put(host.id, host);
            this.stake = stake;
            this.capacity = capacity;
            this.multiplayer = multiplayer;
            this.ownParty = ownParty;
            touch(player);
        }

        boolean multi() { return multiplayer; }
        Seat guest() { return seats.values().stream().skip(1).findFirst().orElse(null); }
        boolean open(int now) { return bracket == null && !starting && (capacity == 0 || seats.size() < capacity) && expires > now; }
        void touch(ServerPlayer player) { expires = player.server.getTickCount() + LIFETIME; }
    }

    public static final class Escrow implements Consumer<UUID> {
        private final MinecraftServer server;
        private final UUID host, guest;
        private final BigInteger stake;
        private boolean settled;

        private Escrow(MinecraftServer server, Room room) {
            this.server = server;
            host = room.host.id;
            guest = room.guest().id;
            stake = room.stake;
        }

        /** A draw/setup failure refunds both; the winner otherwise receives the full pot. */
        @Override public void accept(UUID winner) {
            if (settled) return;
            settled = true;
            if (host.equals(winner) || guest.equals(winner)) {
                credit(server, winner, stake.add(stake));
            } else {
                credit(server, host, stake);
                credit(server, guest, stake);
            }
        }
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
        if (MEMBERS.containsKey(player.getUUID())) send(player, false);
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
        if (event.getEntity() instanceof ServerPlayer player) {
            var room = MEMBERS.get(player.getUUID());
            if (room == null || room.bracket == null) leave(player, false);
            else {
                var seat = room.seats.get(player.getUUID());
                seat.ready = false;
                broadcast(player.server, room, false, player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void expire(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 100 != 0) return;
        var room = MEMBERS.get(player.getUUID());
        if (room != null && room.bracket == null && !room.starting && (room.expires <= player.server.getTickCount() || !hasAccess(player))) {
            leave(player, false);
        }
    }

    @SubscribeEvent
    public static void stop(ServerStoppingEvent event) {
        for (var room : List.copyOf(ROOMS.values())) if (!room.starting) cancel(event.getServer(), room);
        MEMBERS.clear();
        ROOMS.clear();
        COOLDOWNS.clear();
    }

    private static boolean hasAccess(ServerPlayer player) {
        return MegaSimulator.atTerminal(player) || PhoneState.hasPhone(player);
    }

    private static boolean eligible(ServerPlayer player) {
        return hasAccess(player) && ArenaMatch.available(player) && player.server.isPvpAllowed()
                && SmashConfig.<Boolean>get(SmashConfig.ENABLED);
    }

    public static void open(ServerPlayer player) {
        if (!ArenaMatch.available(player) || !MEMBERS.containsKey(player.getUUID()) && !eligible(player)) { problem(player, "unavailable"); return; }
        send(player, true);
    }

    private static void problem(ServerPlayer player, String key) {
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
        if ("resume".equals(action)) {
            if (room != null && ArenaMatch.available(player)) send(player, true);
            else if (room == null) send(player, false);
            return;
        }
        if ("leave".equals(action)) { leave(player, true); return; }
        if (!eligible(player) && (room == null || room.bracket == null || !ArenaMatch.available(player))) { problem(player, "unavailable"); return; }
        if (room != null && room.starting) return;
        if ("list".equals(action)) { send(player, false); return; }
        if ("create".equals(action)) {
            var raw = command.getString("stake");
            if (room != null || ROOMS.size() >= MAX_ROOMS) { problem(player, "unavailable"); return; }
            if (!raw.matches("[0-9]{1,18}")) { problem(player, "stake"); return; }
            var stake = new BigInteger(raw);
            boolean multi = command.getBoolean("multi");
            int capacity = multi ? command.getInt("capacity") : 2;
            if (multi && (!command.contains("capacity", Tag.TAG_INT) || capacity != 0 && capacity < 4)) { problem(player, "capacity"); return; }
            boolean ownParty = command.getBoolean("ownParty");
            var host = new Seat(player, ownParty);
            if (ownParty && host.personalParty.isEmpty()) { problem(player, "own_party"); return; }
            if (!debit(player, stake)) { problem(player, "funds"); return; }
            room = new Room(player, host, stake, multi, capacity, ownParty);
            ROOMS.put(room.id, room);
            MEMBERS.put(player.getUUID(), room);
            if (room.multi()) recordDeposits(player.server, room);
            send(player, false);
            return;
        }
        if ("join".equals(action)) {
            if (room != null || !command.hasUUID("room")) return;
            join(player, ROOMS.get(command.getUUID("room")), false);
            return;
        }
        if (room == null || !command.hasUUID("room") || !room.id.equals(command.getUUID("room"))) { send(player, false); return; }
        var seat = room.seats.get(player.getUUID());
        if (seat == null) return;
        if ("invite".equals(action)) { invite(player, room, command); return; }
        if (room.bracket != null) { roundCommand(player, room, seat, action, command); return; }
        if (room.multi()) {
            if ("ready".equals(action)) seat.ready = !seat.ready;
            else if ("start".equals(action) && seat == room.host && room.seats.size() >= 3
                    && room.seats.values().stream().allMatch(s -> s.ready && eligibleOnline(player.server, s.id))) {
                var order = new ArrayList<>(room.seats.keySet());
                java.util.Collections.shuffle(order);
                room.bracket = new FrontierBracket(order, player.server.getTickCount());
                room.seats.values().forEach(s -> s.ready = false);
                broadcast(player.server, room, true, null);
                return;
            } else { send(player, false); return; }
            room.touch(player);
            broadcast(player.server, room);
            return;
        }
        switch (action) {
            case "edit", "ready" -> { if (!draft(player, room, seat, action, command)) return; }
            case "start" -> {
                var opponent = room.guest();
                if (seat != room.host || opponent == null || !room.host.ready || !opponent.ready) { send(player, false); return; }
                var guest = player.server.getPlayerList().getPlayer(opponent.id);
                if (guest == null || !eligible(guest)) { problem(player, "closed"); return; }
                if (!room.ownParty && (FrontierRoster.validate(room.host.draft(), true) != FrontierRoster.Issue.NONE
                        || FrontierRoster.validate(opponent.draft(), true) != FrontierRoster.Issue.NONE)) {
                    room.host.ready = opponent.ready = false;
                    broadcast(player.server, room);
                    problem(player, "unavailable");
                    return;
                }
                var escrow = new Escrow(player.server, room);
                room.starting = true;
                boolean ownParty = room.ownParty;
                if (!ArenaMatch.startFrontier(player, guest, () -> seat.forMatch(player, ownParty),
                        () -> opponent.forMatch(guest, ownParty), escrow, () -> escrow.accept(null))) escrow.accept(null);
                remove(room);
                close(player);
                close(guest);
                return;
            }
            default -> { send(player, false); return; }
        }
        room.touch(player);
        broadcast(player.server, room);
    }

    private static boolean draft(ServerPlayer player, Room room, Seat seat, String action, CompoundTag command) {
        if (room.ownParty) {
            if (!"ready".equals(action) || seat.personalParty.isEmpty()) { send(player, false); return false; }
            seat.ready = !seat.ready;
            return true;
        }
        if ("edit".equals(action)) {
            int slot = command.getInt("slot");
            var pick = decode(command.getCompound("pick"));
            if (seat.ready || slot < 0 || slot >= FrontierRoster.TEAM_SIZE || pick == null) { send(player, false); return false; }
            var next = new ArrayList<>(seat.draft());
            next.set(slot, pick);
            if (!validate(player, next, false)) return false;
            seat.picks[slot] = pick;
        } else {
            if (!seat.ready && !validate(player, seat.draft(), true)) return false;
            seat.ready = !seat.ready;
        }
        return true;
    }

    private static boolean validate(ServerPlayer player, List<FrontierRoster.Pick> draft, boolean ready) {
        var issue = FrontierRoster.validate(draft, ready);
        if (issue == FrontierRoster.Issue.NONE) return true;
        problem(player, issue.name().toLowerCase(Locale.ROOT));
        return false;
    }

    private static boolean eligibleOnline(MinecraftServer server, UUID id) {
        var player = id == null ? null : server.getPlayerList().getPlayer(id);
        return player != null && eligible(player);
    }

    private static boolean join(ServerPlayer player, Room room, boolean invited) {
        int now = player.server.getTickCount();
        if (!eligible(player) || MEMBERS.containsKey(player.getUUID()) || room == null || !room.open(now)
                || !eligibleOnline(player.server, room.host.id)
                || invited && room.invites.getOrDefault(player.getUUID(), 0) <= now) {
            problem(player, "closed"); return false;
        }
        var seat = new Seat(player, room.ownParty);
        if (room.ownParty && seat.personalParty.isEmpty()) { problem(player, "own_party"); return false; }
        if (!debit(player, room.stake)) { problem(player, "funds"); return false; }
        room.seats.put(player.getUUID(), seat);
        room.host.ready = false;
        room.touch(player);
        MEMBERS.put(player.getUUID(), room);
        if (room.multi()) recordDeposits(player.server, room);
        broadcast(player.server, room, false, player.getUUID());
        send(player, invited);
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
        target.sendSystemMessage(Component.translatable("rumble.frontier.invited_chat", player.getDisplayName(), room.stake)
                .append(" ").append(Component.translatable("rumble.frontier.accept_invite").withStyle(style -> style
                        .withColor(net.minecraft.ChatFormatting.GOLD).withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rumble_frontier_accept " + room.id)))));
        broadcast(player.server, room);
    }

    private static void roundCommand(ServerPlayer player, Room room, Seat seat, String action, CompoundTag command) {
        var round = room.bracket;
        var bout = round.bout(seat.id);
        if (bout == null || bout.done || bout.running || !room.roundOpened || room.withdrawals.contains(seat.id)
                || command.getInt("round") != round.rounds.size() || !("edit".equals(action) || "ready".equals(action))) {
            send(player, false); return;
        }
        if (!draft(player, room, seat, action, command)) return;
        broadcast(player.server, room);
    }

    @SubscribeEvent
    public static void rounds(ServerTickEvent.Post event) {
        var server = event.getServer();
        int now = server.getTickCount();
        if (now % 10 != 0) return;
        if (now % 1200 == 0) COOLDOWNS.values().removeIf(expiry -> expiry <= now);
        for (var room : List.copyOf(ROOMS.values())) {
            var bracket = room.bracket;
            if (bracket == null) continue;
            if (bracket.finished) {
                if (now >= room.expires) {
                    var present = room.seats.keySet().stream().filter(id -> MEMBERS.get(id) == room).toList();
                    remove(room);
                    for (var id : present) {
                        var player = server.getPlayerList().getPlayer(id);
                        if (player != null) send(player, false);
                    }
                }
                continue;
            }
            if (now < bracket.opensAt) continue;
            if (!room.roundOpened) {
                room.roundOpened = true;
                broadcast(server, room, true, null);
            }
            boolean resolvedUnavailable = false;
            for (var bout : bracket.current()) {
                if (bout.done || bout.running) continue;
                var host = roundPlayer(server, room, bout.left);
                var guest = roundPlayer(server, room, bout.right);
                boolean readyHost = host != null && room.seats.get(bout.left).ready;
                boolean readyGuest = guest != null && room.seats.get(bout.right).ready;
                boolean leftOut = room.withdrawals.contains(bout.left), rightOut = room.withdrawals.contains(bout.right);
                if (bout.resolveUnavailable(leftOut, rightOut, now >= bracket.closesAt, readyHost, readyGuest)) {
                    resolvedUnavailable = true;
                } else if (readyHost && readyGuest) {
                    bout.running = true;
                    Runnable retry = () -> {
                        bout.running = false;
                        room.seats.get(bout.left).ready = room.seats.get(bout.right).ready = false;
                        // Allow both players to reopen their lineups after a failed instance has closed.
                        bracket.closesAt = Math.max(bracket.closesAt, server.getTickCount() + 20 * 30);
                        if (!host.hasDisconnected()) problem(host, "round_setup");
                        if (!guest.hasDisconnected()) problem(guest, "round_setup");
                    };
                    boolean started = ArenaMatch.startFrontier(host, guest,
                            () -> room.seats.get(bout.left).forMatch(host, room.ownParty),
                            () -> room.seats.get(bout.right).forMatch(guest, room.ownParty),
                            winner -> { bout.resolve(winner); broadcast(server, room); }, retry);
                    if (!started) retry.run();
                    else { close(host); close(guest); }
                }
            }
            if (resolvedUnavailable) broadcast(server, room);
            if (bracket.current().stream().anyMatch(b -> !b.done) || room.seats.keySet().stream().anyMatch(id -> {
                if (room.withdrawals.contains(id)) return false;
                var p = server.getPlayerList().getPlayer(id);
                return p != null && ArenaMatch.inFrontier(p);
            })) continue;
            bracket.advance(now);
            if (bracket.finished) {
                room.prizes = bracket.prizes(room.stake.multiply(BigInteger.valueOf(bracket.size)));
                if (room.prizes.isEmpty()) room.seats.keySet().forEach(id -> credit(server, id, room.stake));
                else room.prizes.forEach((id, amount) -> credit(server, id, amount));
                room.settled = true;
                recordDeposits(server, room);
                room.expires = now + 20 * 120;
            }
            room.seats.values().forEach(seat -> seat.ready = false);
            room.roundOpened = false;
            broadcast(server, room, true, null);
        }
    }

    private static ServerPlayer roundPlayer(MinecraftServer server, Room room, UUID id) {
        if (id == null || room.withdrawals.contains(id)) return null;
        var p = server.getPlayerList().getPlayer(id);
        return p != null && ArenaMatch.available(p) && server.isPvpAllowed() && SmashConfig.<Boolean>get(SmashConfig.ENABLED) ? p : null;
    }

    private static void leave(ServerPlayer player, boolean answer) {
        Room room = MEMBERS.get(player.getUUID());
        if (room == null || room.starting) return;
        if (room.bracket != null) {
            room.withdrawals.add(player.getUUID());
            MEMBERS.remove(player.getUUID(), room);
            broadcast(player.server, room);
            if (answer) send(player, false);
            return;
        }
        if (room.host.id.equals(player.getUUID())) {
            cancel(player.server, room);
            for (var id : room.seats.keySet()) if (!id.equals(player.getUUID())) {
                var guest = player.server.getPlayerList().getPlayer(id);
                if (guest != null) problem(guest, "closed");
            }
        } else {
            MEMBERS.remove(player.getUUID());
            room.seats.remove(player.getUUID());
            room.host.ready = false;
            credit(player.server, player.getUUID(), room.stake);
            if (room.multi()) recordDeposits(player.server, room);
            broadcast(player.server, room);
        }
        if (!player.hasDisconnected()) send(player, false);
    }

    private static void remove(Room room) {
        ROOMS.remove(room.id);
        room.seats.keySet().forEach(id -> MEMBERS.remove(id, room));
    }

    private static void cancel(MinecraftServer server, Room room) {
        remove(room);
        if (!room.settled) {
            room.seats.keySet().forEach(id -> credit(server, id, room.stake));
            room.settled = true;
            if (room.multi()) recordDeposits(server, room);
        }
    }

    private static void broadcast(MinecraftServer server, Room room) {
        broadcast(server, room, false, null);
    }

    private static void broadcast(MinecraftServer server, Room room, boolean open, UUID skip) {
        for (var id : room.seats.keySet()) {
            if (id.equals(skip) || MEMBERS.get(id) != room) continue;
            var p = server.getPlayerList().getPlayer(id);
            if (p != null) send(p, open && ArenaMatch.available(p), "", true);
        }
    }

    private static void close(ServerPlayer player) {
        var data = new CompoundTag();
        data.putBoolean("close", true);
        var room = MEMBERS.get(player.getUUID());
        if (room != null) data.putUUID("room", room.id);
        PacketDistributor.sendToPlayer(player, new View(data));
    }

    private static void send(ServerPlayer player, boolean open) { send(player, open, ""); }

    private static void send(ServerPlayer player, boolean open, String problem) { send(player, open, problem, false); }

    private static void send(ServerPlayer player, boolean open, String problem, boolean automatic) {
        var data = new CompoundTag();
        data.putBoolean("open", open);
        data.putBoolean("automatic", automatic);
        data.putString("problem", problem);
        data.putString("balance", balance(player).toString());
        var room = MEMBERS.get(player.getUUID());
        if (room == null) {
            var available = new ListTag();
            for (var entry : ROOMS.values().stream().sorted(java.util.Comparator.comparing(Room::multi)).toList()) {
                if (entry.bracket != null || entry.starting || entry.expires <= player.server.getTickCount()) continue;
                var row = new CompoundTag();
                row.putUUID("room", entry.id);
                row.put("host", publicSeat(entry.host));
                if (entry.guest() != null) row.put("guest", publicSeat(entry.guest()));
                row.putInt("capacity", entry.capacity);
                row.putBoolean("multi", entry.multi());
                row.putBoolean("ownParty", entry.ownParty);
                row.putInt("count", entry.seats.size());
                row.putString("stake", entry.stake.toString());
                available.add(row);
            }
            data.put("rooms", available);
        } else {
            data.putUUID("room", room.id);
            data.putString("stake", room.stake.toString());
            data.putBoolean("owner", room.host.id.equals(player.getUUID()));
            data.put("host", encode(room.host));
            if (room.guest() != null) data.put("guest", encode(room.guest()));
            data.putInt("capacity", room.capacity);
            data.putBoolean("multi", room.multi());
            data.putBoolean("ownParty", room.ownParty);
            data.putInt("count", room.seats.size());
            data.putBoolean("canInvite", room.open(player.server.getTickCount()));
            var seats = new ListTag();
            room.seats.values().forEach(s -> seats.add(publicSeat(s)));
            data.put("seats", seats);
            var self = room.seats.get(player.getUUID());
            data.putBoolean("selfReady", self.ready);
            data.putBoolean("canStart", room.seats.size() >= 3 && room.seats.values().stream().allMatch(s -> s.ready));
            if (room.bracket != null) encodeBracket(player, room, data);
            else if (room.open(player.server.getTickCount())) encodeInvites(player, room, data);
        }
        PacketDistributor.sendToPlayer(player, new View(data));
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

    private static void encodeBracket(ServerPlayer player, Room room, CompoundTag data) {
        var bracket = room.bracket;
        data.putInt("round", bracket.rounds.size());
        data.putInt("roundTotal", bracket.depth);
        data.putBoolean("finished", bracket.finished);
        data.putBoolean("roundOpened", room.roundOpened);
        var rows = new ListTag();
        for (var round : bracket.rounds) {
            var column = new CompoundTag();
            var bouts = new ListTag();
            for (var bout : round) {
                var row = new CompoundTag();
                if (bout.left != null) row.put("left", publicSeat(room.seats.get(bout.left)));
                if (bout.right != null) row.put("right", publicSeat(room.seats.get(bout.right)));
                if (bout.winner != null) row.putUUID("winner", bout.winner);
                row.putBoolean("done", bout.done);
                row.putBoolean("running", bout.running);
                row.putBoolean("bronze", bout.bronze);
                bouts.add(row);
            }
            column.put("bouts", bouts);
            rows.add(column);
        }
        data.put("rounds", rows);
        var bout = bracket.bout(player.getUUID());
        data.putBoolean("boutDone", bout != null && bout.done);
        data.putBoolean("hasBout", bout != null);
        if (!bracket.finished && bout != null && bout.opponent(player.getUUID()) != null) {
            data.put("currentOpponent", publicSeat(room.seats.get(bout.opponent(player.getUUID()))));
        }
        if (bout != null && bout.opponent(player.getUUID()) != null && !bout.done && !bout.running && room.roundOpened) {
            data.put("own", encode(room.seats.get(player.getUUID())));
            data.put("rival", encode(room.seats.get(bout.opponent(player.getUUID()))));
        }
        data.putBoolean("bye", !bracket.finished && bout != null && bout.done && bout.opponent(player.getUUID()) == null);
        var awards = new ListTag();
        room.prizes.forEach((id, amount) -> {
            var award = publicSeat(room.seats.get(id));
            award.putString("prize", amount.toString());
            awards.add(award);
        });
        data.put("awards", awards);
    }

    private static CompoundTag publicSeat(Seat seat) {
        var data = new CompoundTag();
        data.putString("name", seat.name);
        data.putUUID("player", seat.id);
        data.putBoolean("ready", seat.ready);
        var species = new ListTag();
        for (var pick : seat.picks) species.add(StringTag.valueOf(pick.species().toString()));
        data.put("species", species);
        return data;
    }

    private static CompoundTag encode(Seat seat) {
        var data = new CompoundTag();
        data.putString("name", seat.name);
        data.putUUID("player", seat.id);
        data.putBoolean("ready", seat.ready);
        var picks = new ListTag();
        for (var pick : seat.picks) picks.add(encode(pick));
        data.put("picks", picks);
        return data;
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
