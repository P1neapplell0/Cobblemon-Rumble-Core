package com.p1nero.cobblemon.rumblecore.client.quest;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.arcadia.arc_quest.client.hud.QuestHudOverlay;
import org.arcadia.arc_quest.client.hud.questmarker.MarkerCameraBasisRegistry;
import org.arcadia.arc_quest.quest.registry.QuestRegistry;
import org.arcadia.arc_quest.questmarker.api.QuestMarkerPresentationRegistry;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class QuestMarkerPresentation {
    private QuestMarkerPresentation() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        QuestMarkerPresentationRegistry.registerLabelResolver(marker -> {
            var level = Minecraft.getInstance().level;
            if (!marker.hasEntityBinding() || level == null || !level.dimension().location().toString().equals(marker.getDimension())) return null;
            var actor = level.getEntity(marker.getFollowEntityId());
            return actor != null && (marker.getFollowEntityUuid().isBlank()
                    || actor.getUUID().toString().equals(marker.getFollowEntityUuid())) ? actor.getName() : null;
        });
        QuestMarkerPresentationRegistry.registerColorResolver(marker -> {
            if (!marker.hasQuestBinding()) return null;
            var quest = QuestRegistry.get(marker.getQuestId());
            return quest == null ? null : 0xFF000000 | (quest.getThemeColor() & 0xFFFFFF);
        });
        QuestMarkerPresentationRegistry.registerVisibilityFilter(marker -> {
            if (!marker.hasQuestBinding()) return true;
            var hud = QuestHudOverlay.INSTANCE;
            if (!marker.getQuestId().equals(hud.getTrackedQuestId())) return false;
            var phase = hud.getTrackedPhaseId();
            return !GymStory.isGymQuest(marker.getQuestId()) || !marker.hasPhaseBinding()
                    || phase == null || phase.isBlank() || phase.equals(marker.getPhaseId());
        });
        MarkerCameraBasisRegistry.register(new MarkerCameraBasisRegistry.Provider() {
            @Override public boolean applies(Camera camera) {
                for (Entity entity = camera.getEntity(); entity != null; entity = entity.getVehicle()) {
                    if (entity instanceof PokemonEntity) return true;
                }
                return false;
            }

            @Override public MarkerCameraBasisRegistry.Basis resolve(Camera camera, MarkerCameraBasisRegistry.Basis fallback) {
                return new MarkerCameraBasisRegistry.Basis(new Vec3(camera.getLookVector()),
                        new Vec3(camera.getLeftVector()), new Vec3(camera.getUpVector()));
            }
        });
    }
}
