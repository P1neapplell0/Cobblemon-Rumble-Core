package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Food restores health instead of funding Minecraft's hunger-based regeneration loop. */
@Mixin(Player.class)
public abstract class FullStomach {
    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void rumble$canEatWhileInjured(boolean alwaysEdible, CallbackInfoReturnable<Boolean> cir) {
        if (((Player) (Object) this).isHurt()) cir.setReturnValue(true);
    }

    @Inject(method = "eat", at = @At("RETURN"))
    private void rumble$healAfterEating(Level level, ItemStack stack, FoodProperties food,
                                       CallbackInfoReturnable<ItemStack> cir) {
        if (!level.isClientSide() && food.nutrition() > 0) ((Player) (Object) this).heal(food.nutrition());
    }

    @Mixin(FoodData.class)
    public abstract static class ResetFoodData {
        @Shadow private int foodLevel;
        @Shadow private int lastFoodLevel;
        @Shadow private int tickTimer;
        @Shadow private float saturationLevel;
        @Shadow private float exhaustionLevel;

        @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
        private void rumble$keepFull(Player player, CallbackInfo ci) {
            foodLevel = lastFoodLevel = 20;
            saturationLevel = exhaustionLevel = 0;
            tickTimer = 0;
            ci.cancel();
        }
    }
}
