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
import com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining;
import com.p1nero.misc.custom_instances.api.InstanceApi;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

final class ArenaIntegration {
    @Mixin(value = com.johnn.realtimecobblemon.MegaEvo.class, remap = false)
    static abstract class Mega {
        @Inject(method = "begin", at = @At("RETURN"))
        private static void rumble$protectMegaCast(ServerPlayer player, Smash.Session session, String choice, CallbackInfo ci) {
            com.p1nero.cobblemon.rumblecore.combat.arena.RealtimeProtection.beginMega(session.pokemon);
        }

        @Inject(method = {"onSmashEnd", "forceEvolve"}, at = @At("HEAD"), require = 1)
        private static void rumble$endMegaCast(ServerPlayer player, CallbackInfo ci) {
            var session = Smash.session(player);
            if (session != null) com.p1nero.cobblemon.rumblecore.combat.arena.RealtimeProtection.endMega(session.pokemon);
        }

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
        @WrapOperation(method = "end", at = @At(value = "INVOKE", target =
                "Lnet/minecraft/server/level/ServerPlayer;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V", remap = true))
        private static void rumble$leaveOwnPosition(ServerPlayer player, ServerLevel destination, double x, double y,
                                                   double z, float yaw, float pitch, Operation<Void> original) {
            // InstanceManager owns arena exit. RTC's origin points back into the arena.
            if (!ArenaMatch.isArena(destination)) original.call(player, destination, x, y, z, yaw, pitch);
        }

        @Inject(method = "onPlayerTick", at = @At(value = "INVOKE", target =
                "Lcom/cobblemon/mod/common/pokemon/Pokemon;isFainted()Z"), cancellable = true)
        private static void rumble$holdFaintedCamera(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event,
                                                     CallbackInfo ci, @Local Smash.Session session) {
            if (session.pokemon.getPokemon().isFainted() && ArenaMatch.keepKnockoutBody(session)) ci.cancel();
        }

        @WrapOperation(method = "hurtPokemon", at = @At(value = "INVOKE", target =
                "Lcom/johnn/realtimecobblemon/Smash;end(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/Component;Z)V"))
        private static void rumble$keepKoControl(ServerPlayer player, Component message, boolean returnToOrigin,
                                                 Operation<Void> original, @Local(argsOnly = true) Smash.Session session) {
            if (!ArenaMatch.keepKnockoutBody(session)) original.call(player, message, returnToOrigin);
        }

        @WrapOperation(method = "hurtPokemon", at = @At(value = "INVOKE", target =
                "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;recallWithAnimation()Ljava/util/concurrent/CompletableFuture;"))
        private static java.util.concurrent.CompletableFuture<?> rumble$keepKoBody(PokemonEntity pokemon,
                Operation<java.util.concurrent.CompletableFuture<?>> original, @Local(argsOnly = true) Smash.Session session) {
            return ArenaMatch.keepKnockoutBody(session) ? java.util.concurrent.CompletableFuture.completedFuture(null) : original.call(pokemon);
        }

        @Inject(method = "start", at = @At(value = "INVOKE", target =
                "Lcom/johnn/realtimecobblemon/Smash;applyBody(Lnet/minecraft/server/level/ServerPlayer;Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;Z)V"))
        private static void rumble$startAtPlayer(ServerPlayer player, PokemonEntity requested,
                                                CallbackInfoReturnable<Component> cir,
                                                @Local(index = 2) PokemonEntity controlled) {
            if (controlled.level() != player.level() || !rumble$isFreeRoam(player)) return;
            controlled.teleportTo(player.getX(), player.getY(), player.getZ());
            controlled.setYRot(player.getYRot());
            controlled.setYHeadRot(player.getYRot());
            controlled.setYBodyRot(player.getYRot());
        }

        @Inject(method = "end", at = @At(value = "INVOKE", target =
                "Lcom/johnn/realtimecobblemon/Smash;sendState(Lnet/minecraft/server/level/ServerPlayer;I)V"))
        private static void rumble$exitAtPokemon(ServerPlayer player, Component message, boolean returnToOrigin,
                                                CallbackInfo ci, @Local(index = 5) PokemonEntity controlled) {
            if (returnToOrigin || !player.isAlive() || controlled.isRemoved()
                    || controlled.level() != player.level() || !rumble$isFreeRoam(player)) return;
            var level = player.serverLevel();
            var destination = controlled.position();
            if (player.position().distanceToSqr(destination) < 1.0e-6) return;
            var box = player.getBoundingBox().move(destination.subtract(player.position())).deflate(1.0e-4);
            // The human hitbox is taller than many Pokémon; retain RTC's fallback in tight spaces.
            if (level.getWorldBorder().isWithinBounds(box) && !level.getBlockCollisions(player, box).iterator().hasNext()) {
                player.teleportTo(destination.x, destination.y, destination.z);
                player.fallDistance = 0;
            }
        }

