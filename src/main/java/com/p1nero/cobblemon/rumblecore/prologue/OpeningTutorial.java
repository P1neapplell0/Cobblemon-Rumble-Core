package com.p1nero.cobblemon.rumblecore.prologue;

import com.nbp.cobblemon_smartphone.item.SmartphoneItem;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.client.quest.OpeningTutorialOverlay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.registry.QuestRegistry;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

/** A UI walkthrough only. Progress never gates items, travel or quests. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class OpeningTutorial {
    private static final String PROGRESS = "rumble.opening_tutorial.step";

    public enum Step {
        INACTIVE, JOURNAL, MAIN_GROUP, MAIN_QUEST, TRACK, CLOSE_JOURNAL,
        HOLD_PHONE, OPEN_PHONE, CLOUD, WAYSTONES, POKEMON_INFO, DISTRIBUTION, COMPLETE;

        private static final Step[] STEPS = values();

        public static Step fromId(int id) {
            return id >= 0 && id < STEPS.length ? STEPS[id] : INACTIVE;
        }

        public boolean active() { return this != INACTIVE && this != COMPLETE; }
    }

    private OpeningTutorial() { }

    static void start(ServerPlayer player) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (data.getVariable(PROGRESS) != Step.INACTIVE.ordinal()) return;
        data.setVariable(PROGRESS, Step.JOURNAL.ordinal());
        sync(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    private static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new State(ArcQuestPlayerManager.getOrCreate(player).getVariable(PROGRESS)));
    }

    private static void acknowledge(ServerPlayer player, int completed) {
        var step = Step.fromId(completed);
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (!step.active() || data.getVariable(PROGRESS) != completed || !data.isQuestCompleted(PrologueStory.ID)) return;
        if (step == Step.TRACK) {
            var tracked = data.getTrackedQuestId();
            var definition = tracked == null ? null : QuestRegistry.get(tracked);
            if (definition == null || !QuestCategory.ARCHON.equals(definition.getCategory()) || !data.isQuestActive(tracked)) return;
        }
        if ((step == Step.HOLD_PHONE || step == Step.OPEN_PHONE)
                && !(player.getMainHandItem().getItem() instanceof SmartphoneItem)) return;
        data.setVariable(PROGRESS, completed + 1);
        sync(player);
    }

    public record State(int step) implements CustomPacketPayload {
        static final Type<State> TYPE = new Type<>(PrologueWorld.id("opening_tutorial_state"));
        static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, State::step, State::new);
        @Override public Type<State> type() { return TYPE; }
    }

    public record Acknowledge(int step) implements CustomPacketPayload {
        static final Type<Acknowledge> TYPE = new Type<>(PrologueWorld.id("opening_tutorial_step"));
        static final StreamCodec<RegistryFriendlyByteBuf, Acknowledge> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Acknowledge::step, Acknowledge::new);
        @Override public Type<Acknowledge> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(State.TYPE, State.CODEC, (packet, context) -> OpeningTutorialOverlay.sync(packet.step()));
        registrar.playToServer(Acknowledge.TYPE, Acknowledge.CODEC,
                (packet, context) -> acknowledge((ServerPlayer) context.player(), packet.step()));
    }
}
