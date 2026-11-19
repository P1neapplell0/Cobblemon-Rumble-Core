package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.models;

import com.cobblemon.mod.common.client.render.models.blockbench.PosableEntityModel;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.p1nero.cobblemon.rumblecore.prologue.client.QuarkProfessorGesture;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PosableEntityModel.class, remap = false)
abstract class GymQuarkExpressions {
    @Shadow public abstract PosableModel getPosableModel();

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", at = @At("TAIL"))
    private void rumble$poseTrainer(Entity entity, float limb, float stride, float age, float headYaw, float headPitch, CallbackInfo ci) {
        if (entity instanceof NPCEntity npc && ModList.get().isLoaded("quark")) {
            QuarkProfessorGesture.poseTrainer(npc, getPosableModel());
        }
    }
}
