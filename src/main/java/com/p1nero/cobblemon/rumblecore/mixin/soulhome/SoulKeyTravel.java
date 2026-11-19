package com.p1nero.cobblemon.rumblecore.mixin.soulhome;

import com.p1nero.cobblemon.rumblecore.prologue.PersonalDimension;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class SoulKeyTravel {
    private static final String NEARBY = "Lleaf/soulhome/utils/EntityHelper;getEntitiesInRange(Lnet/minecraft/world/entity/Entity;DZ)Ljava/util/List;";
    private static final String FINISH = "finishUsingItem";

    @Mixin(targets = "leaf.soulhome.items.SoulKeyItem", remap = false)
    public abstract static class Unbound {
        @ModifyConstant(method = "getUseDuration", constant = @Constant(intValue = 80))
        private int rumble$chargeTime(int duration) { return 40; }

        @Redirect(method = FINISH, at = @At(value = "INVOKE", target = NEARBY))
        private List<Entity> rumble$onlyUser(Entity user, double radius, boolean includeUser) { return List.of(user); }

        @Inject(method = FINISH, at = @At("HEAD"), cancellable = true)
        private void rumble$checkAccess(ItemStack stack, Level level, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
            if (user instanceof ServerPlayer player && !PersonalDimension.allowKeyTravel(player)) cir.setReturnValue(stack);
        }
    }

    @Mixin(targets = "leaf.soulhome.items.BoundSoulkey", remap = false)
    public abstract static class Bound {
        @Redirect(method = FINISH, at = @At(value = "INVOKE", target = NEARBY))
        private List<Entity> rumble$onlyUser(Entity user, double radius, boolean includeUser) { return List.of(user); }

        @Inject(method = FINISH, at = @At("HEAD"), cancellable = true)
        private void rumble$checkAccess(ItemStack stack, Level level, LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
            if (user instanceof ServerPlayer player && !PersonalDimension.allowKeyTravel(player)) cir.setReturnValue(stack);
        }
    }
}
