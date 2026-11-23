package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.combat;

import com.bedrockk.molang.Expression;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.pokemon.ai.tasks.MoveToOwnerTask;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The hidden trainer still counts as a party owner; never let its idle follow task take over combat. */
@Mixin(value = MoveToOwnerTask.class, remap = false)
abstract class ArenaOwnerFollow {
    @Inject(method = "create$lambda$0$0$0", at = @At("HEAD"), cancellable = true)
    private static void rumble$arenaNavigation(Expression condition, Expression teleport, Expression maxDistance,
            Expression speed, Expression completionRange, BehaviorBuilder.Instance<?> builder, MemoryAccessor<?, ?> walk,
            ServerLevel level, PokemonEntity pokemon, long time, CallbackInfoReturnable<Boolean> cir) {
        if (ArenaMatch.isTrainerFighter(pokemon)) cir.setReturnValue(false);
    }
}
