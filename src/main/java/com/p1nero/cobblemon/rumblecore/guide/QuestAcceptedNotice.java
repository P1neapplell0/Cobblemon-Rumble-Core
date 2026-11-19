package com.p1nero.cobblemon.rumblecore.guide;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.client.quest.AdventureQuestAcceptedOverlay;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.arcadia.arc_quest.api.event.quest.QuestAcceptedEvent;
import org.arcadia.arc_quest.quest.registry.QuestRegistry;
import org.arcadia.arc_quest.quest.service.TrackedQuestService;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public record QuestAcceptedNotice(Component title, Component description) implements CustomPacketPayload {
    private static final Type<QuestAcceptedNotice> TYPE = new Type<>(PrologueWorld.id("quest_accepted_notice"));
    private static final StreamCodec<RegistryFriendlyByteBuf, QuestAcceptedNotice> CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, QuestAcceptedNotice::title,
            ComponentSerialization.STREAM_CODEC, QuestAcceptedNotice::description, QuestAcceptedNotice::new);

    @Override public Type<QuestAcceptedNotice> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (packet, ctx) -> AdventureQuestAcceptedOverlay.show(packet.title(), packet.description()));
    }

    @SubscribeEvent
    public static void onAccepted(QuestAcceptedEvent event) {
        var quest = event.getQuestId();
        if (!CobblemonRumbleCore.MOD_ID.equals(quest.getNamespace())) return;
        TrackedQuestService.setTrackedQuest(event.getPlayer(), quest.toString());
        var text = QuestRegistry.getQuestTextById(event.getPlayer(), quest.toString(), null);
        if (text != null) PacketDistributor.sendToPlayer(event.getPlayer(), new QuestAcceptedNotice(text.questDisplayName(), text.questDescription()));
    }
}
