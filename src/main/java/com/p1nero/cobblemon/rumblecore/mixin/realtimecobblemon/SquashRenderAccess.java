package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(targets = "com.johnn.realtimecobblemon.client.SquashStretch", remap = false)
public interface SquashRenderAccess {
    @Accessor("PUSHED")
    static Set<LivingEntity> rumble$pushed() { throw new AssertionError(); }
}
