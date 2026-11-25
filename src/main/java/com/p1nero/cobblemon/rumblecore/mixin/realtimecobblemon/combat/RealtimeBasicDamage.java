package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Brawl;
import com.johnn.realtimecobblemon.moves.Combo;
import com.johnn.realtimecobblemon.moves.Fighting;
import com.johnn.realtimecobblemon.moves.MoveEngine;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeAiDamage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;

@Mixin(value = Combo.class, remap = false)
abstract class RealtimeBasicDamage {
    private static final String AGAINST = "Lcom/johnn/realtimecobblemon/MoveCombat;against(Lcom/cobblemon/mod/common/pokemon/Pokemon;Lnet/minecraft/world/entity/LivingEntity;DZDZFF)F";

    @ModifyArg(method = "strike", at = @At(value = "INVOKE", target = AGAINST), index = 2)
    private static double rumble$comboPower(double power) { return power > 15 ? 18 : 8; }

    @ModifyArg(method = "slam", at = @At(value = "INVOKE", target = AGAINST), index = 2)
    private static double rumble$slamPower(double power) { return 10; }

    @ModifyArg(method = "strike", at = @At(value = "INVOKE", target = AGAINST), index = 7)
    private static float rumble$comboRoll(float roll) { return Math.min(1, .85F + (roll - .90F) * 1.5F); }
    @ModifyArg(method = "slam", at = @At(value = "INVOKE", target = AGAINST), index = 7)
    private static float rumble$slamRoll(float roll, @Local(argsOnly = true) ServerLevel level) {
        return .85F + .15F * level.random.nextFloat();
    }
}

@Mixin(value = Fighting.class, remap = false)
abstract class RealtimeHeavyDamage {
    private static final String AGAINST = "Lcom/johnn/realtimecobblemon/MoveCombat;against(Lcom/cobblemon/mod/common/pokemon/Pokemon;Lnet/minecraft/world/entity/LivingEntity;DZDZFF)F";
    @ModifyArg(method = "fireHeavy", at = @At(value = "INVOKE", target = AGAINST), index = 2)
    private static double rumble$heavyPower(double power) { return Math.min(28, power * .55); }

    @ModifyArg(method = "fireHeavy", at = @At(value = "INVOKE", target = AGAINST), index = 7)
    private static float rumble$heavyRoll(float roll) { return Math.min(1, .85F + (roll - .90F) * 1.5F); }
}

@Mixin(value = Brawl.class, remap = false)
abstract class RealtimeAiBasicDamage {
    private static final String AGAINST = "Lcom/johnn/realtimecobblemon/MoveCombat;against(Lcom/cobblemon/mod/common/pokemon/Pokemon;Lnet/minecraft/world/entity/LivingEntity;DZDZFF)F";
    private static final String LAND = "land(Lcom/johnn/realtimecobblemon/moves/Brawl$Foe;Lcom/johnn/realtimecobblemon/Smash$Session;Lnet/minecraft/server/level/ServerLevel;Lcom/johnn/realtimecobblemon/moves/MoveInfo;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;ZF)F";

    @ModifyExpressionValue(method = LAND, at = @At(value = "INVOKE", target = "Lcom/johnn/realtimecobblemon/TypeChart;multiplier(Ljava/lang/String;Ljava/lang/Iterable;)F"))
    private static float rumble$aiNeutralAttacks(float effectiveness, @Local(argsOnly = true) MoveInfo move) {
        return RealtimeAiDamage.basic(move) ? 1 : effectiveness;
    }

    @WrapOperation(method = LAND, at = @At(value = "INVOKE", target = AGAINST))
    private static float rumble$aiDamage(Pokemon user, LivingEntity target, double power, boolean special,
            double attack, boolean stab, float type, float random, Operation<Float> original,
            @Local(argsOnly = true) MoveInfo move, @Local(argsOnly = true) Smash.Session victim,
            @Local(argsOnly = true) ServerLevel level, @Share("crit") LocalBooleanRef critical) {
        boolean basic = RealtimeAiDamage.basic(move);
        double effectivePower = basic ? power : power * 1.15;
        var actor = user.getEntity();
        if (actor == null) return original.call(user, target, effectivePower, special, attack, stab && !basic, type, random);
        boolean crit = RealtimeAiDamage.critical(move, level.random);
        critical.set(crit);
        return RealtimeAiDamage.calculate(actor, victim.pokemon, target, effectivePower, special, stab && !basic, type, random, crit);
    }

    @ModifyArg(method = LAND, at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/Pops;damage(Lnet/minecraft/world/entity/Entity;FFZ)V"), index = 3)
    private static boolean rumble$showAiCrit(boolean previous, @Share("crit") LocalBooleanRef critical) {
        return critical.get();
    }

    @ModifyConstant(method = "jabs", constant = @Constant(doubleValue = 14.0))
    private static double rumble$aiJabPower(double power) { return 8; }

    @ModifyArg(method = "heavy", at = @At(value = "INVOKE", target = "Lcom/johnn/realtimecobblemon/moves/MoveInfo;basic(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;DDILjava/lang/String;)Lcom/johnn/realtimecobblemon/moves/MoveInfo;"), index = 4)
    private static double rumble$aiHeavyPower(double power) { return Math.min(28, power * .55); }
}

@Mixin(value = MoveEngine.class, remap = false)
abstract class RealtimeSkillDamage {
    // Ordinary damaging moves only: fixed-HP, counter and other special branches are evaluated earlier.
    @ModifyArg(method = "damage", at = @At(value = "INVOKE", target = "Lcom/johnn/realtimecobblemon/MoveCombat;scaled(IIDDDZFFZZ)F"), index = 2)
    private static double rumble$skillPowerPokemon(double power) { return power * 1.15; }

    @ModifyArg(method = "damage", at = @At(value = "INVOKE", target = "Lcom/johnn/realtimecobblemon/MoveCombat;scaledVsMob(IDZFZ)F"), index = 1)
    private static double rumble$skillPowerMob(double power) { return power * 1.15; }
}
