package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.prologue.FixedStoryActors;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
abstract class StoryActorTrackedEntity implements FixedStoryActors.TrackedActor {
    @Shadow public abstract void updatePlayer(ServerPlayer player);

    @Override
    public void rumble$updateViewer(ServerPlayer player) { updatePlayer(player); }
}
