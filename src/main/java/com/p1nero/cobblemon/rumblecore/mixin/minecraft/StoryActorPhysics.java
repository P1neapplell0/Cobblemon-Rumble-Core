package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.p1nero.cobblemon.rumblecore.prologue.FixedStoryActors;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

final class StoryActorPhysics {
    private StoryActorPhysics() {}

    @Mixin(LivingEntity.class)
    static abstract class Movement {
        @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/entity/LivingEntity;isControlledByLocalInstance()Z"))
        private boolean rumble$applyGravityWithAiDisabled(boolean original) {
            var actor = (LivingEntity) (Object) this;
            // Vanilla ties travel to active AI. Allow the server to simulate gravity and block collisions.
            return original || !actor.level().isClientSide && FixedStoryActors.isFixedInHall(actor);
        }
    }

    @Mixin(Entity.class)
    static abstract class Push {
        @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
        private void rumble$keepHallActorsInPlace(Entity other, CallbackInfo ci) {
            if (FixedStoryActors.isFixedInHall((Entity) (Object) this) || FixedStoryActors.isFixedInHall(other)) ci.cancel();
        }
    }
}
