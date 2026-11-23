package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.p1nero.cobblemon.rumblecore.prologue.ChampionStory;
import com.p1nero.misc.custom_instances.storage.CompletionStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CompletionStore.class, remap = false)
abstract class HallCompletionLimit {
    // Only the hall requests the full champion roster. The normal leaderboard keeps its own limit.
    @ModifyExpressionValue(method = "leaderboard", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"))
    private int rumble$hallLimit(int cap, String id, boolean team, int requested) {
        return !team && requested == Integer.MAX_VALUE && ChampionStory.TEMPLATE.equals(id) ? requested : cap;
    }
}
