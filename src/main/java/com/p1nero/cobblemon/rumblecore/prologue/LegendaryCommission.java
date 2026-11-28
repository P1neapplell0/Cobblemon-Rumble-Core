package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.gym.LegendaryTrials;
import com.p1nero.cobblemon.rumblecore.gym.LegendaryTrials.Trial;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.dialogue.api.DialogueContext;
import org.arcadia.arc_quest.dialogue.api.DialogueText;
import org.arcadia.arc_quest.dialogue.builder.DialogueTreeBuilder;
import org.arcadia.arc_quest.dialogue.runtime.DialogueSessionManager;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.api.QuestGroupDefinition;
import org.arcadia.arc_quest.quest.api.QuestText;
import org.arcadia.arc_quest.quest.builder.ObjectiveBuilder;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;
import org.arcadia.arc_quest.quest.builder.QuestBuilder;
import org.arcadia.arc_quest.quest.logic.QuestProgressHandler;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

public final class LegendaryCommission {
    private static final String KEY = "rumble.legendary.", MEET = "meet_merchant", HUNT = "resolve_encounter";
    public static final ResourceLocation ID = PrologueWorld.id("legendary_commission");
    private static final ResourceLocation GROUP = PrologueWorld.id("legendary_path");
    private static final int PATH_THEME = 0xBA83E8;
    private static final String INTRO = PrologueWorld.id("story/merchant/legendary").toString();

    private LegendaryCommission() {}

    static void register(ArcQuestRegistrationEvent.Quest event) {
        event.registerGroup(new QuestGroupDefinition(GROUP, Component.translatable("rumble.quest_group.legendary_path").withColor(PATH_THEME), 1, PATH_THEME));
        event.register(SquadQuest.sound(QuestBuilder.create(ID)).category(QuestCategory.ADVENTURE).themeColor(SquadQuest.SIDE_THEME)
                .sortOrder(8).icon(Items.COMPASS).cannotAbandon().requiresQuest(GymStory.sixthGymQuest())
                .displayName(QuestText.translatable(KEY + "commission")).description(QuestText.translatable(KEY + "commission_desc"))
                .phase(phase(ID, MEET, KEY + "commission_goal")).build());
        event.assignQuestToGroup(ID, PrologueWorld.id("adventure_quests"));
        int order = 1;
        for (var trial : LegendaryTrials.all()) {
            event.register(SquadQuest.sound(QuestBuilder.create(trial.quest())).category(QuestCategory.ARCHON).themeColor(PATH_THEME)
                    .sortOrder(2000 + order++).icon(Items.ENDER_EYE).cannotAbandon().requiresQuest(ID)
                    .displayName(QuestText.translatable(trial.titleKey())).description(QuestText.translatable(KEY + "objective"))
                    .phase(phase(trial.quest(), HUNT, KEY + "objective")).build());
            event.assignQuestToGroup(trial.quest(), GROUP);
        }
    }

    private static PhaseBuilder phase(ResourceLocation id, String label, String text) {
        var goal = QuestText.translatable(text);
        return PhaseBuilder.create(label).displayName(goal).objective(ObjectiveBuilder.custom(id, 1).display(goal))
                .trackingMarker(HallQuestMarkers.actor(label, WanderingMerchantQuest.NAME, FixedStoryActors.MERCHANT, WanderingMerchantQuest.POS));
    }

    static void register(ArcQuestRegistrationEvent.Dialogue event) {
        var tree = DialogueTreeBuilder.create(INTRO).npc(DialogueText.translatable(WanderingMerchantQuest.NAME)).repeatable(true);
        tree.node("greeting").say(DialogueText.translatable(KEY + "greeting"), INTRO + "/greeting").delay(2900).autoNext("unlock");
        tree.node("unlock").say(DialogueText.translatable(KEY + "unlocked_notice"), INTRO + "/unlock")
                .choice("browse", DialogueText.translatable(KEY + "browse"), c -> c.addEvents((p, npc) -> finish(p, () -> MerchantPanels.openLegendary(p))).close())
                .choice("hoopa", DialogueText.translatable("rumble.hoopa.browse"), c -> c.addEvents((p, npc) -> finish(p, () -> MerchantHoopa.open(p))).close())
                .choice("later", DialogueText.translatable("rumble.merchant.leave"), c -> c.addEvents((p, npc) -> finish(p, null)).close());
        event.register(tree.build());
    }

    public static void offer(ServerPlayer player) { QuestProgressHandler.acceptQuest(player, ID.toString()); }
    public static boolean meeting(ServerPlayer p) { return GymStory.phaseActive(p, ID, MEET); }
    public static boolean unlocked(ServerPlayer p) { return ArcQuestPlayerManager.getOrCreate(p).isQuestCompleted(ID.toString()); }
    static void talk(ServerPlayer p, TrialMerchantEntity npc) { DialogueSessionManager.INSTANCE.startDialogue(p, npc, INTRO, new DialogueContext()); }

    private static void finish(ServerPlayer player, Runnable open) {
        var session = DialogueSessionManager.INSTANCE.getSession(player);
        if (session == null || !(session.getEntity() instanceof TrialMerchantEntity trader)
                || !WanderingMerchantQuest.at(player, trader) || !meeting(player)) return;
        QuestProgressHandler.incrementObjective(player, ID.toString(), MEET, 0, 1);
        if (open != null) PrologueStory.after(player, open);
    }

    public static void acceptTrial(ServerPlayer p, Trial trial) {
        if (unlocked(p) && !ArcQuestPlayerManager.getOrCreate(p).isQuestCompleted(trial.quest().toString()))
            QuestProgressHandler.acceptQuest(p, trial.quest().toString());
    }

    public static void cleared(ServerPlayer p, Trial trial) {
        if (GymStory.phaseActive(p, trial.quest(), HUNT)) QuestProgressHandler.incrementObjective(p, trial.quest().toString(), HUNT, 0, 1);
    }
}
