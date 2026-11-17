package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.GlideRenderAccess;
import com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.SquashRenderAccess;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
abstract class LivingRenderCancellation {
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
