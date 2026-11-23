package com.p1nero.cobblemon.rumblecore.combat;

import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeMoveWindupClient;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record RealtimeMoveWindupPacket(int ticks) implements CustomPacketPayload {
    private static final Type<RealtimeMoveWindupPacket> TYPE = new Type<>(PrologueWorld.id("realtime_move_windup"));
    private static final StreamCodec<RegistryFriendlyByteBuf, RealtimeMoveWindupPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RealtimeMoveWindupPacket::ticks, RealtimeMoveWindupPacket::new);

    @Override public Type<RealtimeMoveWindupPacket> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC,
                (packet, context) -> RealtimeMoveWindupClient.onWarning(packet.ticks()));
    }

}
