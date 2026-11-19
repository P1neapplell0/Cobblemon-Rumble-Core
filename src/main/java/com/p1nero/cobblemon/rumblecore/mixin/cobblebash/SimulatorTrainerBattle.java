package com.p1nero.cobblemon.rumblecore.mixin.cobblebash;

import com.gitlab.srcmc.rctapi.api.trainer.TrainerRegistry;
import com.nore.cobblebash.integration.GymBattleFormat;
import com.nore.cobblebash.integration.RctApiProbe;
import com.p1nero.cobblemon.rumblecore.integration.cobblebash.SimulatorArena;
import java.lang.ref.WeakReference;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RctApiProbe.class, remap = false)
abstract class SimulatorTrainerBattle {
    @Unique private static WeakReference<MinecraftServer> rumble$registryServer = new WeakReference<>(null);

    @Redirect(method = "registerGymTrainer(Lnet/minecraft/server/MinecraftServer;Ljava/lang/String;ILjava/lang/String;ILjava/lang/String;)Z",
            at = @At(value = "INVOKE", target = "Lcom/gitlab/srcmc/rctapi/api/trainer/TrainerRegistry;init(Lnet/minecraft/server/MinecraftServer;)V"))
    private static void rumble$initializeRegistryOnce(TrainerRegistry registry, MinecraftServer server) {
        // RCTAPI init clears the registry, including trainers in other active gym slots.
        if (rumble$registryServer.get() == server) return;
        registry.init(server);
        rumble$registryServer = new WeakReference<>(server);
    }

    @Inject(method = "startGymBattle(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;ILjava/lang/String;Lcom/nore/cobblebash/integration/GymBattleFormat;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void rumble$arena(ServerPlayer player, String gym, int slot, String trainer, GymBattleFormat format,
                                     CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(SimulatorArena.challenge(player, gym, slot, trainer));
    }
}
