package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.spawning;

import com.cobblemon.mod.common.api.spawning.detail.NPCSpawnAction;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnAction;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {PokemonSpawnAction.class, NPCSpawnAction.class}, remap = false)
abstract class PrologueSpawnActions {
    // The Cobblemon spawn-action pipeline is also used by spawners other than PlayerSpawner.
    @Inject(method = "createEntity()Lnet/minecraft/world/entity/Entity;", at = @At("HEAD"), cancellable = true)
    private void rumble$blockAmbientSpawnAction(CallbackInfoReturnable<Entity> cir) {
        var action = (SpawnAction<?>) (Object) this;
        if (PrologueWorld.blocksAmbientSpawns(action.getSpawnablePosition().getWorld())) cir.setReturnValue(null);
    }
}
