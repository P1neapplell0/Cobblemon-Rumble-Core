package com.p1nero.cobblemon.rumblecore.shop;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.RecyclingScreen;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class RecyclingPackets {
    private RecyclingPackets() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(Request.TYPE, Request.CODEC, (p, context) -> BulkRecycling.handle((ServerPlayer) context.player(), p));
        registrar.playToClient(Result.TYPE, Result.CODEC, (p, context) -> RecyclingScreen.receive(p));
    }

    public record Request(int menuId, int quote, List<ResourceLocation> selection) implements CustomPacketPayload {
        static final Type<Request> TYPE = new Type<>(id("recycle_request"));
        static final StreamCodec<ByteBuf, Request> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Request::menuId,
                ByteBufCodecs.VAR_INT, Request::quote, ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(4096)),
                Request::selection, Request::new);
        @Override public Type<Request> type() { return TYPE; }
    }

    public record Result(int menuId, int quote, String status, String inventory, String storage) implements CustomPacketPayload {
        static final Type<Result> TYPE = new Type<>(id("recycle_result"));
        static final StreamCodec<ByteBuf, Result> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Result::menuId,
                ByteBufCodecs.VAR_INT, Result::quote, ByteBufCodecs.stringUtf8(16), Result::status,
                ByteBufCodecs.stringUtf8(100), Result::inventory, ByteBufCodecs.stringUtf8(100), Result::storage, Result::new);
        @Override public Type<Result> type() { return TYPE; }
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(CobblemonRumbleCore.MOD_ID, path); }
}
