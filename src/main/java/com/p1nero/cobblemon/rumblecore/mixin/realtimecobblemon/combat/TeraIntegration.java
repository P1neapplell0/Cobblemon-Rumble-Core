package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Brawl;
import com.johnn.realtimecobblemon.moves.MoveEngine;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.p1nero.cobblemon.rumblecore.combat.arena.TeraCombat;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

final class TeraIntegration {
    @Mixin(value = MoveEngine.class, remap = false)
    static abstract class MoveDamage {
        @ModifyReturnValue(method = "types", at = @At("RETURN"))
        private static List<String> rumble$defense(List<String> original, Pokemon pokemon) {
            return TeraCombat.defensiveTypes(pokemon, original);
        }

        @WrapOperation(method = "hit", at = @At(value = "INVOKE",
                target = "Lcom/johnn/realtimecobblemon/Smash;hasType(Lcom/cobblemon/mod/common/pokemon/Pokemon;Ljava/lang/String;)Z"))
        private static boolean rumble$powderImmunity(Pokemon pokemon, String type, Operation<Boolean> original) {
            return TeraCombat.hasDefensiveType(pokemon, type, original.call(pokemon, type));
        }

        @WrapOperation(method = "damage", at = @At(value = "INVOKE",
                target = "Lcom/johnn/realtimecobblemon/Smash;hasType(Lcom/cobblemon/mod/common/pokemon/Pokemon;Ljava/lang/String;)Z"))
        private static boolean rumble$stab(Pokemon pokemon, String type, Operation<Boolean> original) {
            return TeraCombat.stab(pokemon, type, original.call(pokemon, type));
        }

        @WrapOperation(method = "damage", at = @At(value = "INVOKE",
                target = "Lcom/johnn/realtimecobblemon/MoveCombat;scaled(IIDDDZFFZZ)F"))
        private static float rumble$sameType(int level, int defenderLevel, double power, double attack, double defense,
                boolean stab, float effectiveness, float random, boolean trainer, boolean defendingTrainer, Operation<Float> original,
                @Local(index = 4) Pokemon pokemon, @Local MoveInfo move) {
            return original.call(level, defenderLevel, power, attack, defense, stab, effectiveness, random, trainer, defendingTrainer)
                    * TeraCombat.boost(pokemon, move.type());
        }

        @WrapOperation(method = "damage", at = @At(value = "INVOKE",
                target = "Lcom/johnn/realtimecobblemon/MoveCombat;scaledVsMob(IDZFZ)F"))
        private static float rumble$sameTypeMob(int level, double power, boolean stab, float random, boolean trainer,
                Operation<Float> original, @Local(index = 4) Pokemon pokemon, @Local MoveInfo move) {
            return original.call(level, power, stab, random, trainer) * TeraCombat.boost(pokemon, move.type());
        }
    }

    @Mixin(value = Brawl.class, remap = false)
    static abstract class TrainerDamage {
        @ModifyReturnValue(method = "types", at = @At("RETURN"))
        private static List<String> rumble$defense(List<String> original, PokemonEntity body) {
            return TeraCombat.defensiveTypes(body.getPokemon(), original);
        }
    }

    @Mixin(value = Smash.class, remap = false)
    static abstract class Session {
        @Inject(method = "end", at = @At("HEAD"))
        private static void rumble$clear(ServerPlayer player, Component reason, boolean teleportBack, CallbackInfo ci) {
            TeraCombat.clear(Smash.session(player));
        }
    }
}
