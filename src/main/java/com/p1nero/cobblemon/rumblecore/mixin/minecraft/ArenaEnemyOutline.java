package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.client.combat.BattleNameplates;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
abstract class ArenaEnemyOutline {
    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/Minecraft;shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean rumble$outlineOpponent(Minecraft client, Entity entity, Operation<Boolean> original) {
        return original.call(client, entity) || BattleNameplates.outlined(entity);
    }

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int rumble$opponentOutlineColor(Entity entity, Operation<Integer> original) {
        return BattleNameplates.outlined(entity) ? 0xff5252 : original.call(entity);
    }
}
