package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.client.CobblemonClient;
import com.johnn.realtimecobblemon.client.ClientSmash;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ClientSmash.class, remap = false)
abstract class RealtimeSelectedToggle {
    @ModifyArg(method = "onTickPre", at = @At(value = "INVOKE", target = "Lcom/johnn/realtimecobblemon/client/SwitchFx;request(I)V"))
    private static int rumble$selectedSlotWhenNoFollower(int target) {
        var player = Minecraft.getInstance().player;
        if (player == null || ClientSmash.active()) return target;
        // Preserve explicit hits on our own follower; an unrelated crosshair entity should not change the party selection.
        if (player.level().getEntity(target) instanceof com.cobblemon.mod.common.entity.pokemon.PokemonEntity pokemon
                && player.getUUID().equals(pokemon.getPokemon().getOwnerUUID())) return target;
        return -2 - Math.clamp(CobblemonClient.INSTANCE.getStorage().getSelectedSlot(), 0, 5);
    }
}
