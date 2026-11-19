package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.prologue.client.PrologueClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ProloguePackets {
    public enum Cue {
        NONE, TRANSFER, LOAD, CHALLENGE, MODE_EXIT, MODE_REENTER, MOVEMENT, LOCK, ATTACK, HEAVY, GUARD, PARRY, MOVE, SPAR, CLEAR,
        MEGA_ITEM, MEGA_BATTLE, TERA_ITEM, TERA_BATTLE;

        public boolean defensive() { return this == GUARD || this == PARRY; }

        public boolean accepts(Cue action) { return this == action || this == PARRY && action == GUARD; }

        public boolean focus() { return ordinal() >= CHALLENGE.ordinal() && ordinal() <= MOVE.ordinal(); }
    }

    private ProloguePackets() {}

    public record Talk(int entity) implements CustomPacketPayload {
        public static final Type<Talk> TYPE = new Type<>(PrologueWorld.id("talk"));
        static final StreamCodec<RegistryFriendlyByteBuf, Talk> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Talk::entity, Talk::new);
        @Override public Type<Talk> type() { return TYPE; }
    }

    public record ArenaReady(int target) implements CustomPacketPayload {
        public static final Type<ArenaReady> TYPE = new Type<>(PrologueWorld.id("prologue_ready"));
        static final StreamCodec<RegistryFriendlyByteBuf, ArenaReady> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, ArenaReady::target, ArenaReady::new);
        @Override public Type<ArenaReady> type() { return TYPE; }
    }

    public record TargetLocked(int target) implements CustomPacketPayload {
        public static final Type<TargetLocked> TYPE = new Type<>(PrologueWorld.id("prologue_lock"));
        static final StreamCodec<RegistryFriendlyByteBuf, TargetLocked> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, TargetLocked::target, TargetLocked::new);
        @Override public Type<TargetLocked> type() { return TYPE; }
    }

    public record StoryTravel(int menuId, boolean toHall) implements CustomPacketPayload {
        public static final Type<StoryTravel> TYPE = new Type<>(PrologueWorld.id("story_travel"));
        static final StreamCodec<RegistryFriendlyByteBuf, StoryTravel> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, StoryTravel::menuId, ByteBufCodecs.BOOL, StoryTravel::toHall, StoryTravel::new);
        @Override public Type<StoryTravel> type() { return TYPE; }
    }

    public record PersonalTravel(int menuId) implements CustomPacketPayload {
        public static final Type<PersonalTravel> TYPE = new Type<>(PrologueWorld.id("personal_travel"));
        static final StreamCodec<RegistryFriendlyByteBuf, PersonalTravel> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, PersonalTravel::menuId, PersonalTravel::new);
        @Override public Type<PersonalTravel> type() { return TYPE; }
    }

    public record State(int cue, int target, boolean ready, int moves, boolean accepted) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(PrologueWorld.id("prologue_cue"));
        static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, State::cue, ByteBufCodecs.VAR_INT, State::target,
                ByteBufCodecs.BOOL, State::ready, ByteBufCodecs.VAR_INT, State::moves,
                ByteBufCodecs.BOOL, State::accepted, State::new);
        @Override public Type<State> type() { return TYPE; }
    }

    public static void show(ServerPlayer player, Cue step, int target, boolean ready, int moves) {
        show(player, step, target, ready, moves, false);
    }

    public static void show(ServerPlayer player, Cue step, int target, boolean ready, int moves, boolean accepted) {
        PacketDistributor.sendToPlayer(player, new State(step.ordinal(), target, ready, moves, accepted));
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("5");
        registrar.playToServer(Talk.TYPE, Talk.CODEC, (p, ctx) -> PrologueStory.talk((ServerPlayer) ctx.player(), p.entity()));
        registrar.playToServer(ArenaReady.TYPE, ArenaReady.CODEC, (p, ctx) -> PrologueTraining.clientReady((ServerPlayer) ctx.player(), p.target()));
        registrar.playToServer(TargetLocked.TYPE, TargetLocked.CODEC, (p, ctx) -> PrologueTraining.locked((ServerPlayer) ctx.player(), p.target()));
        registrar.playToServer(StoryTravel.TYPE, StoryTravel.CODEC, (p, ctx) -> TrialWaystones.travel((ServerPlayer) ctx.player(), p.menuId(), p.toHall()));
        registrar.playToServer(PersonalTravel.TYPE, PersonalTravel.CODEC, (p, ctx) -> TrialWaystones.travelHome((ServerPlayer) ctx.player(), p.menuId()));
        registrar.playToClient(State.TYPE, State.CODEC, (p, ctx) -> PrologueClient.receive(p));
    }
}
