package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.storage.CompletionStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = InstanceManager.class, remap = false)
public interface HallCompletionAccess {
    @Accessor("completions")
    CompletionStore rumble$getCompletions();
}
