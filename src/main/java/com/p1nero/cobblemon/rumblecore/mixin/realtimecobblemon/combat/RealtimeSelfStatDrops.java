package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.MoveEngine;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeCastStages;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/** RTC 1.0.3alpha.1: recheck fire's scheduled lambda and the cast-specific deal overload on upgrades. */
@Mixin(value = MoveEngine.class, remap = false)
abstract class RealtimeSelfStatDrops {
    @WrapOperation(method = "lambda$fire$13", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/MoveEngine;selfStages(Lcom/johnn/realtimecobblemon/Smash$Session;Ljava/util/Map;)V"))
    private static void rumble$deferNegativeStages(Smash.Session session, Map<String, Integer> changes,
                                                   Operation<Void> original) {
        if (changes.values().stream().noneMatch(delta -> delta < 0)) {
            original.call(session, changes);
            return;
        }
        var immediate = new LinkedHashMap<>(changes);
        immediate.values().removeIf(delta -> delta < 0);
        if (!immediate.isEmpty()) original.call(session, immediate);
    }

    @WrapOperation(method = "deal(Lcom/johnn/realtimecobblemon/moves/MoveEngine$Cast;Lnet/minecraft/world/entity/LivingEntity;F)F",
            at = @At(value = "INVOKE", target =
                    "Lcom/johnn/realtimecobblemon/moves/MoveEngine;deal(Lcom/johnn/realtimecobblemon/Smash$Session;Lnet/minecraft/world/entity/LivingEntity;F)F"))
    private static float rumble$confirmDamage(Smash.Session session, LivingEntity target, float amount,
                                               Operation<Float> original, @Coerce Object cast) {
        var stages = (RealtimeCastStages) cast;
        if (!stages.rumble$hasPendingStatDrop()) return original.call(session, target, amount);

        // RTC can return nominal damage even when hurt() is rejected. Read the actual
        // HP pool, including the controlled Pokemon behind a PvP player entity.
        var pokemon = Smash.pokemonBehind(target);
        float before = pokemon == null ? target.getHealth() : pokemon.getCurrentHealth();
        float dealt = original.call(session, target, amount);
        float after = pokemon == null ? target.getHealth() : pokemon.getCurrentHealth();
        if (dealt > 0 && after < before) stages.rumble$applyStatDrop();
        return dealt;
    }
}

/** RTC 1.0.3alpha.1 shares a Cast across its projectiles, targets and repeated hits. */
@Mixin(targets = "com.johnn.realtimecobblemon.moves.MoveEngine$Cast", remap = false)
abstract class RealtimeSelfStatDropCast implements RealtimeCastStages {
    @Shadow @Final Smash.Session s;
    @Shadow @Final MoveInfo m;
    @Unique private boolean rumble$statDropApplied;

    @Override
    public boolean rumble$hasPendingStatDrop() {
        return !rumble$statDropApplied && !m.isStatus() && m.selfBoosts().values().stream().anyMatch(delta -> delta < 0);
    }

    @Override
    public void rumble$applyStatDrop() {
        if (rumble$statDropApplied || Smash.session(s.player) != s) return;
        rumble$statDropApplied = true;
        var drops = new LinkedHashMap<>(m.selfBoosts());
        drops.values().removeIf(delta -> delta >= 0);
        MoveEngine.selfStages(s, drops);
    }
}
