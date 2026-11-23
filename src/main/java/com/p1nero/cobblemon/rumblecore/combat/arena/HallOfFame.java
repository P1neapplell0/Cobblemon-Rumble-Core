package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.Cobblemon;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.client.combat.HallOfFameScreen;
import com.p1nero.cobblemon.rumblecore.prologue.ChampionStory;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.client.screen.information.InstanceInformationScreen;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.commands.Commands;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class HallOfFame {
    private static final UUID PREVIEW_TOKEN = new UUID(0L, 0L);
    private static final Map<ServerPlayer, UUID> PENDING = new WeakHashMap<>();

    private HallOfFame() {}

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rumble")
                .then(Commands.literal("halloffame").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("preview").executes(ctx -> {
                            preview(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))));
    }

    private static void preview(ServerPlayer player) {
        var roster = new ListTag();
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);
        for (int i = 0; i < 6; i++) {
            var pokemon = party.get(i);
            if (pokemon == null) continue;
            var entry = ArenaRoster.pokemon(pokemon);
            entry.putBoolean("used", true);
            roster.add(entry);
        }
        if (roster.isEmpty()) {
            for (String species : List.of("pikachu", "bulbasaur", "charmander", "squirtle", "eevee", "lucario")) {
                var entry = new CompoundTag();
                entry.putString("species", "cobblemon:" + species);
                entry.putBoolean("used", true);
                roster.add(entry);
            }
        }
        var details = new CompoundTag();
        details.putString("player", player.getGameProfile().getName());
        details.put("roster", roster);
        PacketDistributor.sendToPlayer(player, new Show(PREVIEW_TOKEN, details));
    }

    public static boolean isPreview(UUID token) { return PREVIEW_TOKEN.equals(token); }

    public static void open(ServerPlayer player) {
        if (!ChampionStory.awaitingCeremony(player)) return;
        var record = InstanceManager.get(player.server).personalBest(ChampionStory.TEMPLATE, player.getUUID());
        var details = new CompoundTag();
        details.putString("player", player.getGameProfile().getName());
        if (record != null) {
            String raw = record.playerDetails().get(player.getUUID());
            if (raw != null) {
                try { details = TagParser.parseTag(raw); }
                catch (com.mojang.brigadier.exceptions.CommandSyntaxException ignored) { }
            }
        }
        var token = UUID.randomUUID();
        PENDING.put(player, token);
        PacketDistributor.sendToPlayer(player, new Show(token, details));
    }

    private static void finish(ServerPlayer player, UUID token) {
        if (!PENDING.remove(player, token)) return;
        ChampionStory.finishCeremony(player);
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var network = event.registrar("1");
        network.playToClient(Show.TYPE, Show.CODEC, (packet, context) -> HallOfFameScreen.open(packet));
        network.playToServer(Finish.TYPE, Finish.CODEC, (packet, context) -> finish((ServerPlayer) context.player(), packet.token()));
        ServerHall.register(network);
    }

    public static void registerClient() {
        InstanceInformationScreen.registerDetailScreen(ArenaRoster.KIND, HallOfFameScreen::details);
    }

    public record Show(UUID token, CompoundTag data) implements CustomPacketPayload {
        private static final Type<Show> TYPE = new Type<>(PrologueWorld.id("hall_of_fame_show"));
        private static final StreamCodec<RegistryFriendlyByteBuf, Show> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Show::token, ByteBufCodecs.COMPOUND_TAG, Show::data, Show::new);
        @Override public Type<Show> type() { return TYPE; }
    }

    public record Finish(UUID token) implements CustomPacketPayload {
        private static final Type<Finish> TYPE = new Type<>(PrologueWorld.id("hall_of_fame_finish"));
        private static final StreamCodec<RegistryFriendlyByteBuf, Finish> CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, Finish::token, Finish::new);
        @Override public Type<Finish> type() { return TYPE; }
    }
}
