package com.p1nero.cobblemon.rumblecore.prologue;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.quest.CobbleDollarReward;
import java.util.Arrays;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.quest.api.IReward;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.api.QuestGroupDefinition;
import org.arcadia.arc_quest.quest.api.QuestText;
import org.arcadia.arc_quest.quest.builder.ObjectiveBuilder;
import org.arcadia.arc_quest.quest.builder.PhaseBuilder;
import org.arcadia.arc_quest.quest.builder.QuestBuilder;
import org.arcadia.arc_quest.quest.logic.QuestProgressHandler;
import org.arcadia.arc_quest.quest.reward.ItemReward;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

/** Opening milestones from the old Trainer Association. */
final class TrainerGrowth {
    private static final int GREEN = 0x43A047;
    private static final String PREFIX = "rumble.growth.";
    private static final String PRISM = "cobblemon_dreamweaving_prism_ball:prism_ball";
    private static final String DREAM = "cobblemon_dreamweaving_prism_ball:dreamweaving_prism_ball";
    private static final String CAPS = "horizoncobblemonitems:";
    private static final List<String> RANDOM_CAPS = List.of("health", "mighty", "tough", "smart", "courage", "quick");
    private static final List<Track> TRACKS = List.of(
            new Track("level", CobblemonItems.EXPERIENCE_CANDY_M, TrainerGrowth::highest, List.of(
                    milestone(20, 2_000, item("exp_candy_m", 2)),
                    milestone(50, 6_000, item("exp_candy_xl", 2)),
                    milestone(70, 20_000, item("rare_candy", 5)),
                    milestone(90, 40_000, item("rare_candy", 10)),
                    milestone(100, 100_000, item(CAPS + "ultra_rare_candy", 1), item(PRISM, 1)))),
            new Track("pokedex", CobblemonItems.POKEDEX_GREEN, TrainerGrowth::caught, List.of(
                    milestone(20, 4_000, item("great_ball", 10)),
                    milestone(50, 8_000, item("ultra_ball", 32)),
                    milestone(80, 13_000, item("quick_ball", 16), item("ultra_ball", 32)),
                    milestone(100, 16_000, item("master_ball", 1)),
                    milestone(150, 20_000, item("quick_ball", 16), item("great_ball", 16), item("timer_ball", 16)),
                    milestone(200, 30_000, item("ancient_origin_ball", 1)),
                    milestone(300, 40_000, item(PRISM, 1)),
                    milestone(400, 50_000, item(PRISM, 1)),
                    milestone(500, 60_000, item(DREAM, 1)),
                    milestone(600, 800_000, item("master_ball", 1), item(PRISM, 1)),
                    milestone(700, 1_100_000, item(PRISM, 1), item(DREAM, 1)),
                    milestone(800, 1_400_000, item("master_ball", 1), item(PRISM, 1), item(DREAM, 1)),
                    milestone(900, 1_700_000, item("ancient_origin_ball", 2), item(PRISM, 1), item(DREAM, 1)),
                    milestone(1025, 2_000_000, item("ancient_origin_ball", 3), item("master_ball", 3), item(PRISM, 2), item(DREAM, 2)))),
            new Track("badges", CobblemonItems.ABILITY_CAPSULE, GymStory::badgeCount, List.of(
                    milestone(2, 0, item("exp_candy_m", 3), item("ability_capsule", 1)),
                    milestone(4, 0, item("exp_candy_l", 3), new RandomCap(), item("master_ball", 1)),
                    milestone(6, 0, item("exp_candy_xl", 3), item("ability_patch", 1)),
                    milestone(8, 0, item("exp_candy_xl", 5), item(CAPS + "golden_bottle_cap", 1), item(PRISM, 1)))));

    private TrainerGrowth() {}

    private static final String PHASE = "milestone";

