package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.api.moves.Move;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.network.SmashPayloads;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Smash.class, remap = false)
abstract class RealtimeCombatFeedback {
    @WrapOperation(method = "sendHud", at = @At(value = "INVOKE", target =
            "Lcom/cobblemon/mod/common/api/moves/Move;getDisplayName()Lnet/minecraft/network/chat/MutableComponent;"))
    private static MutableComponent rumble$displayPp(Move move, Operation<MutableComponent> original) {
        return original.call(move).append(" " + move.getCurrentPp() + "/" + move.getMaxPp());
    }

    @Inject(method = "hurtPokemon", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/level/ServerLevel;broadcastDamageEvent(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private static void rumble$hitFeedback(Smash.Session victim, float damage, DamageSource source, CallbackInfo ci) {
        if (damage <= 0) return;
        boolean ko = victim.pokemon.getPokemon().isFainted();
        Smash.send(victim.player, new SmashPayloads.Shake(ko ? 1F : .44F));
        var attacker = source.getEntity();
        var control = attacker != null ? Smash.sessionOf(attacker) : null;
        ServerPlayer player = attacker instanceof ServerPlayer p ? p : control != null ? control.player : null;
        if (player != null && player != victim.player) Smash.send(player, new SmashPayloads.Shake(ko ? .67F : .24F));
    }
}
