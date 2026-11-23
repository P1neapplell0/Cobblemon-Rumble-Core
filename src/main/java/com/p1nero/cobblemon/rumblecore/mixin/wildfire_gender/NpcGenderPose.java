package com.p1nero.cobblemon.rumblecore.mixin.wildfire_gender;

import com.cobblemon.mod.common.client.render.models.blockbench.PosableEntityModel;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.p1nero.cobblemon.rumblecore.client.npc.TrainerGenderLayer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PosableEntityModel.class)
abstract class NpcGenderPose {
    @Inject(method = "renderToBuffer", at = @At(value = "INVOKE", remap = false,
            target = "Lcom/cobblemon/mod/common/client/render/models/blockbench/PosableModel;setDefault()V"))
    private void rumble$renderWithAnimatedTorso(PoseStack pose, VertexConsumer vertices, int light, int overlay, int color,
                                               CallbackInfo ci) {
        var model = (PosableEntityModel<?>) (Object) this;
        if (!(model.getContext().getEntity() instanceof NPCEntity npc)) return;
        var buffers = model.getPosableModel().getBufferProvider();
        if (buffers != null && Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(npc) instanceof TrainerGenderLayer.Owner owner) {
            owner.rumble$trainerGender().render(pose, buffers, light, npc, model, ((PosableState) npc.getDelegate()).getPartialTicks());
        }
    }
}
