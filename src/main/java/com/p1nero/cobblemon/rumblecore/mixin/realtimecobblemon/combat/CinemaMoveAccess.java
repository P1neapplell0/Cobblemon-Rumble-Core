package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.MoveEngine;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = MoveEngine.class, remap = false)
public interface CinemaMoveAccess {
    @Invoker("receive")
    static float rumble$receive(Smash.Session target, DamageSource source, float damage, MoveInfo move) {
        throw new AssertionError();
    }
}
