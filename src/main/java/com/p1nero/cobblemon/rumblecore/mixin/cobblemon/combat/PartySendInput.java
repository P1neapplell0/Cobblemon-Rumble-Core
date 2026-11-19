package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.cobblemon.mod.common.client.keybind.keybinds.PartySendBinding;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.net.messages.server.SendOutPokemonPacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.client.ClientSmash;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaClient;
import com.p1nero.cobblemon.rumblecore.prologue.client.PrologueClient;
import com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets.Cue;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PartySendBinding.class, remap = false)
abstract class PartySendInput {
    // Outside combat retain Cobblemon's target trace: empty space sends a follower, a wild target challenges.
    // In combat RTC owns input (including its lock key); don't also send/recall on the same R release.
    @Inject(method = "checkForTargetInteractions", at = @At("HEAD"), cancellable = true)
    private void rumble$sendSelected(LocalPlayer player, Pokemon selected, CallbackInfo ci) {
        if (ClientSmash.active() || !PrologueClient.allow(Cue.CHALLENGE)) ci.cancel();
        else if (ArenaClient.isArena()) {
            new SendOutPokemonPacket(CobblemonClient.INSTANCE.getStorage().getSelectedSlot()).sendToServer();
            ci.cancel();
        }
    }
}
