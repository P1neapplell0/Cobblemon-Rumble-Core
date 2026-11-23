package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.moves.MoveInfo;
import com.johnn.realtimecobblemon.moves.Sounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Sounds.class, remap = false)
public interface CinemaSoundAccess {
    @Invoker("move")
    static void rumble$move(ServerLevel level, MoveInfo move, boolean hit, Vec3 position) { throw new AssertionError(); }
}
