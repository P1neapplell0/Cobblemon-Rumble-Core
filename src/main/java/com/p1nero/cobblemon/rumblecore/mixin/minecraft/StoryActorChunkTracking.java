package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.prologue.FixedStoryActors;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkMap.class)
abstract class StoryActorChunkTracking implements FixedStoryActors.Tracking {
    @Shadow @Final private Int2ObjectMap<?> entityMap;

    @Override
    public void rumble$updateStoryAudience(Entity entity, ServerPlayer player) {
        if (entityMap.get(entity.getId()) instanceof FixedStoryActors.TrackedActor tracker) tracker.rumble$updateViewer(player);
    }
}
