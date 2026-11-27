package com.p1nero.cobblemon.rumblecore.mixin.cobblemoncinematics;

import com.p1nero.cceib.client.CinematicTestScreen;
import com.p1nero.cobblemon.rumblecore.client.combat.GimmickCinematicScreen;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** CCEIB 1.4.0 only keeps a presentation alive in BattleGUI or CinematicTestScreen.
 * Adapt that check, not Minecraft's actual screen or pause state. Native turn-based scenes are unchanged. */
@Mixin(targets = "com.p1nero.cceib.client.compat.megashowdown.MegaShowdownCinematicManager", remap = false)
abstract class RealtimeGimmickPresentation {
    @Unique private static CinematicTestScreen rumble$acceptedScreen;

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;"))
    private Screen rumble$acceptRtcPresentation(Minecraft mc) {
        if (!(mc.screen instanceof GimmickCinematicScreen)) return mc.screen;
        if (rumble$acceptedScreen == null) rumble$acceptedScreen = new CinematicTestScreen();
        return rumble$acceptedScreen;
    }

    @Inject(method = "findTrainerId", at = @At("HEAD"), cancellable = true)
    private void rumble$useActualCaster(UUID pokemon, CallbackInfoReturnable<UUID> cir) {
        UUID caster = GimmickCinematicScreen.enqueuingTrainer();
        if (caster != null) cir.setReturnValue(caster);
    }
}
