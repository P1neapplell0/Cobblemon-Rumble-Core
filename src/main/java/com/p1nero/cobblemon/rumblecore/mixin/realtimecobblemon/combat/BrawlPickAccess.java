package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.moves.MoveInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "com.johnn.realtimecobblemon.moves.Brawl$Pick", remap = false)
public interface BrawlPickAccess {
    @Invoker("m") MoveInfo rumble$move();
}
