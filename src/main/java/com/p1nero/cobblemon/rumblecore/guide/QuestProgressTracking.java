package com.p1nero.cobblemon.rumblecore.guide;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.arcadia.arc_quest.api.event.quest.QuestPhaseCompletedEvent;
import org.arcadia.arc_quest.api.event.quest.QuestProgressChangedEvent;
import org.arcadia.arc_quest.quest.service.TrackedQuestService;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class QuestProgressTracking {
    private QuestProgressTracking() {}

    @SubscribeEvent
    public static void onPhaseCompleted(QuestPhaseCompletedEvent event) {
        // ArcQuest posts before changing the phase. The subsequent state packets carry the new phase.
        track(event.getPlayer(), event.getQuestId());
    }

    @SubscribeEvent
    public static void onProgressChanged(QuestProgressChangedEvent event) {
        if (event.getNewProgress() != event.getOldProgress()) track(event.getPlayer(), event.getQuestId());
    }

    private static void track(ServerPlayer player, ResourceLocation quest) {
        var definition = org.arcadia.arc_quest.api.ArcQuestAPI.getQuest(quest);
        if (definition == null || !definition.canBeAutoTrack()) return;
        var data = ArcQuestPlayerManager.get(player);
        String questId = quest.toString();
        if (data != null && data.isQuestActive(questId) && !questId.equals(data.getTrackedQuestId())) {
            TrackedQuestService.setTrackedQuest(player, questId);
        }
    }
}
