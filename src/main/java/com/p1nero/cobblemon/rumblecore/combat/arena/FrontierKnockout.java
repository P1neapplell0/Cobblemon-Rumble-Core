package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.p1nero.cobblemon.rumblecore.client.combat.FrontierKnockoutOverlay;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class FrontierKnockout {
    public static final int CUE_TICKS = 29;
    public static final int SLOW_TICKS = 16;
    public static final int FLIGHT_STEP = 3, TUMBLE_TICKS = 40;
    public static final float SPEED = .10f;

    private FrontierKnockout() {}

    public record Cue(ResourceLocation dimension, int ticks, int focus, int attacker, List<Integer> actors) implements CustomPacketPayload {
        static final Type<Cue> TYPE = new Type<>(PrologueWorld.id("frontier_ko"));
        static final StreamCodec<RegistryFriendlyByteBuf, Cue> CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Cue::dimension, ByteBufCodecs.VAR_INT, Cue::ticks,
                ByteBufCodecs.VAR_INT, Cue::focus, ByteBufCodecs.VAR_INT, Cue::attacker,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(1024)), Cue::actors, Cue::new);
        @Override public Type<Cue> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("3").playToClient(Cue.TYPE, Cue.CODEC, (packet, context) -> FrontierKnockoutOverlay.start(packet));
    }
}
