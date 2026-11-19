package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.p1nero.cobblemon.rumblecore.prologue.FixedStoryActors;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.johnn.realtimecobblemon.client.BattleCamera", remap = false)
abstract class StoryActorLockOn {
    @Inject(method = "candidates", at = @At("RETURN"), cancellable = true)
    private static void rumble$filter(LocalPlayer player, CallbackInfoReturnable<List<LivingEntity>> cir) {
        var candidates = cir.getReturnValue();
        if (candidates.stream().noneMatch(FixedStoryActors::protectedFromCombat)) return;
        var filtered = new ArrayList<>(candidates);
        filtered.removeIf(FixedStoryActors::protectedFromCombat);
        cir.setReturnValue(filtered);
    }

    @Inject(method = "keep", at = @At("HEAD"), cancellable = true)
    private static void rumble$release(LocalPlayer player, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (FixedStoryActors.protectedFromCombat(target)) cir.setReturnValue(false);
    }
}
