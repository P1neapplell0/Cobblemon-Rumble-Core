package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.quest.RewardNotice;
import net.minecraft.world.item.ItemStack;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog;
import com.p1nero.cobblemon.rumblecore.gym.GymTeamPokemon;
import com.p1nero.cobblemon.rumblecore.guide.RumbleGuides;
import com.p1nero.cobblemon.rumblecore.quest.CobbleDollarReward;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.arcadia.arc_quest.api.event.dialogue.DialogueEndedEvent;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.dialogue.api.DialogueContext;
import org.arcadia.arc_quest.dialogue.api.DialogueText;
import org.arcadia.arc_quest.dialogue.builder.DialogueTreeBuilder;
import org.arcadia.arc_quest.dialogue.runtime.DialogueSessionManager;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.api.QuestCompletionPolicy;
import org.arcadia.arc_quest.quest.api.QuestText;
import org.arcadia.arc_quest.quest.builder.ObjectiveBuilder;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;
import org.arcadia.arc_quest.quest.builder.QuestBuilder;
import org.arcadia.arc_quest.quest.logic.QuestProgressHandler;
import org.arcadia.arc_quest.quest.reward.ItemReward;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class ProfessorRestock {
    static final ResourceLocation ID = PrologueWorld.id("professor_restock");
    public static final String TREE = PrologueWorld.id("professor_restock").toString();
    public static final String JESSIE = "rumble.gym.actor.restock_jessie", JAMES = "rumble.gym.actor.restock_james";
    public static final String DUO = "rumble.restock.duo", MEOWTH = "rumble.restock.meowth";
    private static final String PREFIX = "rumble.restock.";
    private static final int BATTLE_REWARD = 4_000;
    private static final String ASK = "request", FIND = "find_tumblestone", BATTLE = "rocket_battle", RETURN = "training_supplies";
    private static final String[] STONES = {"tumblestone", "black_tumblestone", "sky_tumblestone"};
    private static final String[] POWERS = {"power_weight", "power_bracer", "power_belt", "power_lens", "power_band", "power_anklet"};
    private static final GymCatalog.Actor JESSIE_ACTOR = new GymCatalog.Actor("restock_jessie", BlockPos.ZERO, GymTeamPokemon::jessie);
    private static final GymCatalog.Actor JAMES_ACTOR = new GymCatalog.Actor("restock_james", BlockPos.ZERO, GymTeamPokemon::james);
    private static final Map<ServerPlayer, Integer> RESULT = new WeakHashMap<>();
    private static final ArenaMatch.StoryChallenge CHALLENGE = new ArenaMatch.StoryChallenge() {
        @Override public List<GymCatalog.Actor> actors() { return List.of(JESSIE_ACTOR, JAMES_ACTOR); }
        @Override public boolean eligible(ServerPlayer player) {
            // During ArenaMatch setup the player has already been transferred into the instance.
            return inPhase(player, BATTLE);
        }
        @Override public void result(ServerPlayer player, boolean won) {
            if (won && inPhase(player, BATTLE)) {
                advance(player, BATTLE);
                if (inPhase(player, RETURN)) RESULT.put(player, player.server.getTickCount() + 10);
            }
        }
    };

    private ProfessorRestock() {}

    static void register(ArcQuestRegistrationEvent.Quest event) {
        var quest = SquadQuest.sound(QuestBuilder.create(ID)).category(QuestCategory.ADVENTURE)
                .themeColor(SquadQuest.SIDE_THEME).sortOrder(1).icon(item("tumblestone")).cannotAbandon().canBeAutoTrack(false)
                .requiresQuest(FirstBadgeVisit.ID).displayName(QuestText.translatable(PREFIX + "title"))
                .description(QuestText.translatable(PREFIX + "description"))
                .completionPolicy(QuestCompletionPolicy.SPECIFIC_PHASE).completionTargetPhase(RETURN)
                .phase(phase(ASK).thenGoTo(FIND))
                .phase(phase(FIND).thenGoTo(BATTLE))
                .phase(phase(BATTLE).reward(new CobbleDollarReward(BATTLE_REWARD)).thenGoTo(RETURN))
                .phase(phase(RETURN).grantGuideOnComplete(RumbleGuides.EFFORT_VALUES));
        quest.reward(new ItemReward(item("ancient_origin_ball"), 1));
        for (String power : POWERS) quest.reward(new ItemReward(item(power), 1));
        event.register(quest.build());
        event.assignQuestToGroup(ID, PrologueWorld.id("adventure_quests"));
    }

    private static PhaseBuilder phase(String phase) {
        return PhaseBuilder.create(phase).displayName(QuestText.translatable(PREFIX + "phase." + phase))
                .objective(ObjectiveBuilder.custom(ID.withSuffix("/" + phase), 1)
                        .display(QuestText.translatable(PREFIX + "objective." + phase)))
                .trackingMarker(PrologueStory.PROFESSOR_MARK);
    }

    static void register(ArcQuestRegistrationEvent.Dialogue event) {
        var request = tree("request");
        line(request, "ask", null, DialogueText.DialogueArg.playerName())
                .choice("what", text("what"), c -> c.goTo("explain"));
        line(request, "explain", null).choice("where", text("where"), c -> c.goTo("location"));
        line(request, "location", null).delay(3250).autoNext("accepted");
        line(request, "accepted", null).choice("accept", text("accept"), c -> c
                .addEvents((p, npc) -> { if (PrologueStory.atDialogueProfessor(p)) advance(p, ASK); }).close());
        event.register(request.build());

        var delivery = tree("delivery");
        delivery.node("offer").say(text("offer"), TREE + "/offer")
                .choice("stone", option(), c -> c.conditionIf((p, npc) -> stoneSlot(p) >= 0)
                        .addEvents((p, npc) -> {
                            if (deliver(p)) PrologueStory.after(p, () -> openAtProfessor(p, "arrival"));
                        }).close())
                .choice("where", option(), c -> c.conditionIf((p, npc) -> stoneSlot(p) < 0).goTo("location"));
        line(delivery, "location", null).choice("close", text("close"), c -> c.close());
        event.register(delivery.build());

        var arrival = tree("arrival");
        line(arrival, "perfect", null).delay(1850).autoNext("jessie_stop");
        rocketLines(arrival);
        event.register(arrival.build());

        var retry = tree("retry");
        rocketLines(retry);
        event.register(retry.build());

        var victory = tree("victory");
        line(victory, "duo_retreat", DUO).delay(2150).autoNext("coins");
        victory.node("coins").speaker(DialogueText.translatable(DUO))
                .say(DialogueText.translatable("rumble.gym.reward.coins", DialogueText.DialogueArg.of(ctx -> BATTLE_REWARD)),
                        TREE + "/coins").delay(1400).autoNext("thanks");
        giftLines(victory);
        event.register(victory.build());

        var gift = tree("gift");
        giftLines(gift);
        event.register(gift.build());
    }

    private static void rocketLines(DialogueTreeBuilder tree) {
        line(tree, "jessie_stop", JESSIE).choice("who", text("who"), c -> c.goTo("jessie_1"));
        beat(tree, "jessie_1", JESSIE, "james_1", 2550);
        beat(tree, "james_1", JAMES, "jessie_2", 2600);
        beat(tree, "jessie_2", JESSIE, "james_2", 2300);
        beat(tree, "james_2", JAMES, "jessie_3", 2350);
        beat(tree, "jessie_3", JESSIE, "james_3", 2350);
        beat(tree, "james_3", JAMES, "jessie_name", 2900);
        beat(tree, "jessie_name", JESSIE, "james_name", 1250);
        beat(tree, "james_name", JAMES, "duo_motto", 1300);
        beat(tree, "duo_motto", DUO, "meowth", 4700);
        tree.node("meowth").speaker(DialogueText.translatable(MEOWTH)).say(text("line_meowth"), TREE + "/meowth")
                .delay(2250).autoNext("help");
        line(tree, "help", null, DialogueText.DialogueArg.playerName()).choice("battle", text("battle"), c -> c
                .conditionIf((p, npc) -> inPhase(p, BATTLE))
                .addEvents((p, npc) -> {
                    if (PrologueStory.atDialogueProfessor(p)) PrologueStory.after(p, () -> {
                        if (PrologueStory.nearby(p) != null) ArenaMatch.challengeStory(p, CHALLENGE);
                    });
                }).close());
    }

    private static void giftLines(DialogueTreeBuilder tree) {
        line(tree, "thanks", null, DialogueText.DialogueArg.playerName()).delay(2750).autoNext("ball");
        beat(tree, "ball", null, "ball_received", 4200);
        tree.node("ball_received").say(RewardNotice.items(new ItemStack(item("ancient_origin_ball"))), TREE + "/ball_received")
                .delay(1900).autoNext("tools");
        beat(tree, "tools", null, "tools_received", 3000);
        tree.node("tools_received").say(RewardNotice.items(java.util.Arrays.stream(POWERS).map(power -> new ItemStack(item(power)))
                .toArray(ItemStack[]::new)), TREE + "/tools_received").choice("take", text("take"), c -> c
                .addEvents((p, npc) -> {
                    if (PrologueStory.atDialogueProfessor(p) && inPhase(p, RETURN)) advance(p, RETURN);
                }).close());
    }

    private static void beat(DialogueTreeBuilder tree, String node, String speaker, String next, int delay) {
        line(tree, node, speaker).delay(delay).autoNext(next);
    }

    private static DialogueTreeBuilder line(DialogueTreeBuilder tree, String node, String speaker, DialogueText.DialogueArg... args) {
        tree.node(node);
        if (speaker != null) tree.speaker(DialogueText.translatable(speaker));
        return tree.say(text(node, args), TREE + "/" + node);
    }

    private static DialogueTreeBuilder tree(String suffix) {
        return DialogueTreeBuilder.create(TREE + "/" + suffix)
                .npc(DialogueText.translatable("entity.cobblemon_rumble_core.professor")).repeatable(true);
    }

    private static DialogueText text(String line, DialogueText.DialogueArg... args) { return DialogueText.translatable(PREFIX + line, args); }
    static DialogueText option() { return DialogueText.of(ctx -> Component.translatable(PREFIX + "about").withStyle(ChatFormatting.GOLD)); }

    static void accept(ServerPlayer player) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (data.isQuestCompleted(FirstBadgeVisit.ID.toString()) && !data.isQuestActive(ID.toString()) && !data.isQuestCompleted(ID.toString()))
            QuestProgressHandler.acceptQuest(player, ID.toString());
    }

    static boolean active(ServerPlayer player) { return ArcQuestPlayerManager.getOrCreate(player).isQuestActive(ID.toString()); }

    private static boolean inPhase(ServerPlayer player, String phase) {
        var runtime = ArcQuestPlayerManager.getOrCreate(player).getActiveQuest(ID.toString());
        return runtime != null && runtime.isPhaseActive(phase);
    }

    private static void advance(ServerPlayer player, String phase) {
        QuestProgressHandler.incrementObjective(player, ID.toString(), phase, 0, 1);
    }

    private static net.minecraft.world.item.Item item(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("cobblemon", path));
    }

    private static int stoneSlot(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            for (String stone : STONES) if (stack.is(item(stone))) return slot;
        }
        return -1;
    }

    private static boolean deliver(ServerPlayer player) {
        if (!PrologueStory.atDialogueProfessor(player) || !inPhase(player, FIND)) return false;
        int slot = stoneSlot(player);
        if (slot < 0) return false;
        var removed = player.getInventory().removeItem(slot, 1);
        if (removed.isEmpty()) return false;
        advance(player, FIND);
        if (inPhase(player, BATTLE)) return true;
        if (!player.addItem(removed)) player.drop(removed, false);
        return false;
    }

    static void open(ServerPlayer player) {
        String route = inPhase(player, ASK) ? "request" : inPhase(player, FIND) ? "delivery"
                : inPhase(player, BATTLE) ? "retry" : inPhase(player, RETURN) ? "gift" : null;
        if (route != null) openAtProfessor(player, route);
    }

    private static void openAtProfessor(ServerPlayer player, String route) {
        var doctor = PrologueStory.nearby(player);
        if (doctor != null) DialogueSessionManager.INSTANCE.startDialogue(player, doctor, TREE + "/" + route, new DialogueContext());
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Integer at = RESULT.get(player);
        if (at == null || player.server.getTickCount() < at) return;
        if (!inPhase(player, RETURN) || player.server.getTickCount() > at + 160) {
            RESULT.remove(player);
            return;
        }
        var doctor = PrologueStory.nearby(player);
        if (doctor == null || DialogueSessionManager.INSTANCE.isInDialogue(player)) return;
        RESULT.remove(player);
        DialogueSessionManager.INSTANCE.startDialogue(player, doctor, TREE + "/victory", new DialogueContext());
    }

    @SubscribeEvent
    public static void ended(DialogueEndedEvent event) {
        if (event.getDialogueId().startsWith(TREE + "/")) RestockScene.clear(event.getPlayer(), false);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) RESULT.remove(player);
    }

    static GymCatalog.Actor jessie() { return JESSIE_ACTOR; }
    static GymCatalog.Actor james() { return JAMES_ACTOR; }
}
