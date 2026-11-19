package com.p1nero.cobblemon.rumblecore.prologue;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonSounds;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.quest.CobbleDollarReward;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.arcadia.arc_quest.api.event.registry.ArcQuestRegistrationEvent;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.api.QuestCompletionPolicy;
import org.arcadia.arc_quest.quest.api.QuestGroupDefinition;
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
public final class SquadQuest {
    static final ResourceLocation ID = PrologueWorld.id("squad_assemble");
    private static final String TRAVEL = "overworld", CAPTURE = "capture", PARTY = "party";
    private static final String PREFIX = "rumble.squad.";
    private static final MarkSpec WAYSTONE = MarkSpec.translated("trial_waystone", TrialWaystones.HALL_NAME,
            new MarkableObject.DimensionPos(PrologueWorld.GYMS, PrologueWorld.ANCHOR.getX(),
                    PrologueWorld.ANCHOR.getY() + 2, PrologueWorld.ANCHOR.getZ()));

    private SquadQuest() {}

    static QuestBuilder sound(QuestBuilder quest) {
        return quest.chapterStartSound(CobblemonSounds.LEVELUP_START).chapterCompleteSound(CobblemonSounds.LEVELUP);
    }

    static final int SIDE_THEME = 0x5599FF;

    static void group(ArcQuestRegistrationEvent.Quest event, String name, QuestCategory category, int order, ResourceLocation... quests) {
        var group = PrologueWorld.id(name);
        int color = category.equals(QuestCategory.ADVENTURE) ? SIDE_THEME : ChatFormatting.GOLD.getColor();
        event.registerGroup(new QuestGroupDefinition(group, Component.translatable("rumble.quest_group." + name).withColor(color), order, color));
        for (var quest : quests) event.assignQuestToGroup(quest, group);
    }

    static void register(ArcQuestRegistrationEvent.Quest event) {
        event.register(sound(QuestBuilder.create(ID)).category(QuestCategory.ADVENTURE).sortOrder(0)
                .displayName(QuestText.translatable(PREFIX + "title"))
                .description(QuestText.translatable(PREFIX + "description"))
                .icon(Items.DIAMOND_SWORD).themeColor(SIDE_THEME).cannotAbandon().requiresQuest(PrologueStory.ID)
                .completionPolicy(QuestCompletionPolicy.SPECIFIC_PHASE).completionTargetPhase(PARTY)
                .phase(phase(TRAVEL, 1).trackingMarker(WAYSTONE).thenGoTo(CAPTURE))
                .phase(phase(CAPTURE, 1).thenGoTo(PARTY))
                .phase(phase(PARTY, 1))
                .reward(new CobbleDollarReward(2000))
                .reward(new ItemReward(Items.DIAMOND_SWORD, 1)).reward(new ItemReward(Items.DIAMOND_AXE, 1))
                .reward(new ItemReward(Items.DIAMOND_SHOVEL, 1)).reward(new ItemReward(Items.DIAMOND_HOE, 1)).build());
    }

    private static PhaseBuilder phase(String id, int required) {
        return PhaseBuilder.create(id).displayName(QuestText.translatable(PREFIX + "phase." + id))
                .objective(ObjectiveBuilder.custom(PrologueWorld.id("squad/" + id), required)
                        .display(QuestText.translatable(PREFIX + "phase." + id + ".objective")));
    }

    static boolean travelUnlocked(ServerPlayer player) {
        return ArcQuestPlayerManager.getOrCreate(player).isQuestCompleted(PrologueStory.ID);
    }

    static void accept(ServerPlayer player) {
        var data = ArcQuestPlayerManager.getOrCreate(player);
        if (!data.isQuestActive(ID.toString()) && !data.isQuestCompleted(ID.toString())) QuestProgressHandler.acceptQuest(player, ID.toString());
    }

    static void arrived(ServerPlayer player) {
        if (player.level().dimension() == Level.OVERWORLD) advance(player, TRAVEL, 1);
    }

    /** Called after Cobblemon reports a successful thrown-ball capture. */
    public static void captured(ServerPlayer player) {
        if (player.level().dimension() == Level.OVERWORLD) advance(player, CAPTURE, 1);
    }

    private static void advance(ServerPlayer player, String phase, int amount) {
        QuestProgressHandler.incrementObjective(player, ID.toString(), phase, 0, amount);
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
        var data = ArcQuestPlayerManager.get(player);
        if (data == null) return;
        var quest = data.getActiveQuest(ID.toString());
        if (quest == null || !quest.isPhaseActive(PARTY)) return;
        // Party size is a snapshot, not the accumulated number of caught/stored Pokémon.
        if (Cobblemon.INSTANCE.getStorage().getParty(player).occupied() == 6) advance(player, PARTY, 1);
    }

}
