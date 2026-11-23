package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.instance.InstanceSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InstanceManager.class, remap = false)
abstract class ArenaIntroOnPrepare {
    @Inject(method = "prepare", at = @At(value = "INVOKE",
            target = "Ljava/util/concurrent/ExecutorService;execute(Ljava/lang/Runnable;)V"))
    private void rumble$playIntro(InstanceSession session, CallbackInfo ci) {
        if (session instanceof ArenaMatch match) match.playIntro(((InstanceManager) (Object) this).getServer());
    }
}
