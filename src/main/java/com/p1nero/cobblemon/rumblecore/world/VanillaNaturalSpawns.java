package com.p1nero.cobblemon.rumblecore.world;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.RumbleConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class VanillaNaturalSpawns {
    private VanillaNaturalSpawns() {}

    private static boolean blocked(EntityType<?> type) {
        if (type == EntityType.PLAYER || type == EntityType.VILLAGER
                || type == EntityType.ARMOR_STAND || type == EntityType.ENDER_DRAGON) return false;
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace();
        // Turning off the migrated rules retains this project's original vanilla natural-spawn filter.
        return "minecraft".equals(namespace) || RumbleConfig.blocksVanillaAndQuark() && "quark".equals(namespace);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void filterCandidates(LevelEvent.PotentialSpawns event) {
        if (event.getLevel().isClientSide()) return;
        for (int i = event.getSpawnerDataList().size() - 1; i >= 0; i--) {
            var candidate = event.getSpawnerDataList().get(i);
            if (blocked(candidate.type)) event.removeSpawnerData(candidate);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockPosition(MobSpawnEvent.PositionCheck event) {
        if (!event.getLevel().isClientSide() && RumbleConfig.blocksVanillaAndQuark() && blocked(event.getEntity().getType()))
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockJoin(EntityJoinLevelEvent event) {
        // Imported policy applies to living entities only; projectiles, drops, boats and markers are untouched.
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living
                && RumbleConfig.blocksVanillaAndQuark() && blocked(living.getType())) event.setCanceled(true);
    }

    // Also handles chunk population and special spawners (e.g. phantoms) that skip the candidate list.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockNaturalSpawn(FinalizeSpawnEvent event) {
        var reason = event.getSpawnType();
        if ((reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.PATROL)
                && blocked(event.getEntity().getType())) event.setSpawnCancelled(true);
    }
}
