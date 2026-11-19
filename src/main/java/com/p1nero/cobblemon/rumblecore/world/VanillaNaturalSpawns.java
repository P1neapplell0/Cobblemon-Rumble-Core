package com.p1nero.cobblemon.rumblecore.world;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class VanillaNaturalSpawns {
    private VanillaNaturalSpawns() {}

    private static boolean blocked(EntityType<?> type) {
        return type != EntityType.VILLAGER && type != EntityType.ENDER_DRAGON
                && "minecraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void filterCandidates(LevelEvent.PotentialSpawns event) {
        if (event.getLevel().isClientSide()) return;
        for (int i = event.getSpawnerDataList().size() - 1; i >= 0; i--) {
            var candidate = event.getSpawnerDataList().get(i);
            if (blocked(candidate.type)) event.removeSpawnerData(candidate);
        }
    }

    // Also handles chunk population and special spawners (e.g. phantoms) that skip the candidate list.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockNaturalSpawn(FinalizeSpawnEvent event) {
        var reason = event.getSpawnType();
        if ((reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.PATROL)
                && blocked(event.getEntity().getType())) event.setSpawnCancelled(true);
    }
}
