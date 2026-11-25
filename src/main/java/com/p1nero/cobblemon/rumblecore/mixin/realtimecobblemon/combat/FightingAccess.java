package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Fighting;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor bridge for RTC 1.0.3alpha.1's private Fighting helpers.
 *
 * Maintenance: this must remain in rumblecore.mixins.json (normal mixins list).
 * If RTC changes role(MoveInfo) or grabTarget(Smash.Session), update both the
 * @Invoker signatures and every caller before removing or changing this bridge.
 */
@Mixin(value = Fighting.class, remap = false)
public interface FightingAccess {
    @Invoker("role")
    static Fighting.Role rumble$role(MoveInfo info) { throw new AssertionError(); }

    @Invoker("grabTarget")
    static LivingEntity rumble$grabTarget(Smash.Session session) { throw new AssertionError(); }
}