    static void register(ArcQuestRegistrationEvent.Quest event) {
        var group = PrologueWorld.id("growth_quests");
        event.registerGroup(new QuestGroupDefinition(group, Component.translatable("rumble.quest_group.growth_quests").withColor(GREEN), 3, GREEN));
        for (int i = 0; i < TRACKS.size(); i++) {
            var track = TRACKS.get(i);
            String key = PREFIX + track.kind() + ".";
            for (int index = 0; index < track.milestones().size(); index++) {
                var goal = track.milestones().get(index);
                var id = track.id(index);
                var label = QuestText.translatable(key + "phase", QuestText.Arg.constant(goal.target()));
                var phase = PhaseBuilder.create(PHASE).displayName(label)
                        .objective(ObjectiveBuilder.custom(id, goal.target()).display(label));
                if (goal.money() > 0) phase.reward(new CobbleDollarReward(goal.money()));
                goal.rewards().forEach(phase::reward);
                event.register(QuestBuilder.create(id).category(QuestCategory.ADVENTURE).themeColor(GREEN)
                        .icon(track.icon()).sortOrder(1000 + i * 20 + index).cannotAbandon().canBeAutoTrack(false)
                        .displayName(QuestText.translatable(key + "numbered", QuestText.Arg.constant(index + 1)))
                        .description(QuestText.translatable(key + "description"))
                        .requiresQuest(index == 0 ? PrologueStory.ID : track.id(index - 1).toString())
                        .phase(phase).build());
                event.assignQuestToGroup(id, group);
            }
        }
    }

    static void accept(ServerPlayer player) { update(player); }

    static void update(ServerPlayer player) {
        var data = ArcQuestPlayerManager.get(player);
        if (data == null || !data.isQuestCompleted(PrologueStory.ID)) return;
        for (var track : TRACKS) {
            for (int index = 0; index < track.milestones().size(); index++) {
                String id = track.id(index).toString();
                if (data.isQuestCompleted(id)) continue;
                if (!data.isQuestActive(id)) QuestProgressHandler.acceptQuest(player, id);
                var runtime = data.getActiveQuest(id);
                if (runtime == null || !runtime.isPhaseActive(PHASE)) break;
                var goal = track.milestones().get(index);
                int count = track.metric().applyAsInt(player);
                int delta = Math.min(count, goal.target()) - runtime.getObjectiveProgress(PHASE, 0);
                if (delta > 0 && (count < goal.target() || goal.rewards().stream()
                        .noneMatch(r -> r instanceof RegisteredReward reward && !reward.available()))) {
                    QuestProgressHandler.incrementObjective(player, id, PHASE, 0, delta);
                }
                // The completion advances the chain on the next update, keeping only one quest active.
                break;
            }
        }
    }

    private static int highest(ServerPlayer player) {
        int max = 0;
        for (var pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) max = Math.max(max, pokemon.getLevel());
        for (var pokemon : Cobblemon.INSTANCE.getStorage().getPC(player)) max = Math.max(max, pokemon.getLevel());
        return max;
    }

    private static int caught(ServerPlayer player) {
        return (int) Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(player).getSpeciesRecords().values()
                .stream().filter(record -> record.hasAtLeast(PokedexEntryProgress.OWNED)).count();
    }

    private static Milestone milestone(int target, int money, IReward... rewards) {
        return new Milestone(target, money, Arrays.asList(rewards));
    }

    private static RegistryReward item(String key, int count) {
        return new RegistryReward(ResourceLocation.parse(key.contains(":") ? key : "cobblemon:" + key), count);
    }

    private record Track(String kind, Item icon, ToIntFunction<ServerPlayer> metric, List<Milestone> milestones) {
        ResourceLocation id(int index) { return PrologueWorld.id("growth_" + kind + "_" + (index + 1)); }
    }

    private record Milestone(int target, int money, List<IReward> rewards) {}

    private interface RegisteredReward extends IReward {
        boolean available();
    }

    private record RegistryReward(ResourceLocation id, int count) implements RegisteredReward {
        @Override public boolean available() { return BuiltInRegistries.ITEM.containsKey(id); }
        @Override public void grant(ServerPlayer player) {
            BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> new ItemReward(item, count).grant(player));
        }
        @Override public String describe() {
            return BuiltInRegistries.ITEM.getOptional(id).map(i -> new ItemReward(i, count).describe())
                    .orElse(id + " x" + count);
        }
    }

    private static final class RandomCap implements RegisteredReward {
        @Override public boolean available() {
            return RANDOM_CAPS.stream().allMatch(name -> item(CAPS + name + "_bottle_cap", 1).available());
        }

        @Override public void grant(ServerPlayer player) {
            item(CAPS + RANDOM_CAPS.get(player.getRandom().nextInt(RANDOM_CAPS.size())) + "_bottle_cap", 1).grant(player);
        }
        @Override public String describe() { return Component.translatable(PREFIX + "random_cap").getString(); }
    }
}
