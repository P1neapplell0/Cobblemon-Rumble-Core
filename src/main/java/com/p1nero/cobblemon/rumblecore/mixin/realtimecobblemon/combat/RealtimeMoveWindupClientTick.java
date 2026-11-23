package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.client.ClientSmash;
import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeMoveWindupClient;
import java.util.Arrays;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientSmash.class, remap = false)
abstract class RealtimeMoveWindupClientTick {
    @Shadow @Final private static long[] chargeStart;
    @Shadow private static Vec3 lungeTo;

    @Inject(method = "onTickPre", at = @At("HEAD"), cancellable = true)
    private static void rumble$ignoreClientCombatInput(ClientTickEvent.Pre event, CallbackInfo ci) {
        if (RealtimeMoveWindupClient.active()) {
            Arrays.fill(chargeStart, -1L);
            lungeTo = null;
            KeyMapping.releaseAll();
            ci.cancel();
        }
    }
}
