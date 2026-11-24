package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.client;

import com.cobblemon.mod.common.client.gui.battle.BattleOverlay;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.p1nero.cobblemon.rumblecore.client.combat.RealtimeHealthBar;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BattleOverlay.class, remap = false)
abstract class RealtimeHealthTile {
    @WrapOperation(method = "drawBattleTile", at = @At(value = "INVOKE", ordinal = 5, target =
            "Lcom/cobblemon/mod/common/api/gui/GuiUtilsKt;blitk$default(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/ResourceLocation;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;Ljava/lang/Number;ZFILjava/lang/Object;)V"))
    private void rumble$paintHealth(PoseStack pose, ResourceLocation texture, Number x, Number y, Number height,
                                    Number width, Number u, Number v, Number textureWidth, Number textureHeight,
                                    Number z, Number red, Number green, Number blue, Number alpha, boolean blend,
                                    float scale, int defaults, Object marker, Operation<Void> draw,
                                    @Local(argsOnly = true, ordinal = 0) boolean reversed) {
        var frame = RealtimeHealthBar.painting;
        if (frame == null) {
            draw.call(pose, texture, x, y, height, width, u, v, textureWidth, textureHeight, z,
                    red, green, blue, alpha, blend, scale, defaults, marker);
            return;
        }
        float difference = (frame.trail() - frame.fill()) * 97;
        float filled = frame.fill() * 97;
        float start = x.floatValue() + (reversed ? width.floatValue() : 0);
        if (difference > 0) {
            float trailX = reversed ? start - filled - difference : start + filled;
            draw.call(pose, texture, trailX, y, height, difference, u, v, textureWidth, textureHeight, z,
                    1f, 1f, 1f, alpha, blend, scale, defaults, marker);
        }
        if (filled > 0) {
            draw.call(pose, texture, reversed ? start - filled : start, y, height, filled,
                    u, v, textureWidth, textureHeight, z, red, green, blue, alpha, blend, scale, defaults, marker);
        }
    }
}
