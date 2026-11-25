package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.api.moves.Move;
import com.johnn.realtimecobblemon.Smash;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Smash.class, remap = false)
abstract class RealtimeCombatFeedback {
    @WrapOperation(method = "sendHud", at = @At(value = "INVOKE", target =
            "Lcom/cobblemon/mod/common/api/moves/Move;getDisplayName()Lnet/minecraft/network/chat/MutableComponent;"))
    private static MutableComponent rumble$displayPp(Move move, Operation<MutableComponent> original) {
        return original.call(move).append(" " + move.getCurrentPp() + "/" + move.getMaxPp());
    }
}
