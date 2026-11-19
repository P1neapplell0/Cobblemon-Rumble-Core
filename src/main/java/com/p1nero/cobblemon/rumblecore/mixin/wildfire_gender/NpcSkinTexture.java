package com.p1nero.cobblemon.rumblecore.mixin.wildfire_gender;

import com.cobblemon.mod.common.client.render.npc.NPCRenderer;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.wildfire.render.GenderLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GenderLayer.class, remap = false)
abstract class NpcSkinTexture {
    @Inject(method = "getBreastTexture", at = @At("HEAD"), cancellable = true)
    private void rumble$texture(LivingEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        if (entity instanceof NPCEntity npc && Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(npc) instanceof NPCRenderer renderer) {
            cir.setReturnValue(renderer.getTextureLocation(npc));
        }
    }
}
