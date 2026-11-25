package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.moves.MoveEngine;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MoveEngine.class, remap = false)
public interface RealtimeLandingMove {
    @Accessor("landing")
    static MoveInfo rumble$landing() { throw new AssertionError(); }
}
