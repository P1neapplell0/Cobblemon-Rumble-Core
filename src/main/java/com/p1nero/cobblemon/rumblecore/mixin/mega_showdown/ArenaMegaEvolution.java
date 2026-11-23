package com.p1nero.cobblemon.rumblecore.mixin.mega_showdown;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.yajatkaul.mega_showdown.api.codec.Effect;
import com.github.yajatkaul.mega_showdown.gimmick.MegaGimmick;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MegaGimmick.class, remap = false)
abstract class ArenaMegaEvolution {
    @ModifyExpressionValue(method = "megaEvolve", at = @At(value = "INVOKE",
            target = "Lcom/github/yajatkaul/mega_showdown/api/codec/Effect;getEffect(Ljava/lang/String;)Lcom/github/yajatkaul/mega_showdown/api/codec/Effect;"), require = 2)
    private static Effect rumble$applyFormAfterRealtimeEffect(Effect original, Pokemon pokemon) {
        var body = pokemon.getEntity();
        // The arena already played RTC's cast. An empty MSD effect applies the same form synchronously.
        return body != null && body.level() instanceof ServerLevel level && ArenaMatch.isArena(level)
                ? Effect.empty() : original;
    }
}
