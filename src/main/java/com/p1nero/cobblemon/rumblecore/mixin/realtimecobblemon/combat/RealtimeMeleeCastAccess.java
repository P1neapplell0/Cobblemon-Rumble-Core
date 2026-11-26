package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** RTC 1.0.3alpha.1: Cast is package-private; keep its field bridge isolated for maintenance. */
@Mixin(targets = "com.johnn.realtimecobblemon.moves.MoveEngine$Cast", remap = false)
public interface RealtimeMeleeCastAccess {
    @Accessor("s") Smash.Session rumble$session();
    @Accessor("m") MoveInfo rumble$info();
    @Invoker("alive") boolean rumble$alive();
    @Invoker("single") boolean rumble$single();
}
