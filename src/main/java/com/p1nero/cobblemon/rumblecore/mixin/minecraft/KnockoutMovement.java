package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
abstract class KnockoutMovement {
    @WrapMethod(method = "move")
    private void rumble$slowCombatantMovement(MoverType type, Vec3 movement, Operation<Void> original) {
        float speed = type == MoverType.SELF ? ArenaMatch.knockoutMovement((Entity) (Object) this) : 1;
        original.call(type, speed == 1 ? movement : movement.scale(speed));
    }
}
