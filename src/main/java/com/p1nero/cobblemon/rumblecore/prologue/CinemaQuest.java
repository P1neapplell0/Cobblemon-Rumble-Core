package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.quest.CobbleDollarReward;
import com.p1nero.cobblemon.rumblecore.quest.RewardNotice;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.dialogue.api.DialogueText;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.api.QuestText;
import org.arcadia.arc_quest.quest.builder.ObjectiveBuilder;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;
import org.arcadia.arc_quest.quest.builder.QuestBuilder;
import org.arcadia.arc_quest.quest.logic.QuestProgressHandler;
import org.arcadia.arc_quest.quest.reward.ItemReward;
import org.arcadia.arc_quest.questmarker.api.MarkSpec;
import org.arcadia.arc_quest.questmarker.api.MarkableObject;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

public final class CinemaQuest {
    public static final ResourceLocation ID = PrologueWorld.id("cinema_mode");
    static final String ROUTE = "cinema_intro";
    public static final String TREE = PrologueWorld.id("prologue/" + ROUTE).toString();
    private static final String KEY = "rumble.cinema.", MEET = "meet", TEST = "test";
    private static final String PRISM = "cobblemon_dreamweaving_prism_ball:prism_ball";

    private CinemaQuest() {}

    public static void offer(ServerPlayer player) {
        if (QuestProgressHandler.acceptQuest(player, ID.toString()) && ModList.get().isLoaded("mine_chat"))
            PrologueStory.after(player, () -> ProfessorStockMessage.sendCinema(player));
    }

    public static boolean unlocked(ServerPlayer player) {
        return testing(player) || ArcQuestPlayerManager.getOrCreate(player).isQuestCompleted(ID.toString());
    }

    static boolean meeting(ServerPlayer player) { return GymStory.phaseActive(player, ID, MEET); }
    static boolean testing(ServerPlayer player) { return GymStory.phaseActive(player, ID, TEST); }

    public static void cleared(ServerPlayer player) {
        if (testing(player)) advance(player, TEST);
    }

    static void register(ArcQuestRegistrationEvent.Quest event) {
        var pos = MegaSimulator.POS;
        var terminal = MarkSpec.translated("cinema_terminal", "rumble.mega.simulator.title",
                new MarkableObject.DimensionPos(PrologueWorld.GYMS, pos.getX(), pos.getY() + 1, pos.getZ()));
        event.register(SquadQuest.sound(QuestBuilder.create(ID)).category(QuestCategory.ADVENTURE)
                .sortOrder(9).themeColor(SquadQuest.SIDE_THEME).icon(Items.MUSIC_DISC_OTHERSIDE).cannotAbandon()
                .requiresQuest(GymStory.seventhGymQuest()).displayName(QuestText.translatable(KEY + "title"))
                .description(QuestText.translatable(KEY + "description"))
                .phase(phase(MEET).trackingMarker(PrologueStory.PROFESSOR_MARK).thenGoTo(TEST))
                .phase(phase(TEST).trackingMarker(terminal))
                .reward(new CobbleDollarReward(40_000))
                .reward(new ItemReward(RewardNotice.stack(PRISM, 1).getItem(), 1)).build());
        event.assignQuestToGroup(ID, PrologueWorld.id("adventure_quests"));
    }

    static void register(ArcQuestRegistrationEvent.Dialogue event) {
        var tree = PrologueStory.tree(ROUTE).repeatable(true);
        tree.node("welcome").say(DialogueText.translatable(KEY + "welcome", DialogueText.DialogueArg.playerName()), TREE + "/welcome")
                .delay(2100).autoNext("feature");
        tree.node("feature").say(DialogueText.translatable(KEY + "feature"), TREE + "/feature").delay(4300).autoNext("reward");
        tree.node("reward").say(DialogueText.translatable(KEY + "reward"), TREE + "/reward")
                .choice("start", DialogueText.translatable(KEY + "start"), c -> c.addEvents((p, npc) -> {
                    if (meeting(p) && PrologueStory.atDialogueProfessor(p)) advance(p, MEET);
                }).close());
        event.register(tree.build());
    }

    private static PhaseBuilder phase(String id) {
        var name = QuestText.translatable(KEY + "phase." + id);
        return PhaseBuilder.create(id).displayName(name).objective(ObjectiveBuilder.custom(ID.withSuffix("/" + id), 1).display(name));
    }

    private static void advance(ServerPlayer player, String phase) {
        QuestProgressHandler.incrementObjective(player, ID.toString(), phase, 0, 1);
    }
}
