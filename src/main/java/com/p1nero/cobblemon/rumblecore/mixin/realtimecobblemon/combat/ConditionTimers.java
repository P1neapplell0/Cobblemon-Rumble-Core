package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.moves.Conditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Conditions.State.class, remap = false)
public interface ConditionTimers {
    @Accessor("statusUntil") long rumble$statusUntil();
    @Accessor("statusUntil") void rumble$expireStatus(long time);
    @Accessor("launchedUntil") long rumble$launchedUntil();
    @Accessor("stunUntil") long rumble$stunUntil();
    @Accessor("boundUntil") long rumble$boundUntil();
    @Accessor("confusedUntil") long rumble$confusedUntil();
    @Accessor("confusedUntil") void rumble$clearConfusion(long time);
}
