package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.GlideRenderAccess;
import com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.SquashRenderAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.p1nero.cobblemon.rumblecore.client.quest.DialogueActorPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
abstract class LivingRenderCancellation {
    @WrapMethod(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void rumble$faceDialogueViewer(LivingEntity actor, float yaw, float partialTick, PoseStack poses,
            MultiBufferSource buffers, int light, Operation<Void> original) {
        var pose = DialogueActorPose.sample(actor, partialTick);
        if (pose == null) {
            original.call(actor, yaw, partialTick, poses, buffers, light);
            return;
        }
        float body = actor.yBodyRot, oldBody = actor.yBodyRotO, head = actor.yHeadRot, oldHead = actor.yHeadRotO;
        float pitch = actor.getXRot(), oldPitch = actor.xRotO;
        actor.yBodyRot = actor.yBodyRotO = pose.body();
        actor.yHeadRot = actor.yHeadRotO = pose.head();
        actor.setXRot(pose.pitch());
        actor.xRotO = pose.pitch();
        try {
            original.call(actor, pose.body(), partialTick, poses, buffers, light);
        } finally {
            actor.yBodyRot = body; actor.yBodyRotO = oldBody;
            actor.yHeadRot = head; actor.yHeadRotO = oldHead;
            actor.setXRot(pitch); actor.xRotO = oldPitch;
        }
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;"))
    private Event rumble$restoreCanceledPre(IEventBus bus, Event event, Operation<Event> original) {
        if (!(event instanceof RenderLivingEvent.Pre<?, ?> pre)) return original.call(bus, event);
        var poses = pre.getPoseStack();
        var before = poses.last();
        var result = original.call(bus, event);
        // A cancelled Pre never gets a Post. RTC can already have pushed a glide/deformation
        // transform; it can also cancel Pre and recursively render its reactions with the same stack.
        if (pre.isCanceled() && poses.last() != before) {
            var entity = pre.getEntity();
            GlideRenderAccess.rumble$gliding().remove(entity.getId());
            SquashRenderAccess.rumble$pushed().remove(entity);
            while (poses.last() != before && !poses.clear()) poses.popPose();
        }
        return result;
    }
}
