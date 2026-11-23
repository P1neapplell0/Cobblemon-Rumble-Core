package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.p1nero.cobblemon.rumblecore.client.combat.ServerHallScreen;
import com.p1nero.cobblemon.rumblecore.mixin.custom_instances.HallCompletionAccess;
import com.p1nero.cobblemon.rumblecore.prologue.ChampionStory;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.storage.record.RunRecord;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ServerHall {
    public static final int MAX_ROWS = 10;

    private ServerHall() {}

    static void register(PayloadRegistrar network) {
        network.playToServer(Request.TYPE, Request.CODEC, (packet, context) -> sendPage((ServerPlayer) context.player(), packet));
        network.playToClient(Page.TYPE, Page.CODEC, (packet, context) -> ServerHallScreen.receive(packet));
    }

    private static void sendPage(ServerPlayer player, Request request) {
        if (request.pvp()) {
            sendPage(player, request, PvpStandings.get(player.server).ranked(), entry ->
                    new Row(entry.id(), clippedName(entry.name()), 0, entry.wins(), entry.losses(), new CompoundTag()));
            return;
        }
        InstanceManager manager = InstanceManager.get(player.server);
        if (manager == null) {
            PacketDistributor.sendToPlayer(player, new Page(request.token(), 0, -1, List.of()));
            return;
        }
        var board = ((HallCompletionAccess) manager).rumble$getCompletions()
                .leaderboard(ChampionStory.TEMPLATE, false, Integer.MAX_VALUE);
        sendPage(player, request, board, entry -> {
            UUID id = UUID.fromString(entry.key());
            String name = clippedName(entry.name());
            return new Row(id, name, entry.run().elapsedTicks(), 0, 0, roster(entry.run(), id, name));
        });
    }

    private static <T> void sendPage(ServerPlayer player, Request request, List<T> board, java.util.function.Function<T, Row> convert) {
        int count = Math.clamp(request.rows(), 1, MAX_ROWS);
        int start = Math.clamp(request.page(), 0, Math.max(0, board.size() - 1) / count) * count;
        int end = start + Math.min(count, board.size() - start);
        var rows = board.subList(start, end).stream().map(convert).toList();
        PacketDistributor.sendToPlayer(player, new Page(request.token(), start, board.size(), rows));
    }

    private static CompoundTag roster(RunRecord run, UUID id, String name) {
        String raw = run.playerDetails().get(id);
        if (raw == null) return new CompoundTag();
        try {
            var snapshot = TagParser.parseTag(raw);
            var team = snapshot.getList("roster", Tag.TAG_COMPOUND);
            if (ArenaRoster.KIND.equals(snapshot.getString("kind")) && !team.isEmpty() && team.size() <= 6) {
                var details = new CompoundTag();
                details.putString("player", name);
                details.put("roster", team);
                return details;
            }
        } catch (CommandSyntaxException ignored) {
            // A missing or unreadable snapshot must not hide the completion record.
        }
        return new CompoundTag();
    }

    private static String clippedName(String name) {
        return name.substring(0, Math.min(name.length(), 64));
    }

    public record Request(UUID token, int page, int rows, boolean pvp) implements CustomPacketPayload {
        private static final Type<Request> TYPE = new Type<>(PrologueWorld.id("server_hall_request_v2"));
        private static final StreamCodec<ByteBuf, Request> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Request::token, ByteBufCodecs.VAR_INT, Request::page,
                ByteBufCodecs.VAR_INT, Request::rows, ByteBufCodecs.BOOL, Request::pvp, Request::new);
        @Override public Type<Request> type() { return TYPE; }
    }

    public record Row(UUID id, String name, long ticks, long wins, long losses, CompoundTag roster) {
        private static final StreamCodec<ByteBuf, Row> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Row::id, ByteBufCodecs.stringUtf8(64), Row::name,
                ByteBufCodecs.VAR_LONG, Row::ticks, ByteBufCodecs.VAR_LONG, Row::wins, ByteBufCodecs.VAR_LONG, Row::losses,
                ByteBufCodecs.COMPOUND_TAG, Row::roster, Row::new);
    }

    public record Page(UUID token, int start, int total, List<Row> rows) implements CustomPacketPayload {
        private static final Type<Page> TYPE = new Type<>(PrologueWorld.id("server_hall_page_v3"));
        private static final StreamCodec<ByteBuf, Page> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Page::token, ByteBufCodecs.VAR_INT, Page::start,
                ByteBufCodecs.VAR_INT, Page::total, Row.CODEC.apply(ByteBufCodecs.list(MAX_ROWS)), Page::rows, Page::new);
        @Override public Type<Page> type() { return TYPE; }
    }
}
