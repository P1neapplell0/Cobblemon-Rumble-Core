package com.p1nero.cobblemon.rumblecore.prologue;

import com.nbp.cobblemon_smartphone.item.SmartphoneItem;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.johnn.realtimecobblemon.Smash;
import net.minecraft.network.chat.Component;
import java.util.Map;
import java.util.WeakHashMap;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.arcadia.arc_quest.dialogue.runtime.DialogueSessionManager;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.client.quest.OpeningTutorialOverlay;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
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
    private static final String CAPTURE = "rumble.opening_tutorial.capture";
    private static final String CANDY = "rumble.opening_tutorial.candy";
    private static final int PENDING = 1, STARTED = 2, DONE = 3;
    private static final Map<ServerPlayer, Step> REPLAYS = new WeakHashMap<>();

    public enum Step {
        INACTIVE, JOURNAL, MAIN_GROUP, MAIN_QUEST, TRACK, CLOSE_JOURNAL,
        HOLD_PHONE, OPEN_PHONE, CLOUD, WAYSTONES, POKEMON_INFO, DISTRIBUTION,
        COMPLETE, CAPTURE_SKILLS, CANDY;

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

    public static void initialize() {
        CobblemonEvents.EXPERIENCE_CANDY_USE_POST.subscribe(event -> {
            if (event.getItem() == CobblemonItems.EXPERIENCE_CANDY_S && event.wasExperienceGiven()) {
                finish(event.getPlayer(), Step.CANDY, CANDY, "candy_opened");
            }
        });
    }

    static void startCaptureSkills(ServerPlayer player) { queue(player, CAPTURE); }

    static void startCandy(ServerPlayer player) { queue(player, CANDY); }

    private static void queue(ServerPlayer player, String key) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (data.getVariable(key) == 0) data.setVariable(key, PENDING);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
        var data = ArcQuestPlayerManager.get(player);
        if (data == null) return;
        boolean candyPending = data.getVariable(CANDY) == PENDING;
        boolean capturePending = data.getVariable(CAPTURE) == PENDING;
        if ((!candyPending && !capturePending) || DialogueSessionManager.INSTANCE.isInDialogue(player)) return;
        if (capturePending) data.setVariable(CAPTURE, STARTED);
        if (candyPending) {
            data.setVariable(CANDY, STARTED);
            giveCandy(player);
        }
        sync(player);
    }

    private static void giveCandy(ServerPlayer player) {
        player.getInventory().placeItemBackInInventory(new ItemStack(CobblemonItems.EXPERIENCE_CANDY_S));
    }

    private static int replay(ServerPlayer player, Step step) {
        REPLAYS.put(player, step);
        if (step == Step.CANDY) giveCandy(player);
        sync(player);
        return 1;
    }

    /** Called by the native Cobblemon BenchMoveHandler after exchangeMove succeeds. */
    public static void moveChanged(ServerPlayer player) {
        if (!Smash.active(player) && BattleRegistry.getBattleByParticipatingPlayer(player) == null) {
            finish(player, Step.CAPTURE_SKILLS, CAPTURE, "capture_skills_opened");
        }
    }

    private static void finish(ServerPlayer player, Step step, String key, String message) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        var replay = REPLAYS.get(player);
        if (replay == step) REPLAYS.remove(player);
        else if (replay == null && current(player) == step) data.setVariable(key, DONE);
        else return;
        player.displayClientMessage(Component.translatable("rumble.opening_tutorial." + message), false);
        sync(player);
        if (step == Step.CANDY) PacketDistributor.sendToPlayer(player, new CandyComplete());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rumble").then(Commands.literal("tutorial")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("capture").executes(ctx -> replay(ctx.getSource().getPlayerOrException(), Step.CAPTURE_SKILLS)))
                .then(Commands.literal("candy").executes(ctx -> replay(ctx.getSource().getPlayerOrException(), Step.CANDY)))));
    }

    private static Step current(ServerPlayer player) {
        var replay = REPLAYS.get(player);
        if (replay != null) return replay;
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (data.getVariable(CANDY) == STARTED) return Step.CANDY;
        if (data.getVariable(CAPTURE) == STARTED) return Step.CAPTURE_SKILLS;
        var opening = Step.fromId(data.getVariable(PROGRESS));
        return opening.ordinal() <= Step.COMPLETE.ordinal() ? opening : Step.COMPLETE;
    }

    private static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new State(current(player).ordinal()));
    }

    private static void acknowledge(ServerPlayer player, int completed) {
        var step = Step.fromId(completed);
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (step.ordinal() >= Step.COMPLETE.ordinal() || current(player) != step) return;
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

    public record CandyComplete() implements CustomPacketPayload {
        static final Type<CandyComplete> TYPE = new Type<>(PrologueWorld.id("opening_tutorial_candy_complete"));
        static final StreamCodec<RegistryFriendlyByteBuf, CandyComplete> CODEC = StreamCodec.unit(new CandyComplete());
        @Override public Type<CandyComplete> type() { return TYPE; }
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
        registrar.playToClient(CandyComplete.TYPE, CandyComplete.CODEC, (packet, context) -> OpeningTutorialOverlay.candyComplete());
        registrar.playToServer(Acknowledge.TYPE, Acknowledge.CODEC,
                (packet, context) -> acknowledge((ServerPlayer) context.player(), packet.step()));
    }
}
