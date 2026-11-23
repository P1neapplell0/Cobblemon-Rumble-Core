package com.p1nero.cobblemon.rumblecore.mixin.rct;

import com.gitlab.srcmc.rctapi.api.util.Text;
import com.gitlab.srcmc.rctmod.api.utils.ChatUtils;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.p1nero.cobblemon.rumblecore.integration.rct.RoadTrainerDialogue;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatUtils.class, remap = false)
abstract class TrainerDialogue {
    @Inject(method = "reply(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/player/Player;Ljava/util/function/Function;[Ljava/lang/String;)V",
            at = @At("HEAD"), cancellable = true)
    private static void rumble$presentInArcQuest(LivingEntity speaker, Player player, Function<String, Text[]> lines,
                                                String[] keys, CallbackInfo ci) {
        if (speaker instanceof TrainerMob trainer && player instanceof ServerPlayer serverPlayer
                && RoadTrainerDialogue.route(trainer, serverPlayer, lines, keys)) ci.cancel();
    }
}
