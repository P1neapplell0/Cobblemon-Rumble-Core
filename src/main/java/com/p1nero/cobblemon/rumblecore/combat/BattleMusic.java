package com.p1nero.cobblemon.rumblecore.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Brawl;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.client.combat.BattleMusicClient;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.reward.WildCapture;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class BattleMusic {
    private static final Map<ServerPlayer, Engagement> ENGAGED = new WeakHashMap<>();
    private static final Map<ServerPlayer, Byte> PLAYING = new WeakHashMap<>();

    private BattleMusic() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(State.TYPE, State.CODEC, (packet, context) -> BattleMusicClient.receive(packet));
    }

    public static void engage(Smash.Session session, PokemonEntity opponent, boolean explicit) {
        if (opponent.getPokemon().isWild() && !WildCapture.stunned(opponent)) {
            var existing = ENGAGED.get(session.player);
            if (existing == null || existing.session() != session || existing.opponent() != opponent || explicit) {
                ENGAGED.put(session.player, new Engagement(session, opponent, explicit ? session.player.server.getTickCount() + 80 : 0));
            }
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        byte track = 0;
        var arena = ArenaMatch.inArena(player);
        var engagement = ENGAGED.get(player);
        if (player.isAlive() && arena != null && arena.musicActive(player)) track = 2;
        else if (engagement != null) {
            var opponent = engagement.opponent();
            if (player.isAlive() && Smash.session(player) == engagement.session() && opponent.isAlive()
                    && opponent.level() == player.level() && !WildCapture.stunned(opponent)
                    && (Brawl.fighting(opponent) || player.server.getTickCount() < engagement.expires())
                    && opponent.distanceToSqr(engagement.session().pokemon) < 32 * 32) track = 1;
            else ENGAGED.remove(player);
        }
        if (track == PLAYING.getOrDefault(player, (byte) 0)) return;
        if (track == 0) PLAYING.remove(player);
        else PLAYING.put(player, track);
        PacketDistributor.sendToPlayer(player, new State(player.level().dimension().location(), track));
    }

    private record Engagement(Smash.Session session, PokemonEntity opponent, int expires) {}

    public record State(ResourceLocation dimension, byte track) implements CustomPacketPayload {
        static final Type<State> TYPE = new Type<>(PrologueWorld.id("battle_music"));
        static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, State::dimension, ByteBufCodecs.BYTE, State::track, State::new);
        @Override public Type<State> type() { return TYPE; }
    }
}
