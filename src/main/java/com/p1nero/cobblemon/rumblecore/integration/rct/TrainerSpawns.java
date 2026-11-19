package com.p1nero.cobblemon.rumblecore.integration.rct;

import com.gitlab.srcmc.rctmod.world.entities.TrainerAssociation;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class TrainerSpawns {
    private TrainerSpawns() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && PrologueWorld.GYMS.equals(level.dimension())
                && (event.getEntity() instanceof TrainerMob || event.getEntity() instanceof TrainerAssociation)) {
            event.setCanceled(true);
        }
    }
}
