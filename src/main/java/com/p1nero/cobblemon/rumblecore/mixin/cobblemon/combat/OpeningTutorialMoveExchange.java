package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.net.messages.server.BenchMovePacket;
import com.cobblemon.mod.common.net.serverhandling.storage.BenchMoveHandler;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.p1nero.cobblemon.rumblecore.prologue.OpeningTutorial;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Cobblemon 1.8.4: observes the native candidate-move exchange after it succeeds.
 * If BenchMoveHandler changes in a future Cobblemon update, re-check this invocation
 * descriptor and the local Pokemon before removing or adjusting this mixin.
 */
@Mixin(value = BenchMoveHandler.class, remap = false)
abstract class OpeningTutorialMoveExchange {
    @WrapOperation(method = "handle(Lcom/cobblemon/mod/common/net/messages/server/BenchMovePacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lcom/cobblemon/mod/common/pokemon/Pokemon;exchangeMove(Lcom/cobblemon/mod/common/api/moves/MoveTemplate;Lcom/cobblemon/mod/common/api/moves/MoveTemplate;)Z"))
    private boolean rumble$observeExchange(Pokemon pokemon, MoveTemplate oldMove, MoveTemplate newMove,
                                            Operation<Boolean> original, BenchMovePacket packet,
                                            MinecraftServer server, ServerPlayer player) {
        boolean changed = original.call(pokemon, oldMove, newMove);
        if (changed && oldMove != null) OpeningTutorial.moveChanged(player);
        return changed;
    }
}
