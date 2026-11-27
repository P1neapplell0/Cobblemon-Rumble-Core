package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.p1nero.cobblemon.rumblecore.combat.arena.GimmickCinematics;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Brawl;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import com.johnn.realtimecobblemon.moves.MoveRegistry;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeSignatureMoves;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Brawl.class, remap = false)
abstract class RealtimeMoveWindupAi {
    @Unique private static final java.util.Map<Object, Boolean> rumble$meleePicks = new java.util.WeakHashMap<>();
    @WrapOperation(method = "land(Lcom/johnn/realtimecobblemon/moves/Brawl$Foe;Lcom/johnn/realtimecobblemon/Smash$Session;"
            + "Lnet/minecraft/server/level/ServerLevel;Lcom/johnn/realtimecobblemon/moves/MoveInfo;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;ZF)F",
            at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/MoveEngine;receive(Lcom/johnn/realtimecobblemon/Smash$Session;Lnet/minecraft/world/damagesource/DamageSource;FLcom/johnn/realtimecobblemon/moves/MoveInfo;)F"))
    private static float rumble$scaleLegendaryDamage(Smash.Session defender, DamageSource source, float damage,
                                                     MoveInfo move, Operation<Float> original) {
        if (source.getEntity() instanceof PokemonEntity attacker && ArenaMatch.isLegendaryFoe(attacker)) damage *= 1.5F;
        return original.call(defender, source, damage, move);
    }

    @WrapOperation(method = "picks", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/MoveRegistry;info(Lcom/cobblemon/mod/common/api/moves/Move;)Lcom/johnn/realtimecobblemon/moves/MoveInfo;"))
    private static MoveInfo rumble$heldItemMove(Move move, Operation<MoveInfo> original, PokemonEntity pokemon) {
        return RealtimeSignatureMoves.resolve(pokemon.getPokemon(), move, original.call(move));
    }

    // Reuse Brawl's stun branch so an interrupted action is reset instead of firing late.
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/Conditions;stunned(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean rumble$checkAiControl(Entity entity, Operation<Boolean> original) {
        boolean blocked = original.call(entity) || RealtimeMoveWindup.crowdControlled(entity)
                || GimmickCinematics.locked(entity);
        if (blocked && entity instanceof PokemonEntity pokemon) RealtimeMoveWindup.npcInterrupted(pokemon);
        return blocked;
    }

    @WrapOperation(method = "decide", at = @At(value = "FIELD", target =
            "Lcom/johnn/realtimecobblemon/moves/Brawl$Foe;moves:Ljava/util/List;", opcode = Opcodes.GETFIELD), require = 2)
    private static List<?> rumble$hideCoolingDownMoves(@Coerce Object foe, Operation<List<?>> original, @Coerce Object sameFoe,
                                                         Smash.Session target, ServerLevel level, double distance, long now) {
        var access = (BrawlFoeAccess) foe;
        var pokemon = access.rumble$mon();
        if (RealtimeMoveWindup.npcCoolingDown(pokemon, pokemon.level().getGameTime())) return List.of();
        return original.call(foe).stream().filter(pick -> {
            var move = ((BrawlPickAccess) pick).rumble$move();
            if (!ArenaMatch.isArena(level)) return true;
            var style = MoveRegistry.style(move);
            double contact = 2.3 + (pokemon.getBbWidth() + target.pokemon.getBbWidth()) * .5;
            return switch (style.shape()) {
                case MELEE -> distance <= contact;
                case DASH -> distance <= 8;
                case SHOCKWAVE -> distance <= style.radius() + 1;
                default -> true;
            };
        }).toList();
    }

    @Inject(method = "startMove", at = @At("HEAD"), cancellable = true)
    private static void rumble$warnBeforeAiMove(@Coerce Object foe, Smash.Session target,
            ServerLevel level, long now, @Coerce Object pick, CallbackInfo ci,
            @Share("windupTicks") LocalIntRef windupTicks) {
        var pokemon = ((BrawlFoeAccess) foe).rumble$mon();
        var move = ((BrawlPickAccess) pick).rumble$move();
        int ticks = RealtimeMoveWindup.npcMove(pokemon, level, now, move);
        if (ticks == 0) ci.cancel();
        else {
            windupTicks.set(ticks);
            rumble$meleePicks.put(foe, RealtimeMoveWindup.isMelee(move));
        }
    }

    @ModifyExpressionValue(method = "startMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"))
    private static int rumble$useWarningDuration(int original, @Share("windupTicks") LocalIntRef windupTicks) {
        return windupTicks.get();
    }

    // The first read is AI tracking during the windup. The second actually launches the attack.
    @ModifyExpressionValue(method = "move", at = @At(value = "FIELD", ordinal = 1, target =
            "Lcom/johnn/realtimecobblemon/moves/Brawl$Foe;seen:Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 rumble$aimAtRelease(Vec3 tracked, @Coerce Object foe, Smash.Session target) {
        return target.player.getBoundingBox().getCenter();
    }

    // RTC 1.0.3alpha.1: the first landed read gates contact damage and the follow-up push.
    @ModifyExpressionValue(method = "move", at = @At(value = "FIELD", ordinal = 0, target =
            "Lcom/johnn/realtimecobblemon/moves/Brawl$Foe;landed:Z"))
    private static boolean rumble$waitForMeleeImpact(boolean landed, @Coerce Object foe, Smash.Session target,
                                                     ServerLevel level, long now) {
        var state = (BrawlFoeAccess) foe;
        return landed || Boolean.TRUE.equals(rumble$meleePicks.get(foe))
                && (now - state.rumble$actAt() < state.rumble$windup() + RealtimeMoveWindup.MELEE_IMPACT_TICKS
                    || !state.rumble$mon().hasLineOfSight(target.player));
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/Fx;animateMove(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;Lcom/johnn/realtimecobblemon/moves/MoveInfo;I)V"))
    private static void rumble$startCooldownOnRelease(CallbackInfo ci, @Local PokemonEntity pokemon) {
        RealtimeMoveWindup.npcReleased(pokemon, pokemon.level().getGameTime());
    }
}
