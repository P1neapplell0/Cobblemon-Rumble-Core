package com.p1nero.cobblemon.rumblecore.combat;

import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeBeamClashCamera;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record RealtimeBeamClashPacket(int localBody, int opposingBody) implements CustomPacketPayload {
    private static final Type<RealtimeBeamClashPacket> TYPE = new Type<>(PrologueWorld.id("realtime_beam_clash"));
    private static final StreamCodec<RegistryFriendlyByteBuf, RealtimeBeamClashPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RealtimeBeamClashPacket::localBody,
            ByteBufCodecs.VAR_INT, RealtimeBeamClashPacket::opposingBody, RealtimeBeamClashPacket::new);

    @Override public Type<RealtimeBeamClashPacket> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (packet, context) -> RealtimeBeamClashCamera.begin(packet));
    }
}
