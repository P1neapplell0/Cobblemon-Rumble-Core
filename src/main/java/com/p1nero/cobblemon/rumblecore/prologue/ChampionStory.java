package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.arena.HallOfFame;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.misc.custom_instances.network.InstanceNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.dialogue.api.DialogueText;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.api.QuestText;
import org.arcadia.arc_quest.quest.builder.ObjectiveBuilder;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;
import org.arcadia.arc_quest.quest.builder.QuestBuilder;
import org.arcadia.arc_quest.quest.logic.QuestProgressHandler;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

public final class ChampionStory {
    public static final String TEMPLATE = "rumble_champion";
    public static final ResourceLocation ID = PrologueWorld.id("champion_battle");
    private static final ResourceLocation FINALE = PrologueWorld.id("champion_finale");
    private static final String MEET = "meet", BATTLE = "battle", CEREMONY = "ceremony", EPILOGUE = "epilogue";
    private static final String KEY = "rumble.champion.";
    public static final String BRIEFING = "champion_briefing", RETRY = "champion_retry", RETURN = "champion_return";

    private ChampionStory() {}

    static void register(ArcQuestRegistrationEvent.Quest event) {
        event.register(main(ID, 90, "title").requiresQuest(GymStory.eighthGymQuest())
                .icon(Items.NETHER_STAR).description(QuestText.translatable(KEY + "description"))
                .phase(phase(ID, MEET).trackingMarker(PrologueStory.PROFESSOR_MARK).thenGoTo(BATTLE))
                .phase(phase(ID, BATTLE).trackingMarker(PrologueStory.PROFESSOR_MARK).thenGoTo(CEREMONY))
                .phase(phase(ID, CEREMONY).trackingMarker(PrologueStory.PROFESSOR_MARK)).build());
        event.register(main(FINALE, 91, "finale.title").requiresQuest(ID).icon(Items.FIREWORK_ROCKET)
                .description(QuestText.translatable(KEY + "finale.description"))
                .phase(phase(FINALE, EPILOGUE)).build());
        var group = PrologueWorld.id("champion_road");
        event.assignQuestToGroup(ID, group);
        event.assignQuestToGroup(FINALE, group);
    }

    private static QuestBuilder main(ResourceLocation id, int order, String title) {
        return SquadQuest.sound(QuestBuilder.create(id)).category(QuestCategory.ARCHON).sortOrder(order)
                .displayName(QuestText.translatable(KEY + title)).themeColor(ChatFormatting.GOLD).cannotAbandon();
    }

    private static PhaseBuilder phase(ResourceLocation quest, String id) {
        var name = QuestText.translatable(KEY + "phase." + id);
        return PhaseBuilder.create(id).displayName(name)
                .objective(ObjectiveBuilder.custom(quest.withSuffix("/" + id), 1).display(name));
    }

    static void register(ArcQuestRegistrationEvent.Dialogue event) {
        var intro = PrologueStory.tree(BRIEFING).repeatable(true);
        intro.node("welcome").say(line("welcome", DialogueText.DialogueArg.playerName()), node(BRIEFING, "welcome"))
                .choice("badges", line("badges"), choice -> choice.goTo("challenge"));
        intro.node("challenge").say(line("challenge"), node(BRIEFING, "challenge"))
                .choice("elite", line("elite"), choice -> choice.goTo("budget"));
        intro.node("budget").say(line("budget"), node(BRIEFING, "budget")).delay(5400).autoNext("ready");
        intro.node("ready").say(line("ready"), node(BRIEFING, "ready"))
                .choice("fight", gold("fight"), choice -> choice.addEvents((player, npc) -> prepare(player, true)).close())
                .choice("later", line("later"), choice -> choice.addEvents((player, npc) -> prepare(player, false)).close());
        event.register(intro.build());

        var retry = PrologueStory.tree(RETRY).repeatable(true);
        retry.node("ready").say(line("retry", DialogueText.DialogueArg.playerName()), node(RETRY, "ready"))
                .choice("fight", gold("fight"), choice -> choice.addEvents((player, npc) -> challenge(player)).close())
                .choice("later", line("later"), choice -> choice.close());
        event.register(retry.build());

        var victory = PrologueStory.tree(RETURN).repeatable(true);
        victory.node("congratulations").say(line("congratulations"), node(RETURN, "congratulations"))
                .choice("ceremony", line("ceremony"), choice -> choice.addEvents((player, npc) -> {
                    if (awaitingCeremony(player) && PrologueStory.atDialogueProfessor(player)) {
                        PrologueStory.after(player, () -> HallOfFame.open(player));
                    }
                }).close());
        event.register(victory.build());
    }

