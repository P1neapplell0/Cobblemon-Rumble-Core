package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.github.yajatkaul.mega_showdown.tag.MegaShowdownTags;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.ZMoves;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.EquipmentChecking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ZMoves.class, remap = false)
abstract class ZRingRequirement {
    @Inject(method = "activate", at = @At("HEAD"), cancellable = true)
    private static void rumble$requireEquippedRing(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (Smash.session(player) != null && !rumble$wearsRing(player)) {
            player.displayClientMessage(Component.translatable("rumble.combat.z_ring_required"), true);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "fill", at = @At("HEAD"), cancellable = true)
    private static void rumble$chargeOnlyWithRing(Smash.Session session, float amount, CallbackInfo ci) {
        if (!rumble$wearsRing(session.player)) ci.cancel();
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Ljava/lang/Math;round(F)I"))
    private static int rumble$hideMeterWithoutRing(int percent, Smash.Session session) {
        // Realtime hides negative values, and its normal sync notices equipment changes on the next update.
        return rumble$wearsRing(session.player) ? percent : -2;
    }

    @Unique
    private static boolean rumble$wearsRing(ServerPlayer player) {
        if (!ModList.get().isLoaded("mega_showdown") || !ModList.get().isLoaded("accessories")) return false;
        var accessories = AccessoriesCapability.get(player);
        return accessories != null && accessories.isEquipped(stack -> stack.is(MegaShowdownTags.Items.Z_RING)
                || stack.is(MegaShowdownTags.Items.OMNI_RING), EquipmentChecking.ACCESSORIES_ONLY);
    }
}
