package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Decoys;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Iterator;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Decoys.class, remap = false)
abstract class DecoyTickSafety {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private static Iterator<PokemonEntity> rumble$iterateStableDecoys(List<PokemonEntity> decoys,
            Operation<Iterator<PokemonEntity>> original) {
        // A decoy's strike can synchronously end the session, which clears this very list.
        return List.copyOf(decoys).iterator();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/johnn/realtimecobblemon/moves/Decoys$Swarm;step(Lcom/johnn/realtimecobblemon/Smash$Session;Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;Lnet/minecraft/world/entity/LivingEntity;J)V"))
    private static void rumble$skipRetiredDecoy(Smash.Session session, PokemonEntity decoy,
            LivingEntity target, long now, Operation<Void> original) {
        if (Smash.session(session.player) == session && decoy.isAlive() && session.combat.decoys.contains(decoy)) {
            original.call(session, decoy, target, now);
        }
    }
}
