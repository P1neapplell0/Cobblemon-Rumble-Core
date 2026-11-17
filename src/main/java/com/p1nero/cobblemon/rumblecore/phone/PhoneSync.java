package com.p1nero.cobblemon.rumblecore.phone;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.client.phone.ClientPhoneState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record PhoneSync(int mask) implements CustomPacketPayload {
    public static final Type<PhoneSync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CobblemonRumbleCore.MOD_ID, "phone_apps"));
    private static final StreamCodec<RegistryFriendlyByteBuf, PhoneSync> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PhoneSync::mask, PhoneSync::new);

    @Override public Type<PhoneSync> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) -> ClientPhoneState.update(context.player(), payload.mask()));
    }
}
