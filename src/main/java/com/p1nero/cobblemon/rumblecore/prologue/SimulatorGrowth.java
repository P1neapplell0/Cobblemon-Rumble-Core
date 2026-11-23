package com.p1nero.cobblemon.rumblecore.prologue;

import com.nore.cobblebash.gym.GymType;
import com.p1nero.cobblebashscreen.api.TrainingSimulatorApi;
import com.p1nero.cobblebashscreen.api.event.TrainingSimulatorEliteFourCompletedEvent;
import com.p1nero.cobblebashscreen.api.event.TrainingSimulatorGymClearedEvent;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.quest.CobbleDollarReward;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.api.QuestCompletionPolicy;
import org.arcadia.arc_quest.quest.api.QuestText;
import org.arcadia.arc_quest.quest.builder.ObjectiveBuilder;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;
import org.arcadia.arc_quest.quest.builder.QuestBuilder;
import org.arcadia.arc_quest.quest.logic.QuestProgressHandler;
import org.arcadia.arc_quest.quest.reward.ItemReward;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

/** Simulator milestones are granted after the test report, separate from prologue growth. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class SimulatorGrowth {
    private static final ResourceLocation ID = PrologueWorld.id("growth_training_simulator");
    private static final ResourceLocation PLATES = PrologueWorld.id("growth_simulator_plates");
    private static final Map<String, String> PLATE_NAMES = Map.ofEntries(
            Map.entry("bug", "insect"), Map.entry("dark", "dread"), Map.entry("dragon", "draco"), Map.entry("electric", "zap"),
            Map.entry("fairy", "pixie"), Map.entry("fighting", "fist"), Map.entry("fire", "flame"), Map.entry("flying", "sky"),
            Map.entry("ghost", "spooky"), Map.entry("grass", "meadow"), Map.entry("ground", "earth"), Map.entry("ice", "icicle"),
            Map.entry("poison", "toxic"), Map.entry("psychic", "mind"), Map.entry("rock", "stone"), Map.entry("steel", "iron"), Map.entry("water", "splash"));
    private static final String KEY = "rumble.growth.simulator.", CHAMPION = "champion";
    private static final String WON = "rumble.growth.simulator.champion_won";
    private static final List<Goal> GOALS = List.of(
            new Goal(1, 5_000, "cobblemon:ultra_ball", 5), new Goal(3, 12_000, "cobblemon:quick_ball", 10),
            new Goal(5, 20_000, "cobblemon:master_ball", 1), new Goal(8, 40_000, "cobblemon:ancient_origin_ball", 1),
            new Goal(10, 100_000, "horizoncobblemonitems:ultimate_mac_and_cheese", 1),
            new Goal(12, 160_000, "horizoncobblemonitems:omnipotent_hidden_machine", 1),
            new Goal(14, 200_000, "cobblemon_dreamweaving_prism_ball:dreamweaving_prism_ball", 1),
            new Goal(16, 300_000, "cobblemon_dreamweaving_prism_ball:prism_ball", 1),
            new Goal(18, 400_000, "cobblemon:ancient_origin_ball", 2));

    private SimulatorGrowth() {}

    static void register(ArcQuestRegistrationEvent.Quest event) {
        var quest = QuestBuilder.create(ID).category(QuestCategory.ADVENTURE).themeColor(0x43A047)
                .icon(Items.RESPAWN_ANCHOR).sortOrder(1050).cannotAbandon().canBeAutoTrack(false)
                .displayName(QuestText.translatable(KEY + "title")).description(QuestText.translatable(KEY + "description"))
                .requiresQuest(SimulatorTestQuest.ID).completionPolicy(QuestCompletionPolicy.ALL);
        var bootstrap = PhaseBuilder.create("milestones").objective(ObjectiveBuilder.nullObjective().hidden()).thenGoTo(CHAMPION);
        GOALS.forEach(goal -> bootstrap.thenGoTo(goal.phase()));
        quest.phase(bootstrap);
        for (var goal : GOALS) {
            var label = QuestText.translatable(KEY + "badges", QuestText.Arg.constant(goal.count()));
            var phase = phase(goal.phase(), label, goal.count()).reward(new CobbleDollarReward(goal.coins()));
            reward(phase, goal.item(), goal.amount());
            if (goal.count() == 16) reward(phase, "cobblemon:master_ball", 1);
            quest.phase(phase);
        }
        var champion = phase(CHAMPION, QuestText.translatable(KEY + CHAMPION), 1).reward(new CobbleDollarReward(400_000));
        reward(champion, "cobblemon_dreamweaving_prism_ball:dreamweaving_prism_ball", 2);
        quest.phase(champion);
        event.register(quest.build());
        event.assignQuestToGroup(ID, PrologueWorld.id("growth_quests"));

        var plates = QuestBuilder.create(PLATES).category(QuestCategory.ADVENTURE).themeColor(0x43A047)
                .icon(Items.QUARTZ).sortOrder(1051).cannotAbandon().canBeAutoTrack(false)
                .displayName(QuestText.translatable(KEY + "plates_title")).description(QuestText.translatable(KEY + "plates_description"))
                .requiresQuest(SimulatorTestQuest.ID).completionPolicy(QuestCompletionPolicy.ALL);
        var types = PhaseBuilder.create("types").objective(ObjectiveBuilder.nullObjective().hidden());
        for (var gym : GymType.values()) types.thenGoTo(gym.getId());
        plates.phase(types);
        for (var gym : GymType.values()) {
            var type = gym.getId();
            var label = QuestText.translatable(KEY + "plates_goal", QuestText.Arg.constant(Component.translatable("cobblemon.type." + type)));
            var phase = PhaseBuilder.create(type).displayName(label).objective(ObjectiveBuilder.custom(PLATES.withSuffix("/" + type), 1).display(label));
            if (PLATE_NAMES.containsKey(type)) reward(phase, "mega_showdown:" + PLATE_NAMES.get(type) + "_plate", 1);
            plates.phase(phase);
        }
        event.register(plates.build());
        event.assignQuestToGroup(PLATES, PrologueWorld.id("growth_quests"));
    }

    private static PhaseBuilder phase(String phase, QuestText label, int count) {
        return PhaseBuilder.create(phase).displayName(label).objective(ObjectiveBuilder.custom(ID.withSuffix("/" + phase), count).display(label));
    }

    private static void reward(PhaseBuilder phase, String item, int count) {
        BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(item)).ifPresent(i -> phase.reward(new ItemReward(i, count)));
    }

    static void offer(ServerPlayer player) {
        QuestProgressHandler.acceptQuest(player, ID.toString());
        QuestProgressHandler.acceptQuest(player, PLATES.toString());
        sync(player);
    }

    @SubscribeEvent public static void cleared(TrainingSimulatorGymClearedEvent.Post event) { sync(event.getPlayer()); }

    @SubscribeEvent public static void champion(TrainingSimulatorEliteFourCompletedEvent.Post event) {
        ArcQuestPlayerManager.getOrCreate(event.getPlayer()).setFlag(WON);
        sync(event.getPlayer());
    }

    private static void sync(ServerPlayer player) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        var quest = data.getActiveQuest(ID.toString());
        var plates = data.getActiveQuest(PLATES.toString());
        if (plates != null) for (var gym : GymType.values()) {
            if (plates.isPhaseActive(gym.getId()) && TrainingSimulatorApi.hasClearedGym(player, gym))
                QuestProgressHandler.incrementObjective(player, PLATES.toString(), gym.getId(), 0, 1);
        }
        if (quest == null) return;
        int cleared = TrainingSimulatorApi.getClearedGymCount(player);
        for (var goal : GOALS) {
            if (!quest.isPhaseActive(goal.phase())) continue;
            int delta = Math.min(cleared, goal.count()) - quest.getObjectiveProgress(goal.phase(), 0);
            if (delta > 0) QuestProgressHandler.incrementObjective(player, ID.toString(), goal.phase(), 0, delta);
        }
        if (quest.isPhaseActive(CHAMPION) && data.hasFlag(WON)) QuestProgressHandler.incrementObjective(player, ID.toString(), CHAMPION, 0, 1);
    }

    private record Goal(int count, int coins, String item, int amount) {
        String phase() { return "badges_" + count; }
    }
}