        @Unique
        private static boolean rumble$isFreeRoam(ServerPlayer player) {
            var level = player.serverLevel();
            return InstanceApi.find(level).isEmpty()
                    && !PrologueTraining.isTrainingLevel(level)
                    && Duels.groupOf(player) == null;
        }

        @Inject(method = "sendState", at = @At("HEAD"), cancellable = true)
        private static void rumble$keepCamera(ServerPlayer player, int entity, CallbackInfo ci) {
            if (entity < 0 && ArenaMatch.retainClientControl(player)) ci.cancel();
        }
    }

    @Mixin(value = Brawl.class, remap = false)
    static abstract class TrainerAI {
        @org.spongepowered.asm.mixin.injection.ModifyConstant(method = "decide",
                constant = @org.spongepowered.asm.mixin.injection.Constant(floatValue = .55f))
        private static float rumble$prioritizeArenaMoves(float jabChance,
                @org.spongepowered.asm.mixin.injection.Coerce Object foe, Smash.Session target,
                ServerLevel level, double distance, long now) {
            return ArenaMatch.inArena(target.player) == null ? jabChance : .14f;
        }

        @Inject(method = "aggro", at = @At("HEAD"), cancellable = true)
        private static void rumble$passiveWildAggro(CallbackInfo ci) {
            // This scan is only for unsolicited wild aggro. Hits and trained arena foes provoke separately.
            ci.cancel();
        }

        @Inject(method = "provoke", at = @At("HEAD"), cancellable = true)
        private static void rumble$cinemaFaction(Smash.Session session, LivingEntity target, CallbackInfo ci) {
            if (target instanceof PokemonEntity pokemon && ArenaMatch.cinemaActorDisallowsBrawl(pokemon)) ci.cancel();
        }

        @Inject(method = "valid", at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;isAlive()Z"), cancellable = true)
        private static void rumble$cinemaBrawlTarget(CallbackInfoReturnable<Smash.Session> cir, @Local PokemonEntity pokemon) {
            if (ArenaMatch.cinemaActorDisallowsBrawl(pokemon)) cir.setReturnValue(null);
        }

        @Inject(method = "tick", at = @At(value = "INVOKE", target =
                "Lcom/johnn/realtimecobblemon/moves/Brawl;walk(Lcom/johnn/realtimecobblemon/moves/Brawl$Foe;Lcom/johnn/realtimecobblemon/Smash$Session;DJ)V"), cancellable = true)
        private static void rumble$anticipateCast(@org.spongepowered.asm.mixin.injection.Coerce Object foe,
                Smash.Session attacker, CallbackInfo ci, @Local PokemonEntity pokemon,
                @Local net.minecraft.server.level.ServerLevel level) {
            if (com.p1nero.cobblemon.rumblecore.combat.arena.ArenaDefense.react(pokemon, attacker, level)) ci.cancel();
        }

        @Inject(method = "parries", at = @At("HEAD"), cancellable = true)
        private static void rumble$replaceReactiveParry(Smash.Session attacker, LivingEntity target,
                CallbackInfoReturnable<Boolean> cir) {
            if (target instanceof PokemonEntity foe && ArenaMatch.defenseTier(foe) != 0) cir.setReturnValue(false);
        }

        @Inject(method = "provoke", at = @At("RETURN"))
        private static void rumble$combatMusic(Smash.Session session, LivingEntity target, CallbackInfo ci) {
            if (target instanceof PokemonEntity opponent && Brawl.fighting(opponent)) BattleMusic.engage(session, opponent, false);
        }

        @ModifyExpressionValue(method = "onServerTick", at = @At(value = "CONSTANT", args = "doubleValue=16.0"))
        private static double rumble$trainerAcquireRange(double range, @Local PokemonEntity mon) {
            return ArenaMatch.isTrainerFighter(mon) || ArenaMatch.isLegendaryFoe(mon) || ArenaMatch.isCinemaActor(mon) ? 56.0 : range;
        }

        @ModifyExpressionValue(method = "valid", at = @At(value = "CONSTANT", args = "doubleValue=32.0"))
        private static double rumble$trainerChaseRange(double range, @Local PokemonEntity mon) {
            return ArenaMatch.isTrainerFighter(mon) || ArenaMatch.isLegendaryFoe(mon) || ArenaMatch.isCinemaActor(mon) ? 80.0 : range;
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
