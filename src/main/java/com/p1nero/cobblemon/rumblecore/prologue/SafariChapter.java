package com.p1nero.cobblemon.rumblecore.prologue;

import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.item.components.TMMoveComponent;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.quest.RewardNotice;
import maxigregrze.cobblesafari.config.SafariTimerConfig;
import maxigregrze.cobblesafari.network.ModNetworking;
import com.p1nero.cobblemon.rumblecore.mixin.cobblesafari.SafariTeleporterAccess;
import maxigregrze.cobblesafari.teleporter.TeleporterTickHandler;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.dialogue.api.DialogueText;
import org.arcadia.arc_quest.dialogue.builder.DialogueTreeBuilder;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.api.QuestCompletionPolicy;
import org.arcadia.arc_quest.quest.api.QuestText;
import org.arcadia.arc_quest.quest.builder.ObjectiveBuilder;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;
import org.arcadia.arc_quest.quest.builder.QuestBuilder;
import org.arcadia.arc_quest.quest.logic.QuestProgressHandler;
import org.arcadia.arc_quest.quest.reward.ItemReward;
import org.arcadia.arc_quest.questmarker.api.MarkSpec;
import org.arcadia.arc_quest.questmarker.api.MarkableObject;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class SafariChapter {
    public static final String ROUTE = "safari_briefing";
    public static final ResourceLocation SIDE = PrologueWorld.id("safari_zone");
    private static final String KEY = "rumble.safari.", UNLOCKED = KEY + "unlocked", SMS = KEY + "sms_sent";
    private static final String ENTER = "enter", CAPTURE = "capture";
    private static final Map<ServerPlayer, Integer> TERMINAL_PROMPTS = new WeakHashMap<>();

    private SafariChapter() {}

    public static boolean unlocked(ServerPlayer player) { return ArcQuestPlayerManager.getOrCreate(player).hasFlag(UNLOCKED); }
    static boolean active(ServerPlayer player) { return GymStory.phaseActive(player, GymStory.fourthGymQuest(), ROUTE); }

    public static PhaseBuilder briefing() {
        return PhaseBuilder.create(ROUTE).displayName(QuestText.translatable(KEY + "briefing"))
                .objective(ObjectiveBuilder.custom(PrologueWorld.id(ROUTE), 1).display(QuestText.translatable(KEY + "briefing")))
                .trackingMarker(PrologueStory.PROFESSOR_MARK).setFlagOnComplete(UNLOCKED);
    }

    static void register(ArcQuestRegistrationEvent.Quest event) {
        var pos = MegaSimulator.POS;
        var marker = MarkSpec.translated("safari_simulator", "rumble.mega.simulator.title",
                new MarkableObject.DimensionPos(PrologueWorld.GYMS, pos.getX(), pos.getY() + 1, pos.getZ()));
        event.register(SquadQuest.sound(QuestBuilder.create(SIDE)).category(QuestCategory.ADVENTURE).sortOrder(4)
                .displayName(QuestText.translatable(KEY + "title")).description(QuestText.translatable(KEY + "description"))
                .themeColor(SquadQuest.SIDE_THEME).icon(RewardNotice.stack("cobblesafari:wildegg", 1).getItem())
                .requiresQuest(GymStory.fourthGymQuest().toString()).cannotAbandon().canBeAutoTrack(false)
                .completionPolicy(QuestCompletionPolicy.SPECIFIC_PHASE).completionTargetPhase(CAPTURE)
                .phase(objective(ENTER).trackingMarker(marker).thenGoTo(CAPTURE))
                .phase(objective(CAPTURE).reward(new ItemReward(RewardNotice.stack("cobblesafari:wildegg", 1).getItem(), 1))).build());
    }

    private static PhaseBuilder objective(String phase) {
        var text = QuestText.translatable(KEY + phase);
        return PhaseBuilder.create(phase).displayName(text).objective(ObjectiveBuilder.custom(PrologueWorld.id("safari/" + phase), 1).display(text));
    }

    public static ItemStack tm() {
        return TMMoveComponent.Companion.createStack(Moves.INSTANCE.getByNameOrDummy("icebeam"));
    }

    public static void iceGift(DialogueTreeBuilder tree, String id, java.util.function.Consumer<ServerPlayer> finish) {
        line(tree, id, "stop", "reassure", 1450);
        line(tree, id, "reassure", "praise", 3000);
        line(tree, id, "praise", "offer", 2250);
        line(tree, id, "offer", "teach", 2250);
        line(tree, id, "teach", "bench", 4300);
        line(tree, id, "bench", "received", 3800);
        // Resolve the move only after Cobblemon has loaded its data, not at dialogue registration.
        tree.node("received").say(DialogueText.of(ctx -> RewardNotice.component(tm())), id + "/received")
                .choice("take", text("take"), choice -> choice.addEvents((p, npc) -> finish.accept(p)).close());
    }

    private static void line(DialogueTreeBuilder tree, String id, String node, String next, int delay) {
        tree.node(node).say(text(node), id + "/" + node).delay(delay).autoNext(next);
    }

    static void register(ArcQuestRegistrationEvent.Dialogue event) {
        var tree = PrologueStory.tree(ROUTE).repeatable(true);
        tree.node("welcome").say(DialogueText.translatable(KEY + "welcome", DialogueText.DialogueArg.playerName()), ROUTE + "/welcome")
                .choice("ask", text("ask"), c -> c.goTo("rules"));
        line(tree, ROUTE, "rules", "fee", 4650);
        tree.node("fee").say(text("fee"), ROUTE + "/fee")
                .choice("finish", text("understood"), c -> c.addEvents((p, npc) -> {
                    if (!active(p) || !PrologueStory.atDialogueProfessor(p)) return;
                    QuestProgressHandler.incrementObjective(p, GymStory.fourthGymQuest().toString(), ROUTE, 0, 1);
                    if (!unlocked(p)) return;
                    GymStory.acceptNext(p);
                    QuestProgressHandler.acceptQuest(p, SIDE.toString());
                    WanderingMerchantQuest.accept(p);
                }).close());
        event.register(tree.build());
    }

    private static DialogueText text(String path) { return DialogueText.translatable(KEY + path); }

    static void openAdmission(ServerPlayer player) {
        if (!mayUseTerminal(player)) return;
        var id = player.getUUID();
        // A deliberate click can reopen a prompt the player previously declined.
        SafariTeleporterAccess.rumble$declined().remove(id);
        TeleporterTickHandler.updatePlayerOnTeleporter(player);
        if (SafariTeleporterAccess.rumble$pending().contains(id)) {
            TERMINAL_PROMPTS.put(player, player.server.getTickCount() + 20 * 45);
        }
    }

    /** Native teleporter confirmations still work without a simulator session. */
    public static boolean checkTerminalResponse(ServerPlayer player) {
        var expiry = TERMINAL_PROMPTS.remove(player);
        if (expiry == null || player.server.getTickCount() < expiry && mayUseTerminal(player)) return true;
        cancelAdmission(player);
        return false;
    }

    private static void cancelAdmission(ServerPlayer player) {
        TeleporterTickHandler.cancelTeleport(player);
        ModNetworking.sendCloseTpAccept(player);
    }

    private static boolean mayUseTerminal(ServerPlayer p) {
        return unlocked(p) && MegaSimulator.atTerminal(p) && com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch.available(p);
    }

    private static ResourceKey<Level> dimension() {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(SafariTimerConfig.getSafariDimensionId()));
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (active(p) && p.tickCount % 20 == 0 && !org.arcadia.arc_quest.dialogue.runtime.DialogueSessionManager.INSTANCE.isInDialogue(p)) {
            var data = ArcQuestPlayerManager.getOrCreate(p);
            if (!data.hasFlag(SMS)) {
                data.setFlag(SMS);
                org.arcadia.arc_quest.quest.network.QuestSyncCoordinator.persistAndSyncIfChanged(p, data);
                if (ModList.get().isLoaded("mine_chat")) ProfessorStockMessage.sendSafari(p);
            }
        }
        Integer timeout = TERMINAL_PROMPTS.get(p);
        if (timeout == null) return;
        if (p.server.getTickCount() >= timeout || !mayUseTerminal(p)) {
            TERMINAL_PROMPTS.remove(p);
            cancelAdmission(p);
        } else SafariTerminalPresence.keepAlive(p);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (TERMINAL_PROMPTS.remove(p) != null) TeleporterTickHandler.cancelTeleport(p);
    }

    @SubscribeEvent
    public static void enter(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getTo().equals(dimension())) {
            TERMINAL_PROMPTS.remove(p);
            if (GymStory.phaseActive(p, SIDE, ENTER)) QuestProgressHandler.incrementObjective(p, SIDE.toString(), ENTER, 0, 1);
        }
    }

    static void captured(ServerPlayer p, Level level) {
        if (level.dimension().equals(dimension()) && GymStory.phaseActive(p, SIDE, CAPTURE)) {
            QuestProgressHandler.incrementObjective(p, SIDE.toString(), CAPTURE, 0, 1);
        }
    }
}
