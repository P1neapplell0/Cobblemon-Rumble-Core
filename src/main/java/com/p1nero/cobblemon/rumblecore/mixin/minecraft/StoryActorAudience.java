package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.prologue.FixedStoryActors;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class StoryActorAudience {
    @Inject(method = "broadcastToPlayer", at = @At("HEAD"), cancellable = true)
    private void rumble$storyAudience(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (!FixedStoryActors.mayTrack((Entity) (Object) this, player)) cir.setReturnValue(false);
    }
}
