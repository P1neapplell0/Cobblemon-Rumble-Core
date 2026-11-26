package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.quest.RewardNotice;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import org.arcadia.arc_quest.api.event.dialogue.DialogueEndedEvent;
import org.arcadia.arc_quest.api.event.dialogue.DialogueNodeStartedEvent;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.dialogue.api.DialogueText;
import org.arcadia.arc_quest.quest.api.QuestText;
import org.arcadia.arc_quest.quest.builder.ObjectiveBuilder;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;
import org.arcadia.arc_quest.quest.logic.QuestProgressHandler;
import org.arcadia.arc_quest.quest.reward.ItemReward;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class FirstBadgeVisit {
    public static final String ROUTE = "first_badge";
    public static final ResourceLocation ID = GymStory.firstGymQuest();
    private static final String TRADE_SHOWN = "rumble.professor.trade_intro_shown";
    private static final String SMS_TRIED = "rumble.professor.stock_sms";
    private static final String RECEIVED_BOX = "rumble.professor.badge_box";
    private static final String PREFIX = "rumble.first_badge.";

    private FirstBadgeVisit() {}

    public static PhaseBuilder phase() {
        return PhaseBuilder.create(ROUTE).displayName(QuestText.translatable(PREFIX + "phase"))
                .objective(ObjectiveBuilder.custom(PrologueWorld.id(ROUTE), 1).display(QuestText.component(Component.translatable(
                        PREFIX + "objective", Component.keybind("key.cobblemon_rumble_core.talk")))))
                .trackingMarker(PrologueStory.PROFESSOR_MARK).setFlagOnComplete(ProfessorShop.UNLOCKED);
    }

    static void register(ArcQuestRegistrationEvent.Dialogue event) {
        var tree = PrologueStory.tree(ROUTE).repeatable(true);
        tree.node("start").say(text("welcome", DialogueText.DialogueArg.playerName()), node("start"))
                .choice("first_badge", text("my_badge"), choice -> choice.goTo("praise"));
        tree.node("praise").say(text("praise"), node("praise")).delay(2800).autoNext("box");
        tree.node("box").say(text("box"), node("box"))
                .choice("take", text("take"), choice -> choice.addEvents((p, ignored) -> receiveBox(p)).goTo("received"));
        tree.node("received").say(RewardNotice.items(RewardNotice.stack("badgebox:badgesbox", 1)), node("received")).delay(1900).autoNext("stock");
        tree.node("stock").say(text("stock"), node("stock")).delay(2500).autoNext("trade");
        tree.node("trade").say(text("unlocked"), node("trade"))
                .choice("trade", text("trade"), choice -> choice.conditionIf((p, npc) -> canFinish(p))
                        .addEvents((p, npc) -> ProfessorShop.afterDialogue(p, false)).close())
                .choice("later", text("later"), choice -> choice.close());
        event.register(tree.build());
    }

    public static void notifyPlayer(ServerPlayer player) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (!active(player)) return;
        if (!data.hasFlag(SMS_TRIED)) {
            data.setFlag(SMS_TRIED);
            if (ModList.get().isLoaded("mine_chat")) ProfessorStockMessage.send(player);
        }
    }

    static boolean active(ServerPlayer player) { return GymStory.phaseActive(player, ID, ROUTE); }

    private static void receiveBox(ServerPlayer player) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (!active(player) || data.hasFlag(RECEIVED_BOX) || !PrologueStory.atDialogueProfessor(player)) return;
        BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse("badgebox:badgesbox")).ifPresent(item -> {
            new ItemReward(item, 1).grant(player);
            data.setFlag(RECEIVED_BOX);
        });
    }

    private static boolean canFinish(ServerPlayer player) {
        return PrologueStory.atDialogueProfessor(player) && ArcQuestPlayerManager.getOrCreate(player).hasFlag(RECEIVED_BOX);
    }

    @SubscribeEvent
    public static void tradeShown(DialogueNodeStartedEvent event) {
        if (!"trade".equals(event.getNodeId()) || !PrologueWorld.id("prologue/" + ROUTE).toString().equals(event.getDialogueId())) return;
        var player = event.getPlayer();
        if (active(player) && canFinish(player)) {
            var data = ArcQuestPlayerManager.getOrCreate(player);
            data.setFlag(TRADE_SHOWN);
            data.setFlag(ProfessorShop.UNLOCKED);
            OpeningTutorial.startCandy(player);
        }
    }

    @SubscribeEvent
    public static void dialogueEnded(DialogueEndedEvent event) {
        if (PrologueWorld.id("prologue/" + ROUTE).toString().equals(event.getDialogueId())) acceptFollowups(event.getPlayer());
    }

    static void acceptFollowups(ServerPlayer player) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (active(player) && data.hasFlag(TRADE_SHOWN)) {
            QuestProgressHandler.incrementObjective(player, ID.toString(), ROUTE, 0, 1);
        }
        if (data.isQuestCompleted(ID.toString())) {
            GymStory.acceptNext(player);
            ProfessorRestock.accept(player);
        }
    }

    static DialogueText text(String key, DialogueText.DialogueArg... args) { return DialogueText.translatable(PREFIX + key, args); }
    private static String node(String id) { return ID + "/" + id; }
}
