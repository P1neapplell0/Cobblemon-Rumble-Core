package com.p1nero.cobblemon.rumblecore.quest;

import com.cobblemon.mod.common.CobblemonSounds;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.prologue.HallQuestMarkers;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.arcadia.arc_quest.dialogue.runtime.DialogueSessionManager;
import org.arcadia.arc_quest.quest.network.QuestSyncCoordinator;
import org.arcadia.arc_quest.quest.registry.QuestRegistry;
import org.arcadia.arc_quest.quest.service.TrackedQuestService;
import org.arcadia.arc_quest.questmarker.api.MarkTriggers;
import org.arcadia.arc_quest.questmarker.api.MarkableObject;
import org.arcadia.arc_quest.questmarker.api.QuestMarkerData;
import org.arcadia.arc_quest.questmarker.runtime.QuestMarkerReconciliationService;
import org.arcadia.arc_quest.questmarker.runtime.QuestMarkerTargetService;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayer;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class NearbyQuestTracking {
    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final double MAX_DISTANCE_SQUARED = 25.0;

    private NearbyQuestTracking() {}

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()
                || player.tickCount % CHECK_INTERVAL_TICKS != 0 || DialogueSessionManager.INSTANCE.isInDialogue(player)) return;
        var data = ArcQuestPlayerManager.get(player);
        if (data == null || (data.getTrackedQuestId() != null && !data.getTrackedQuestId().isBlank())) return;

        String dimension = player.level().dimension().location().toString();
        var nearest = Stream.concat(data.getAllMarkers().values().stream(), phaseMarkers(player, data))
                .filter(marker -> marker.isActive() && marker.hasQuestBinding()
                        && dimension.equals(marker.getDimension()) && data.isQuestActive(marker.getQuestId()))
                .filter(marker -> distance(player, marker) <= MAX_DISTANCE_SQUARED)
                .min(Comparator.comparingDouble((QuestMarkerData marker) -> distance(player, marker))
                        .thenComparing(QuestMarkerData::getId)).orElse(null);
        if (nearest == null || !TrackedQuestService.setTrackedQuest(player, nearest.getQuestId())) return;
        var runtime = data.getActiveQuest(nearest.getQuestId());
        if (nearest.hasPhaseBinding() && runtime != null && runtime.isPhaseActive(nearest.getPhaseId())
                && data.setTrackedPhaseId(nearest.getPhaseId())) {
            QuestMarkerReconciliationService.reconcileTrackingPhaseMarkers(player, data, true);
            QuestSyncCoordinator.persistAndSyncIfChanged(player, data);
        }
        var quest = QuestRegistry.get(nearest.getQuestId());
        var sound = quest == null ? null : quest.getChapterStartSound();
        player.playNotifySound(sound == null ? CobblemonSounds.LEVELUP_START : sound, SoundSource.PLAYERS, 1, 1);
    }

    private static Stream<QuestMarkerData> phaseMarkers(ServerPlayer player, ArcQuestPlayer data) {
        // ArcQuest creates tracking marks only for the focused phase. Check nearby-capable
        // targets from active phases without triggering arbitrary structure or entity searches.
        return data.getAllActiveQuests().entrySet().stream().flatMap(entry -> {
            String id = entry.getKey();
            var quest = QuestRegistry.get(id);
            if (quest == null || !data.isQuestActive(id)) return Stream.empty();
            return entry.getValue().getActivePhaseIds().stream().flatMap(phaseId -> {
                var phase = quest.getPhase(phaseId);
                if (phase == null) return Stream.empty();
                return phase.getTrackingMarks().stream()
                        .filter(spec -> spec.target() instanceof MarkableObject.DimensionPos
                                || spec.target() instanceof MarkableObject.Pos || spec.target() instanceof MarkableObject.BlockPosition
                                || HallQuestMarkers.isActorTarget(spec.target()))
                        .filter(MarkTriggers::isContinuous)
                        .filter(spec -> spec.activateWhen().test(player, data)
                                && !spec.deactivateWhen().test(player, data))
                        .map(spec -> QuestMarkerTargetService.resolve(id + "/" + phaseId + "/" + spec.id(),
                                id, phaseId, -1, spec, player, player.serverLevel()))
                        .filter(Objects::nonNull);
            });
        });
    }

    private static double distance(ServerPlayer player, QuestMarkerData marker) {
        return player.distanceToSqr(marker.getWorldX(), marker.getWorldY(), marker.getWorldZ());
    }
}
