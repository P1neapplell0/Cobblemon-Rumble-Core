package com.p1nero.cobblemon.rumblecore.prologue;

import com.nore.cobblebash.dimension.CobbleBashDimensions;
import com.nore.cobblebash.gym.GymType;
import com.nore.cobblebash.instance.GymInstanceManager;
import com.p1nero.cobblebashscreen.api.TrainingSimulatorApi;
import com.p1nero.cobblebashscreen.api.event.TrainingSimulatorChallengeStartedEvent;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.guide.RumbleGuides;
import com.p1nero.cobblemon.rumblecore.quest.RewardNotice;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;
import org.arcadia.arc_quest.questmarker.api.MarkSpec;
import org.arcadia.arc_quest.questmarker.api.MarkableObject;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class SimulatorTestQuest {
    private static final String KEY = "rumble.simulator_test.";
    private static final String MEET = "meet", TEST = "test", REPORT = "report";
    private static final String INTRO = "simulator_test_intro", RESULT = "simulator_test_result";
    private static final String ROCK_ENTERED = "rumble_simulator_test_rock_entered";
    static final ResourceLocation ID = PrologueWorld.id("simulator_test");

    private SimulatorTestQuest() {}

    public static void offer(ServerPlayer player) {
        if (QuestProgressHandler.acceptQuest(player, ID.toString()))
            PrologueStory.after(player, () -> ProfessorStockMessage.sendSimulator(player));
    }

    static String professorRoute(ServerPlayer player) {
        return active(player, MEET) ? INTRO : active(player, REPORT) ? RESULT : null;
    }

    static void register(ArcQuestRegistrationEvent.Quest event) {
        var pos = MegaSimulator.POS;
        var terminal = MarkSpec.translated("simulator_test_terminal", "rumble.mega.simulator.title",
                new MarkableObject.DimensionPos(PrologueWorld.GYMS, pos.getX(), pos.getY() + 1, pos.getZ()));
        event.register(SquadQuest.sound(QuestBuilder.create(ID)).category(QuestCategory.ADVENTURE)
                .sortOrder(7).themeColor(SquadQuest.SIDE_THEME).icon(Items.RESPAWN_ANCHOR).cannotAbandon()
                .requiresQuest(GymStory.fifthGymQuest()).displayName(QuestText.translatable(KEY + "title"))
                .description(QuestText.translatable(KEY + "description"))
                .completionPolicy(QuestCompletionPolicy.SPECIFIC_PHASE).completionTargetPhase(REPORT)
                .phase(phase(MEET).trackingMarker(PrologueStory.PROFESSOR_MARK).thenGoTo(TEST))
                .phase(phase(TEST).trackingMarker(terminal).thenGoTo(REPORT))
                .phase(phase(REPORT).trackingMarker(PrologueStory.PROFESSOR_MARK)
                        .grantGuideOnComplete(RumbleGuides.SIMULATOR))
                .reward(new ItemReward(RewardNotice.stack("cobblemon:pp_max", 1).getItem(), 1)).build());
        event.assignQuestToGroup(ID, PrologueWorld.id("adventure_quests"));
    }

    private static PhaseBuilder phase(String name) {
        return PhaseBuilder.create(name).displayName(QuestText.translatable(KEY + "phase." + name))
                .objective(ObjectiveBuilder.custom(ID.withSuffix("/" + name), 1).display(QuestText.translatable(KEY + "objective." + name)));
    }

    static void register(ArcQuestRegistrationEvent.Dialogue event) {
        var intro = PrologueStory.tree(INTRO).repeatable(true);
        intro.node("welcome").say(text("welcome", DialogueText.DialogueArg.playerName()), INTRO + "/welcome")
                .choice("why", text("why"), c -> c.goTo("strong"));
        intro.node("strong").say(text("strong"), INTRO + "/strong")
                .choice("boast", text("boast"), c -> c.goTo("warning"));
        line(intro, INTRO, "warning", "instructions", 4500);
        intro.node("instructions").say(text("instructions"), INTRO + "/instructions")
                .choice("start", text("start"), c -> c.addEvents((p, npc) -> {
                    if (!PrologueStory.atDialogueProfessor(p) || !active(p, MEET)) return;
                    MegaSimulator.unlockUniverse(p);
                    TrainingSimulatorApi.unlockAllGyms(p);
                    TrainingSimulatorApi.unlockEliteFour(p);
                    advance(p, MEET);
                }).close());
        event.register(intro.build());

        var report = PrologueStory.tree(RESULT).repeatable(true);
        line(report, RESULT, "report_1", "report_2", 3900);
        line(report, RESULT, "report_2", "report_3", 3100);
        line(report, RESULT, "report_3", "report_4", 2850);
        report.node("report_4").say(text("report_4"), RESULT + "/report_4")
                .choice("reward", text("reward"), c -> c.goTo("pp_max"));
        line(report, RESULT, "pp_max", "received", 4000);
        report.node("received").say(RewardNotice.items(RewardNotice.stack("cobblemon:pp_max", 1)), RESULT + "/received")
                .choice("done", DialogueText.translatable("gui.done"), c -> c.addEvents((p, npc) -> {
                    if (!PrologueStory.atDialogueProfessor(p) || !active(p, REPORT)) return;
                    advance(p, REPORT);
                    SimulatorGrowth.offer(p);
                }).close());
        event.register(report.build());
    }

    private static DialogueText text(String key, DialogueText.DialogueArg... args) { return DialogueText.translatable(KEY + key, args); }

    private static void line(DialogueTreeBuilder builder, String route, String key, String next, int delay) {
        builder.node(key).say(text(key), route + "/" + key).delay(delay).autoNext(next);
    }

    @SubscribeEvent
    public static void entered(TrainingSimulatorChallengeStartedEvent.Post event) {
        var player = event.getPlayer();
        if (event.isEliteFour() || !GymType.ROCK.getId().equals(event.getGymId()) || !active(player, TEST)
                || !CobbleBashDimensions.GYM_VOID.equals(player.level().dimension())) return;
        var instance = GymInstanceManager.getActive(player.getUUID());
        if (instance != null && GymType.ROCK.getId().equals(instance.getGymType()))
            ArcQuestPlayerManager.getOrCreate(player).setFlag(ROCK_ENTERED);
    }

    @SubscribeEvent
    public static void left(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && CobbleBashDimensions.GYM_VOID.equals(event.getFrom())
                && !CobbleBashDimensions.GYM_VOID.equals(event.getTo()) && active(player, TEST)
                && ArcQuestPlayerManager.getOrCreate(player).hasFlag(ROCK_ENTERED)) advance(player, TEST);
    }

    static boolean testing(ServerPlayer player) {
        return active(player, TEST) && !ArcQuestPlayerManager.getOrCreate(player).hasFlag(ROCK_ENTERED);
    }

    private static boolean active(ServerPlayer player, String phase) { return GymStory.phaseActive(player, ID, phase); }
    private static void advance(ServerPlayer player, String phase) {
        if (active(player, phase)) QuestProgressHandler.incrementObjective(player, ID.toString(), phase, 0, 1);
    }
}
