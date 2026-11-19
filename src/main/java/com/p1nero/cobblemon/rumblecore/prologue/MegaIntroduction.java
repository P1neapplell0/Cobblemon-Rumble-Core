package com.p1nero.cobblemon.rumblecore.prologue;

import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.quest.RewardNotice;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.dialogue.api.DialogueContext;
import org.arcadia.arc_quest.dialogue.api.DialogueText;
import org.arcadia.arc_quest.dialogue.builder.DialogueTreeBuilder;
import org.arcadia.arc_quest.dialogue.runtime.DialogueSessionManager;
import org.arcadia.arc_quest.quest.api.QuestText;
import org.arcadia.arc_quest.quest.builder.ObjectiveBuilder;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;
import org.arcadia.arc_quest.quest.logic.QuestProgressHandler;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class MegaIntroduction {
    public static final String ROUTE = "meet_may";
    public static final String NAME = "rumble.gym.actor.may";
    public static final String ASPECT = "rumble-gym-may";
    public static final String TREE = PrologueWorld.id("story/" + ROUTE).toString();
    public static final String UNLOCKED = "rumble.mega.simulator_unlocked";
    public static final String KEYSTONE = "mega_showdown:keystone";
    private static final String KEY = "rumble.mega.intro.";
    private static final String SMS = KEY + "sms_sent", GIFT = KEY + "key_received";
    public static final BlockPos MAY_POS = PrologueWorld.PROFESSOR.offset(2, 0, 0);
    private static final GymCatalog.Actor MAY = new GymCatalog.Actor("may", MAY_POS, List::of);

    private MegaIntroduction() {}

    public static PhaseBuilder phase() {
        return PhaseBuilder.create(ROUTE).displayName(QuestText.translatable(KEY + "objective"))
                .objective(ObjectiveBuilder.custom(PrologueWorld.id(ROUTE), 1).display(QuestText.translatable(KEY + "objective")))
                .trackingMarker(HallQuestMarkers.actor(ROUTE, NAME, FixedStoryActors.MAY, MAY_POS)).setFlagOnComplete(UNLOCKED);
    }

    static void register(ArcQuestRegistrationEvent.Dialogue event) {
        var tree = DialogueTreeBuilder.create(TREE).npc(DialogueText.translatable(NAME)).repeatable(true);
        tree.node("may_welcome").say(text("welcome", DialogueText.DialogueArg.playerName()), TREE + "/may_welcome").delay(2400).autoNext("professor_intro");
        tree.node("professor_intro").speaker(DialogueText.translatable("entity.cobblemon_rumble_core.professor"))
                .say(text("family", DialogueText.DialogueArg.playerName()), TREE + "/professor_intro").delay(7300).autoNext("may_gift");
        tree.node("may_gift").say(text("gift", DialogueText.DialogueArg.playerName()), TREE + "/may_gift").delay(2000).autoNext("may_received");
        tree.node("may_received").say(RewardNotice.items(RewardNotice.stack(KEYSTONE, 1)), TREE + "/may_received")
                .delay(1900).autoNext("may_explain");
        tree.node("may_explain").say(text("explain"), TREE + "/may_explain")
                .choice("stone", text("stone"), choice -> choice.goTo("professor_simulator"));
        tree.node("professor_simulator").speaker(DialogueText.translatable("entity.cobblemon_rumble_core.professor"))
                .say(text("simulator"), TREE + "/professor_simulator").delay(6200).autoNext("professor_unlock");
        tree.node("professor_unlock").speaker(DialogueText.translatable("entity.cobblemon_rumble_core.professor"))
                .say(text("unlock"), TREE + "/professor_unlock")
                .choice("done", DialogueText.translatable("gui.done"), choice -> choice.addEvents((p, ignored) -> finish(p)).close());
        event.register(tree.build());
    }

    @SubscribeEvent
    public static void gift(org.arcadia.arc_quest.api.event.dialogue.DialogueNodeStartedEvent event) {
        if (!TREE.equals(event.getDialogueId()) || !"may_received".equals(event.getNodeId())) return;
        var player = event.getPlayer();
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (!active(player) || !atMay(player, event.getNpc()) || data.hasFlag(GIFT)) return;
        var keystone = RewardNotice.stack(KEYSTONE, 1);
        if (keystone.isEmpty()) return;
        RewardNotice.grant(player, keystone);
        data.setFlag(GIFT);
    }

    private static void finish(ServerPlayer player) {
        var session = DialogueSessionManager.INSTANCE.getSession(player);
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (!active(player) || session == null || !atMay(player, session.getEntity()) || !data.hasFlag(GIFT)) return;
        QuestProgressHandler.incrementObjective(player, GymStory.thirdGymQuest().toString(), ROUTE, 0, 1);
        GymStory.acceptNext(player);
        MegaTrainingQuest.accept(player);
        new org.arcadia.arc_quest.guide.runtime.GuideUnlockService().grant(player, com.p1nero.cobblemon.rumblecore.guide.RumbleGuides.MEGA);
    }

    private static boolean active(ServerPlayer player) { return GymStory.phaseActive(player, GymStory.thirdGymQuest(), ROUTE); }

    public static boolean isMay(Entity entity) {
        return entity instanceof NPCEntity npc && npc.getAspects().contains(ASPECT);
    }

    static NPCEntity createActor(ServerLevel level) {
        return GymStory.newTrainer(level, MAY, Vec3.atBottomCenterOf(MAY_POS), 0);
    }

    static boolean visible(ServerPlayer player) {
        if (!player.isAlive() || !PrologueWorld.GYMS.equals(player.level().dimension())) return false;
        var session = DialogueSessionManager.INSTANCE.getSession(player);
        return active(player) || MegaTrainingQuest.visible(player)
                || session != null && FixedStoryActors.MAY.equals(FixedStoryActors.role(session.getEntity()));
    }

    static Entity actor(ServerPlayer player) { return FixedStoryActors.find(player.serverLevel(), FixedStoryActors.MAY); }

    static boolean atMay(ServerPlayer player, Entity entity) {
        return entity != null && entity == actor(player) && visible(player) && entity.isAlive() && entity.level() == player.level()
                && player.isAlive() && !player.isSpectator() && player.hasLineOfSight(entity)
                && player.distanceToSqr(entity) <= ProfessorEntity.TALK_DISTANCE * ProfessorEntity.TALK_DISTANCE;
    }

    static boolean talk(ServerPlayer player, Entity entity) {
        if (!atMay(player, entity)) return false;
        if (!active(player)) return MegaTrainingQuest.talk(player, entity);
        DialogueSessionManager.INSTANCE.startDialogue(player, entity, TREE, new DialogueContext());
        return true;
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0) return;
        boolean talking = DialogueSessionManager.INSTANCE.isInDialogue(player);
        boolean active = active(player);
        if (active && !talking) {
            var data = ArcQuestPlayerManager.getOrCreate(player);
            if (!data.hasFlag(SMS)) {
                data.setFlag(SMS);
                if (ModList.get().isLoaded("mine_chat")) ProfessorStockMessage.sendMay(player);
            }
        }
    }

    private static DialogueText text(String key, DialogueText.DialogueArg... args) {
        return DialogueText.translatable(KEY + key, args);
    }
}
