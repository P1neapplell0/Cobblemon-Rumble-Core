package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
abstract class PrologueLevelTick {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void rumble$trainingClock(BooleanSupplier time, CallbackInfo ci) {
        var level = (ServerLevel) (Object) this;
        if (PrologueTraining.skipLevelTick(level)) ci.cancel();
    }
}
