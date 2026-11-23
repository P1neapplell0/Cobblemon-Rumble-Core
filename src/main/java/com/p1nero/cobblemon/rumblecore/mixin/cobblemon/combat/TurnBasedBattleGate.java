package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.battles.BattleActorErrors;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.BattleStartError;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.ErroredBattleStart;
import com.gitlab.srcmc.rctapi.api.battle.BattleManager.TrainerEntityBattleActor;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.nore.cobblebash.entity.GymTrainerEntity;
import java.util.Arrays;
import java.util.Set;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BattleRegistry.class, remap = false)
abstract class TurnBasedBattleGate {
    // Mod-owned trainer challenges use their own battle formats and progression.
    @Inject(method = "startBattle", at = @At("HEAD"), cancellable = true)
    private static void rumble$disableTurnBased(BattleFormat format, BattleSide first, BattleSide second,
                                                 boolean preempt, CallbackInfoReturnable<BattleStartResult> cir) {
        if (hasExternalTrainer(first) || hasExternalTrainer(second)) return;
        BattleStartError disabled = entity -> Component.translatable("rumble.combat.turn_based_disabled");
        cir.setReturnValue(new ErroredBattleStart(Set.of(disabled), new BattleActorErrors()));
    }

    private static boolean hasExternalTrainer(BattleSide side) {
        return Arrays.stream(side.getActors()).anyMatch(actor -> actor instanceof TrainerEntityBattleActor trainer
                && (trainer.getEntity() instanceof TrainerMob || trainer.getEntity() instanceof GymTrainerEntity));
    }
}
