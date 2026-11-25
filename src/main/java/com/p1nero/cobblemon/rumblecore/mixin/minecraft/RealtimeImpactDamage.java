package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeDamageFeedback;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
abstract class RealtimeImpactDamage {
    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void rumble$actualHpLost(LivingEntity victim, float health, Operation<Void> original,
            DamageSource source, float amount) {
        float before = victim.getHealth();
        original.call(victim, health);
        RealtimeDamageFeedback.impact(victim, source, before - victim.getHealth(), victim.getMaxHealth());
    }
}
