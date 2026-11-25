package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.moves.Kits;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Kits.class, remap = false)
abstract class RealtimeKitMovePp {
    @Unique private static final Map<Pokemon, Map<Move, Move>> rumble$kitCopies = new WeakHashMap<>();

    @Inject(method = "moves(Lcom/cobblemon/mod/common/pokemon/Pokemon;Z)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
    private static void rumble$useEquippedMoves(Pokemon pokemon, boolean duel, CallbackInfoReturnable<List<Move>> cir) {
        var learned = pokemon.getMoveSet().getMoves();
        if (pokemon.getOwnerUUID() != null && !learned.isEmpty()) cir.setReturnValue(learned);
    }

    @Inject(method = "moves(Lcom/cobblemon/mod/common/pokemon/Pokemon;Z)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private static void rumble$kitPpPerPokemon(Pokemon pokemon, boolean duel, CallbackInfoReturnable<List<Move>> cir) {
        if (duel) return;
        var moves = cir.getReturnValue();
        var learned = pokemon.getMoveSet().getMoves();
        if (moves == learned || moves.equals(learned)) return;
        // RTC caches kit templates by species. A cast must never spend a different player's PP.
        synchronized (rumble$kitCopies) {
            var copies = rumble$kitCopies.computeIfAbsent(pokemon, ignored -> new IdentityHashMap<>());
            cir.setReturnValue(moves.stream().map(move -> copies.computeIfAbsent(move, Move::copy)).toList());
        }
    }
}
