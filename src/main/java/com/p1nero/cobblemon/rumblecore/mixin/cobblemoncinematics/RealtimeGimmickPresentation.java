package com.p1nero.cobblemon.rumblecore.mixin.cobblemoncinematics;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.p1nero.cceib.client.CinematicTestScreen;
import com.p1nero.cceib.client.audio.CinematicSoundPlayer;
import com.p1nero.cobblemon.rumblecore.client.combat.GimmickCinematicScreen;
import com.p1nero.cobblemon.rumblecore.combat.arena.GimmickCinematics;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cobblemon Cinematics 1.4.0: reuse the Pokémon reveal/cry phase without the trainer intro.
 * These hooks are scoped to our RTC screen; native turn-based scenes and debug previews stay untouched. */
@Mixin(targets = "com.p1nero.cceib.client.compat.megashowdown.MegaShowdownCinematicManager", remap = false)
abstract class RealtimeGimmickPresentation {
    @Unique private static CinematicTestScreen rumble$acceptedScreen;

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;"))
    private Screen rumble$acceptRtcPresentation(Minecraft mc) {
        if (!(mc.screen instanceof GimmickCinematicScreen)) return mc.screen;
        if (rumble$acceptedScreen == null) rumble$acceptedScreen = new CinematicTestScreen();
        return rumble$acceptedScreen;
    }

    // Shift both render and tick onto the native reveal timeline so the model, cry keyframes,
    // effects and exit still share the same clock. No global clock or camera changes.
    @Inject(method = "elapsedMs", at = @At("RETURN"), cancellable = true)
    private void rumble$startAtPokemonReveal(CallbackInfoReturnable<Long> cir) {
        if (Minecraft.getInstance().screen instanceof GimmickCinematicScreen) {
            cir.setReturnValue(cir.getReturnValueJ() + GimmickCinematics.TRAINER_INTRO_MS);
        }
    }

    // Do not resolve or render any player/NPC skin for RTC, even when the caster is the opponent.
    @Inject(method = "findTrainerEntity", at = @At("HEAD"), cancellable = true)
    private void rumble$noTrainerModel(CallbackInfoReturnable<LivingEntity> cir) {
        if (Minecraft.getInstance().screen instanceof GimmickCinematicScreen) cir.setReturnValue(null);
    }

    // 1.4.0 schedules these opening cues on a separate clock; skip them rather than playing
    // trainer-intro sounds over the cry. The Pokémon's own cry animation still plays its sound.
    @WrapWithCondition(method = "startNext", at = @At(value = "INVOKE", target =
            "Lcom/p1nero/cceib/client/audio/CinematicSoundPlayer;playSequence(Ljava/lang/String;Ljava/util/List;)V"))
    private boolean rumble$skipTrainerCues(CinematicSoundPlayer sounds, String group, List<?> cues) {
        return !(Minecraft.getInstance().screen instanceof GimmickCinematicScreen);
    }
}
