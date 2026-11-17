package com.p1nero.cobblemon.rumblecore.mixin.legendarymonuments;

import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.jorgaomc.legendarymonuments.entities.CalyrexNpcManager;
import com.p1nero.cobblemon.rumblecore.compat.legendarymonuments.dialogue.CalyrexConversation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(targets = "com.jorgaomc.legendarymonuments.entities.CalyrexNpcManager", remap = false)
abstract class CalyrexInteraction {
    @WrapMethod(method = "handleInteraction")
    private static boolean rumble$interact(NPCEntity npc, Player player, InteractionHand hand, Operation<Boolean> original) {
        return player instanceof ServerPlayer serverPlayer && CalyrexNpcManager.isCalyrexNpc(npc)
                ? CalyrexConversation.run(serverPlayer, () -> original.call(npc, player, hand))
                : original.call(npc, player, hand);
    }

    @WrapMethod(method = "handleOption")
    private static int rumble$choose(MinecraftServer server, ServerPlayer player, UUID npc, String option, Operation<Integer> original) {
        return CalyrexConversation.run(player, () -> original.call(server, player, npc, option));
    }

    @Inject(method = "say", at = @At("HEAD"), cancellable = true)
    private static void rumble$say(ServerPlayer player, String text, CallbackInfo ci) {
        if (CalyrexConversation.line(player, text, false)) ci.cancel();
    }

    @Inject(method = "objective", at = @At("HEAD"), cancellable = true)
    private static void rumble$objective(ServerPlayer player, String text, CallbackInfo ci) {
        if (CalyrexConversation.line(player, text, true)) ci.cancel();
    }

    @Inject(method = "sendOptions", at = @At("HEAD"), cancellable = true)
    private static void rumble$options(ServerPlayer player, UUID npc, @Coerce Object first, @Coerce Object second, CallbackInfo ci) {
        var a = (CalyrexOptionAccess) first;
        var b = (CalyrexOptionAccess) second;
        if (CalyrexConversation.options(player, npc, a.rumble$optionId(), a.rumble$optionLabel(), b.rumble$optionId(), b.rumble$optionLabel())) {
            ci.cancel();
        }
    }
}