    private static void prepare(ServerPlayer player, boolean fight) {
        if (!meeting(player) || !PrologueStory.atDialogueProfessor(player)) return;
        advance(player, MEET);
        if (fight) PrologueStory.after(player, () -> ArenaMatch.challengeChampion(player));
    }

    static void challenge(ServerPlayer player) {
        if (eligible(player) && PrologueStory.atDialogueProfessor(player)) {
            PrologueStory.after(player, () -> ArenaMatch.challengeChampion(player));
        }
    }

    static void rankings(ServerPlayer player) {
        if ((awaitingCeremony(player) || completed(player)) && PrologueStory.atDialogueProfessor(player)) {
            PrologueStory.after(player, () -> InstanceNetwork.openInformation(player, TEMPLATE, "players"));
        }
    }

    static boolean meeting(ServerPlayer player) { return GymStory.phaseActive(player, ID, MEET); }
    public static boolean awaitingCeremony(ServerPlayer player) { return GymStory.phaseActive(player, ID, CEREMONY); }
    public static boolean completed(ServerPlayer player) { return ArcQuestPlayerManager.getOrCreate(player).isQuestCompleted(ID.toString()); }
    static boolean preparing(ServerPlayer player) { return GymStory.phaseActive(player, ID, BATTLE); }
    public static boolean eligible(ServerPlayer player) { return preparing(player) || completed(player); }
    public static void offer(ServerPlayer player) { QuestProgressHandler.acceptQuest(player, ID.toString()); }

    static String trackedRoute(ServerPlayer player) {
        if (!ID.toString().equals(ArcQuestPlayerManager.getOrCreate(player).getTrackedQuestId())) return null;
        return activeRoute(player);
    }

    static String activeRoute(ServerPlayer player) {
        if (awaitingCeremony(player)) return RETURN;
        if (meeting(player)) return BRIEFING;
        if (preparing(player)) return RETRY;
        return null;
    }

    public static void won(ServerPlayer player) {
        if (GymStory.phaseActive(player, ID, BATTLE)) advance(player, BATTLE);
    }

    public static void returned(ServerPlayer player) {
        if (!awaitingCeremony(player) || player.hasDisconnected()) return;
        PrologueStory.after(player, () -> {
            var professor = PrologueStory.nearby(player);
            if (professor != null && player.containerMenu == player.inventoryMenu && awaitingCeremony(player)) {
                PrologueStory.open(player, professor, RETURN);
            }
        });
    }

    public static void finishCeremony(ServerPlayer player) {
        if (!awaitingCeremony(player)) return;
        advance(player, CEREMONY);
        if (completed(player)) QuestProgressHandler.acceptQuest(player, FINALE.toString());
    }

    private static void advance(ServerPlayer player, String phase) {
        QuestProgressHandler.incrementObjective(player, ID.toString(), phase, 0, 1);
    }

    static DialogueText gold(String key) {
        return DialogueText.of(ctx -> Component.translatable(KEY + key).withStyle(ChatFormatting.GOLD));
    }

    private static String node(String tree, String name) { return PrologueStory.rId(tree + "/" + name); }
    private static DialogueText line(String name, DialogueText.DialogueArg... args) { return DialogueText.translatable(KEY + name, args); }
}
