package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.p1nero.cobblemon.rumblecore.client.combat.ArenaIntroOverlay;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ArenaIntro {
    public static final int MINIMUM_TICKS = 108;
    public static final int READY_TICKS = 30;
    public static final byte READY = 1, CANCEL = 2;

    private ArenaIntro() {}

    public record Portrait(UUID player, Component name, ResourceLocation species, List<String> aspects) {
        private static final ResourceLocation HUMAN = PrologueWorld.id("player");
        private static final ResourceLocation TRAINER = PrologueWorld.id("gym_trainer");
        public static final StreamCodec<RegistryFriendlyByteBuf, Portrait> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Portrait::player, ComponentSerialization.STREAM_CODEC, Portrait::name,
                ResourceLocation.STREAM_CODEC, Portrait::species,
                ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(32)), Portrait::aspects, Portrait::new);

        public static Portrait player(UUID player, Component name) {
            return new Portrait(player, name, HUMAN, List.of());
        }

        public static Portrait pokemon(ResourceLocation species, Component name, List<String> aspects) {
            return new Portrait(new UUID(0, 0), name, species, aspects);
        }

        public boolean isPlayer() { return species.equals(HUMAN); }

        public static Portrait trainer(Component name, String aspect) {
            return new Portrait(new UUID(0, 0), name, TRAINER, List.of(aspect));
        }

        public boolean isTrainer() { return species.equals(TRAINER); }
    }

    public record Start(UUID match, ResourceLocation destination, Portrait left, Portrait right,
                        List<Portrait> leftSupport, List<Portrait> rightSupport) implements CustomPacketPayload {
        public Start(UUID match, ResourceLocation destination, Portrait left, Portrait right) {
            this(match, destination, left, right, List.of(), List.of());
        }
        public static final Type<Start> TYPE = new Type<>(PrologueWorld.id("arena_intro"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Start> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Start::match, ResourceLocation.STREAM_CODEC, Start::destination,
                Portrait.CODEC, Start::left, Portrait.CODEC, Start::right,
                Portrait.CODEC.apply(ByteBufCodecs.list(3)), Start::leftSupport,
                Portrait.CODEC.apply(ByteBufCodecs.list(3)), Start::rightSupport, Start::new);
        @Override public Type<Start> type() { return TYPE; }
    }

    public record Transition(UUID match, byte state) implements CustomPacketPayload {
        public static final Type<Transition> TYPE = new Type<>(PrologueWorld.id("arena_intro_transition"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Transition> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Transition::match, ByteBufCodecs.BYTE, Transition::state, Transition::new);
        @Override public Type<Transition> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(Start.TYPE, Start.CODEC, (packet, context) -> ArenaIntroOverlay.start(packet));
        registrar.playToClient(Transition.TYPE, Transition.CODEC, (packet, context) -> ArenaIntroOverlay.transition(packet));
    }
}
