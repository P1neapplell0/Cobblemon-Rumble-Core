package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Eternamax;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Brawl;
import net.minecraft.world.entity.LivingEntity;
import com.johnn.realtimecobblemon.field.Field;
import com.johnn.realtimecobblemon.moves.Clash;
import com.johnn.realtimecobblemon.moves.Combo;
import com.johnn.realtimecobblemon.moves.Defense;
import com.johnn.realtimecobblemon.moves.Fighting;
import com.johnn.realtimecobblemon.moves.MoveEngine;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets.Cue;

final class PrologueCombatInput {
    @Mixin(value = Combo.class, remap = false)
    static abstract class Light {
        @WrapMethod(method = "m1(Lnet/minecraft/server/level/ServerPlayer;Z)V")
        private static void rumble$practiceLight(ServerPlayer player, boolean launcher, Operation<Void> original) {
            if (!PrologueTraining.allowCombat(player, Cue.ATTACK)) return;
            Smash.Session session = Smash.session(player);
            long before = session == null ? 0 : session.combat.lastM1;
            original.call(player, launcher);
            if (session != null && session.combat.lastM1 != before) PrologueTraining.performed(player, Cue.ATTACK, 15);
        }
    }

    @Mixin(value = Fighting.class, remap = false)
    static abstract class Heavy {
        @Inject(method = "heavyInput", at = @At("HEAD"), cancellable = true)
        private static void rumble$practiceCharge(ServerPlayer player, boolean held, CallbackInfo ci) {
            if (held && !PrologueTraining.allowCombat(player, Cue.HEAVY)) ci.cancel();
        }

        @Inject(method = "fireHeavy", at = @At("TAIL"))
        private static void rumble$practiceHeavy(Smash.Session session, CallbackInfo ci) {
            PrologueTraining.performed(session.player, Cue.HEAVY, PrologueTraining.heavyRecovery());
        }
    }

    @Mixin(value = Brawl.class, remap = false)
    static abstract class PracticeFoe {
        @Inject(method = "provoke", at = @At("HEAD"), cancellable = true)
        private static void rumble$keepPracticePredictable(Smash.Session session, LivingEntity target, CallbackInfo ci) {
            if (!PrologueTraining.allowProvoke(session, target)) ci.cancel();
        }
    }

    @Mixin(value = Defense.class, remap = false)
    static abstract class Defensive {
        @Inject(method = "block", at = @At("HEAD"), cancellable = true)
        private static void rumble$practiceGuard(ServerPlayer player, boolean held, CallbackInfo ci) {
            if (held && !PrologueTraining.allowCombat(player, Cue.GUARD)) ci.cancel();
        }

        @Inject(method = "dodge", at = @At("HEAD"), cancellable = true)
        private static void rumble$practiceDodge(ServerPlayer player, float strafe, float forward, CallbackInfo ci) {
            if (!PrologueTraining.allowCombat(player, Cue.DODGE)) ci.cancel();
        }
    }

    @Mixin(value = MoveEngine.class, remap = false)
    static abstract class Move {
        @WrapMethod(method = "use(Lnet/minecraft/server/level/ServerPlayer;IF)V")
        private static void rumble$practiceMove(ServerPlayer player, int slot, float charge, Operation<Void> original) {
            if (!PrologueTraining.allowCombat(player, Cue.MOVE)) return;
            Smash.Session session = Smash.session(player);
            boolean usable = session != null && slot >= 0 && slot < session.cooldownUntil.length;
            long before = usable ? session.cooldownUntil[slot] : 0;
            original.call(player, slot, charge);
            // Allow the accepted cast to finish its windup before taking the practice target out of protection.
            if (usable && session.cooldownUntil[slot] != before) PrologueTraining.performed(player, Cue.MOVE, 45);
        }
    }

    @Mixin(value = Smash.class, remap = false)
    static abstract class Toggle {
        @WrapMethod(method = "onToggle")
        private static void rumble$teachToggle(ServerPlayer player, int id, Operation<Void> original) {
            if (ArenaMatch.toggle(player)) return;
            var session = Smash.session(player);
            if (session != null && session.pokemon.getPokemon().isFainted()
                    && com.p1nero.cobblemon.rumblecore.combat.RealtimeKnockout.inFlight(session.pokemon)) return;
            boolean wasActive = Smash.active(player);
            Cue action = wasActive ? Cue.MODE_EXIT : Cue.MODE_REENTER;
            if (!PrologueTraining.allowToggle(player, action)) return;
            if (!wasActive && com.p1nero.cobblemon.rumblecore.combat.RealtimeBattleEntry.enterSelected(player, id)) return;
            original.call(player, id);
            if (Smash.active(player) != wasActive) PrologueTraining.performed(player, action, 36);
        }
    }

    @Mixin(value = Field.class, remap = false)
    static abstract class Order {
        @Inject(method = "order", at = @At("HEAD"), cancellable = true)
        private static void rumble$deferOrder(ServerPlayer player, CallbackInfo ci) {
            if (PrologueTraining.restrict(player)) ci.cancel();
        }
    }

    @Mixin(value = Clash.class, remap = false)
    static abstract class Push {
        @Inject(method = "push", at = @At("HEAD"), cancellable = true)
        private static void rumble$deferClash(ServerPlayer player, CallbackInfo ci) {
            if (PrologueTraining.restrict(player)) ci.cancel();
        }
    }

    @Mixin(value = Eternamax.class, remap = false)
    static abstract class Dynamax {
        @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
        private static void rumble$deferDynamax(ServerPlayer player, CallbackInfo ci) {
            if (PrologueTraining.restrict(player) || ArenaMatch.inGimmickLesson(player)) ci.cancel();
        }
    }
}
