package com.p1nero.cobblemon.rumblecore.combat;

import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeImpactCamera;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record RealtimeImpactPacket(float lostFraction, boolean received) implements CustomPacketPayload {
    private static final Type<RealtimeImpactPacket> TYPE = new Type<>(PrologueWorld.id("realtime_impact"));
    private static final StreamCodec<RegistryFriendlyByteBuf, RealtimeImpactPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RealtimeImpactPacket::lostFraction,
            ByteBufCodecs.BOOL, RealtimeImpactPacket::received, RealtimeImpactPacket::new);

    @Override public Type<RealtimeImpactPacket> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC,
                (packet, context) -> RealtimeImpactCamera.hit(packet.lostFraction(), packet.received()));
    }
}
