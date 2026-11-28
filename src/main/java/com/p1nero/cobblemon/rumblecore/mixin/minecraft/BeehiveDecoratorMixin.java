package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.RumbleConfig;
import net.minecraft.world.level.levelgen.feature.treedecorators.BeehiveDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents generated trees, including trees grown from saplings, from placing bee nests. */
@Mixin(BeehiveDecorator.class)
public abstract class BeehiveDecoratorMixin {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void rumble$disableBeeNestGeneration(TreeDecorator.Context context,
                                               CallbackInfo callbackInfo) {
        if (RumbleConfig.disablesBeehives()) callbackInfo.cancel();
    }
}
