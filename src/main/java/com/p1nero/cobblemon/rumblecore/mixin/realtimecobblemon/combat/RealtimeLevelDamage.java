package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.MoveCombat;
import com.johnn.realtimecobblemon.SmashConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MoveCombat.class, remap = false)
abstract class RealtimeLevelDamage {
    @Inject(method = "scaled(IIDDDZFFZZ)F", at = @At("HEAD"), cancellable = true)
    private static void rumble$useActualBattleStats(int attackerLevel, int defenderLevel, double power,
            double attack, double defense, boolean stab, float effectiveness, float random,
            boolean attackerTrainer, boolean defenderTrainer, CallbackInfoReturnable<Float> cir) {
        // Use the real attacking level/stats. Defense already reflects the defender's level;
        // adding RTC's old wild-only levelEdge here would count the level gap again.
        float base = MoveCombat.damage(Math.max(1, attackerLevel), power,
                (int) Math.max(1, Math.round(attack)), (int) Math.max(1, Math.round(defense)),
                stab, effectiveness, random);
        cir.setReturnValue((float) (base * SmashConfig.get(SmashConfig.REALTIME_DAMAGE_SCALE)));
    }
}
