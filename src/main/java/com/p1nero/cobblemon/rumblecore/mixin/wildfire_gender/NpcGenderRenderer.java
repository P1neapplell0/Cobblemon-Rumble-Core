package com.p1nero.cobblemon.rumblecore.mixin.wildfire_gender;

import com.cobblemon.mod.common.client.render.models.blockbench.PosableEntityModel;
import com.cobblemon.mod.common.client.render.npc.NPCRenderer;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.p1nero.cobblemon.rumblecore.client.npc.TrainerGenderLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NPCRenderer.class, remap = false)
abstract class NpcGenderRenderer extends LivingEntityRenderer<NPCEntity, PosableEntityModel<NPCEntity>> {
    protected NpcGenderRenderer(EntityRendererProvider.Context context, PosableEntityModel<NPCEntity> model, float shadow) {
        super(context, model, shadow);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void rumble$addGenderLayer(EntityRendererProvider.Context context, CallbackInfo ci) {
        addLayer(new TrainerGenderLayer(this, context));
    }
}
