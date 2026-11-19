package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.moves.Brawl;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.UUID;
import com.johnn.realtimecobblemon.MoveCombat;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.duel.Duels;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.BattleMusic;
import com.p1nero.cobblemon.rumblecore.combat.reward.WildCapture;
import com.p1nero.cobblemon.rumblecore.prologue.FixedStoryActors;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

final class ArenaIntegration {
    @Mixin(value = com.johnn.realtimecobblemon.MegaEvo.class, remap = false)
    static abstract class Mega {
        @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
        private static void rumble$useArenaBinding(ServerPlayer player, CallbackInfo ci) {
            if (ArenaMatch.inArena(player) != null) ci.cancel();
        }
    }

    @Mixin(value = Duels.class, remap = false)
    static abstract class Rules {
        @Inject(method = "groupOf", at = @At("HEAD"), cancellable = true)
        private static void rumble$arena(Entity entity, CallbackInfoReturnable<Object> cir) {
            var arena = ArenaMatch.group(entity);
            if (arena != null) cir.setReturnValue(arena);
        }

        @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
        private static void rumble$owned(Player player, Pokemon pokemon, CallbackInfoReturnable<Boolean> cir) {
            if (player instanceof ServerPlayer p && ArenaMatch.owns(p, pokemon)) cir.setReturnValue(true);
        }

        @Inject(method = "team", at = @At("HEAD"), cancellable = true)
        private static void rumble$roster(Player player, CallbackInfoReturnable<List<Pokemon>> cir) {
            if (player instanceof ServerPlayer p) {
                var roster = ArenaMatch.roster(p);
                if (roster != null) cir.setReturnValue(roster);
            }
        }
    }

    @Mixin(value = Smash.class, remap = false)
    static abstract class Session {
        @Inject(method = "sendState", at = @At("HEAD"), cancellable = true)
        private static void rumble$keepCamera(ServerPlayer player, int entity, CallbackInfo ci) {
            if (entity < 0 && ArenaMatch.retainClientControl(player)) ci.cancel();
        }
    }

    @Mixin(value = Brawl.class, remap = false)
    static abstract class TrainerAI {
        @Inject(method = "aggro", at = @At("HEAD"), cancellable = true)
        private static void rumble$passiveWildAggro(CallbackInfo ci) {
            // This scan is only for unsolicited wild aggro. Hits and trained arena foes provoke separately.
            ci.cancel();
        }

        @Inject(method = "provoke", at = @At("RETURN"))
        private static void rumble$combatMusic(Smash.Session session, LivingEntity target, CallbackInfo ci) {
            if (target instanceof PokemonEntity opponent && Brawl.fighting(opponent)) BattleMusic.engage(session, opponent, false);
        }

        @ModifyExpressionValue(method = "onServerTick", at = @At(value = "CONSTANT", args = "doubleValue=16.0"))
        private static double rumble$trainerAcquireRange(double range, @Local PokemonEntity mon) {
            return ArenaMatch.isTrainerFighter(mon) ? 40.0 : range;
        }

        @ModifyExpressionValue(method = "valid", at = @At(value = "CONSTANT", args = "doubleValue=32.0"))
        private static double rumble$trainerChaseRange(double range, @Local PokemonEntity mon) {
            return ArenaMatch.isTrainerFighter(mon) ? 48.0 : range;
        }

        @WrapOperation(method = "provoke", at = @At(value = "INVOKE",
                target = "Lcom/cobblemon/mod/common/pokemon/Pokemon;getOwnerUUID()Ljava/util/UUID;"))
        private static UUID rumble$allowOpposingTrainer(Pokemon pokemon, Operation<UUID> original) {
            PokemonEntity entity = pokemon.getEntity();
            return entity != null && ArenaMatch.isTrainerFighter(entity) ? null : original.call(pokemon);
        }
    }

    @Mixin(value = MoveCombat.class, remap = false)
    static abstract class Targets {
        @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
        private static void rumble$target(Smash.Session attacker, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
            if (FixedStoryActors.protectedFromCombat(target)
                    || target instanceof PokemonEntity pokemon && WildCapture.stunned(pokemon)) {
                cir.setReturnValue(false);
                return;
            }
            var allowed = ArenaMatch.canHit(attacker, target);
            if (allowed != null) cir.setReturnValue(allowed);
        }
    }
}
