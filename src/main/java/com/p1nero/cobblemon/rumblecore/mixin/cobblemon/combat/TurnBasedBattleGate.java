package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.battles.*;
import java.util.Set;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BattleRegistry.class, remap = false)
abstract class TurnBasedBattleGate {
    // All turn-based callers (including challenge packets and NPCs) receive Cobblemon's normal failure result.
    @Inject(method = "startBattle", at = @At("HEAD"), cancellable = true)
    private static void rumble$disableTurnBased(BattleFormat format, BattleSide first, BattleSide second,
                                                 boolean preempt, CallbackInfoReturnable<BattleStartResult> cir) {
        BattleStartError disabled = entity -> Component.translatable("rumble.combat.turn_based_disabled");
        cir.setReturnValue(new ErroredBattleStart(Set.of(disabled), new BattleActorErrors()));
    }
}
