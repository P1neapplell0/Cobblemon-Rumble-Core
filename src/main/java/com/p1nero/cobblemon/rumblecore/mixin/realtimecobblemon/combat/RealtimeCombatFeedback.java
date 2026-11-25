package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import com.llamalad7.mixinextras.sugar.Local;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeSignatureMoves;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Smash.class, remap = false)
abstract class RealtimeCombatFeedback {
    @WrapOperation(method = "sendHud", at = @At(value = "INVOKE", target =
            "Lcom/johnn/realtimecobblemon/moves/MoveRegistry;info(Lcom/cobblemon/mod/common/api/moves/Move;)Lcom/johnn/realtimecobblemon/moves/MoveInfo;"))
    private static MoveInfo rumble$heldItemMove(Move move, Operation<MoveInfo> original, @Local Pokemon pokemon) {
        return RealtimeSignatureMoves.resolve(pokemon, move, original.call(move));
    }

    @WrapOperation(method = "sendHud", at = @At(value = "INVOKE", target =
            "Lcom/cobblemon/mod/common/api/moves/Move;getDisplayName()Lnet/minecraft/network/chat/MutableComponent;"))
    private static MutableComponent rumble$displayPp(Move move, Operation<MutableComponent> original, @Local MoveInfo info) {
        var replacement = move.getName().equals(info.id()) ? null : Moves.getByName(info.id());
        var name = replacement == null ? original.call(move) : replacement.getDisplayName();
        return name.append(" " + move.getCurrentPp() + "/" + move.getMaxPp());
    }
}
