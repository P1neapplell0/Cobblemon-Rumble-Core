package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(targets = "com.johnn.realtimecobblemon.client.ClientSmash", remap = false)
public interface GlideRenderAccess {
    @Accessor("GLIDING")
    static Set<Integer> rumble$gliding() { throw new AssertionError(); }
}
