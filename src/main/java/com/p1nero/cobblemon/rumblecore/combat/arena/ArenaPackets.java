package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.p1nero.cobblemon.rumblecore.client.combat.BattleNameplates;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.cobblemon.rumblecore.prologue.client.PrologueClient;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ArenaPackets {
    public static final int WILD_ROUND = -1;
    private ArenaPackets() {}

    public record Reply(UUID invitation, boolean accept) implements CustomPacketPayload {
        static final Type<Reply> TYPE = new Type<>(PrologueWorld.id("arena_reply"));
        static final StreamCodec<RegistryFriendlyByteBuf, Reply> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Reply::invitation, ByteBufCodecs.BOOL, Reply::accept, Reply::new);
        @Override public Type<Reply> type() { return TYPE; }
    }

    public record Invite(UUID invitation, Component challenger) implements CustomPacketPayload {
        static final Type<Invite> TYPE = new Type<>(PrologueWorld.id("arena_invite"));
        static final StreamCodec<RegistryFriendlyByteBuf, Invite> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Invite::invitation, ComponentSerialization.STREAM_CODEC, Invite::challenger, Invite::new);
        @Override public Type<Invite> type() { return TYPE; }
    }

    public record Pick(UUID match, int round, int index) implements CustomPacketPayload {
        static final Type<Pick> TYPE = new Type<>(PrologueWorld.id("arena_pick"));
        static final StreamCodec<RegistryFriendlyByteBuf, Pick> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Pick::match, ByteBufCodecs.VAR_INT, Pick::round, ByteBufCodecs.VAR_INT, Pick::index, Pick::new);
        @Override public Type<Pick> type() { return TYPE; }
    }

    public record Prepare(ResourceLocation dimension) implements CustomPacketPayload {
        static final Type<Prepare> TYPE = new Type<>(PrologueWorld.id("arena_prepare"));
        static final StreamCodec<RegistryFriendlyByteBuf, Prepare> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Prepare::dimension, Prepare::new);
        @Override public Type<Prepare> type() { return TYPE; }
    }

    public record Option(int index, Pokemon pokemon) {
        static final StreamCodec<RegistryFriendlyByteBuf, Option> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Option::index, Pokemon.Companion.getS2C_CODEC(), Option::pokemon, Option::new);
    }

    /** An empty roster closes only the switch screen for the named match. */
    public record Switch(UUID match, int round, List<Option> roster, int gymDeathsLeft) implements CustomPacketPayload {
        public Switch(UUID match, int round, List<Option> roster) { this(match, round, roster, -1); }

        static final Type<Switch> TYPE = new Type<>(PrologueWorld.id("arena_switch"));
        static final StreamCodec<RegistryFriendlyByteBuf, Switch> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Switch::match, ByteBufCodecs.VAR_INT, Switch::round,
                Option.CODEC.apply(ByteBufCodecs.list(6)), Switch::roster,
                ByteBufCodecs.VAR_INT, Switch::gymDeathsLeft, Switch::new);
        @Override public Type<Switch> type() { return TYPE; }
    }

    public record GymReport(ResourceLocation dimension, CompoundTag report) implements CustomPacketPayload {
        static final Type<GymReport> TYPE = new Type<>(PrologueWorld.id("gym_report"));
        static final StreamCodec<RegistryFriendlyByteBuf, GymReport> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, GymReport::dimension, ByteBufCodecs.COMPOUND_TAG, GymReport::report, GymReport::new);
        @Override public Type<GymReport> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var net = event.registrar("3");
        GimmickCinematics.register(net);
        net.playToServer(UseTera.TYPE, UseTera.CODEC, (p, ctx) -> TeraCombat.use((ServerPlayer) ctx.player()));
        net.playToClient(TeraUsed.TYPE, TeraUsed.CODEC, (p, ctx) -> PrologueClient.teraUsed(p.used()));
        net.playToServer(UseHeld.TYPE, UseHeld.CODEC, (p, ctx) -> ArenaMatch.useHeld((ServerPlayer) ctx.player()));
        net.playToServer(Reply.TYPE, Reply.CODEC, (p, ctx) -> ArenaMatch.reply((ServerPlayer) ctx.player(), p));
        net.playToServer(Pick.TYPE, Pick.CODEC, (p, ctx) -> {
            var player = (ServerPlayer) ctx.player();
            if (GimmickCinematics.locked(player)) return;
            if (p.round() == WILD_ROUND) com.p1nero.cobblemon.rumblecore.combat.RealtimeBattleEntry.pick(player, p);
            else ArenaMatch.pick(player, p);
        });
        net.playToClient(Invite.TYPE, Invite.CODEC, (p, ctx) -> ArenaClient.invite(p));
        net.playToClient(GymReport.TYPE, GymReport.CODEC, (p, ctx) -> ArenaClient.gymReport(p));
        net.playToClient(Prepare.TYPE, Prepare.CODEC, (p, ctx) -> ArenaClient.prepare(p));
        net.playToClient(Switch.TYPE, Switch.CODEC, (p, ctx) -> ArenaClient.roster(p));
        net.playToClient(Allies.TYPE, Allies.CODEC, (p, ctx) -> BattleNameplates.sync(p));
        net.playToClient(Opponents.TYPE, Opponents.CODEC, (p, ctx) -> BattleNameplates.syncOpponents(p));
        net.playToClient(Tenacity.TYPE, Tenacity.CODEC, (p, ctx) ->
                PrologueClient.tenacity(p.entityId(), p.dimension(), p.ticks()));
    }

    /** Both the player and their controlled body are allies; only the body gets a label. */
    public record Ally(UUID player, String name, int playerEntity, int body) {
        static final StreamCodec<RegistryFriendlyByteBuf, Ally> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Ally::player, ByteBufCodecs.stringUtf8(64), Ally::name,
                ByteBufCodecs.VAR_INT, Ally::playerEntity, ByteBufCodecs.VAR_INT, Ally::body, Ally::new);
    }

    public record Allies(ResourceLocation dimension, List<Ally> players, List<Integer> actors) implements CustomPacketPayload {
        static final Type<Allies> TYPE = new Type<>(PrologueWorld.id("arena_allies"));
        static final StreamCodec<RegistryFriendlyByteBuf, Allies> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Allies::dimension, Ally.CODEC.apply(ByteBufCodecs.list(1024)), Allies::players,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(4096)), Allies::actors, Allies::new);
        @Override public Type<Allies> type() { return TYPE; }
    }

    public record Opponents(ResourceLocation dimension, List<Ally> players, List<Integer> actors) implements CustomPacketPayload {
        static final Type<Opponents> TYPE = new Type<>(PrologueWorld.id("arena_opponents"));
        static final StreamCodec<RegistryFriendlyByteBuf, Opponents> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Opponents::dimension, Ally.CODEC.apply(ByteBufCodecs.list(1024)), Opponents::players,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(4096)), Opponents::actors, Opponents::new);
        @Override public Type<Opponents> type() { return TYPE; }
    }

    public record UseTera() implements CustomPacketPayload {
        static final Type<UseTera> TYPE = new Type<>(PrologueWorld.id("arena_use_tera"));
        static final StreamCodec<RegistryFriendlyByteBuf, UseTera> CODEC = StreamCodec.unit(new UseTera());
        @Override public Type<UseTera> type() { return TYPE; }
    }

    public record TeraUsed(boolean used) implements CustomPacketPayload {
        static final Type<TeraUsed> TYPE = new Type<>(PrologueWorld.id("arena_tera_used"));
        static final StreamCodec<RegistryFriendlyByteBuf, TeraUsed> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, TeraUsed::used, TeraUsed::new);
        @Override public Type<TeraUsed> type() { return TYPE; }
    }

    public record UseHeld() implements CustomPacketPayload {
        static final Type<UseHeld> TYPE = new Type<>(PrologueWorld.id("arena_use_held"));
        static final StreamCodec<RegistryFriendlyByteBuf, UseHeld> CODEC = StreamCodec.unit(new UseHeld());
        @Override public Type<UseHeld> type() { return TYPE; }
    }

    public record Tenacity(int entityId, ResourceLocation dimension, int ticks) implements CustomPacketPayload {
        static final Type<Tenacity> TYPE = new Type<>(PrologueWorld.id("arena_tenacity"));
        static final StreamCodec<RegistryFriendlyByteBuf, Tenacity> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Tenacity::entityId, ResourceLocation.STREAM_CODEC, Tenacity::dimension,
                ByteBufCodecs.VAR_INT, Tenacity::ticks, Tenacity::new);
        @Override public Type<Tenacity> type() { return TYPE; }
    }
}
