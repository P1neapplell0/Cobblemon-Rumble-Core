package com.p1nero.cobblemon.rumblecore.mixin.cobblebash;

import com.cobblemon.mod.common.client.render.npc.NPCRenderer;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.p1nero.cobblemon.rumblecore.integration.cobblebash.SimulatorTrainerAppearance;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NPCRenderer.class, remap = false)
abstract class SimulatorTrainerRenderer {
    @Unique private final Map<NPCEntity, Map.Entry<String, WeakReference<LivingEntity>>> rumble$displays = new WeakHashMap<>();

    @Inject(method = "render(Lcom/cobblemon/mod/common/entity/npc/NPCEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void rumble$renderSimulatorTrainer(NPCEntity npc, float yaw, float partialTick, PoseStack pose,
                                               MultiBufferSource buffers, int light, CallbackInfo ci) {
        String aspect = SimulatorTrainerAppearance.getAspect(npc);
        if (aspect.isEmpty()) return;
        var display = rumble$displays.get(npc);
        var entity = display != null && display.getKey().equals(aspect) ? display.getValue().get() : null;
        if (entity == null) {
            entity = SimulatorTrainerAppearance.createDisplay(npc.level(), aspect);
            if (entity == null) return;
            rumble$displays.put(npc, new AbstractMap.SimpleImmutableEntry<>(aspect, new WeakReference<>(entity)));
        }
        // Only the original Cobblemon NPC is tracked and participates in sending and recalling Pokémon.
        entity.setPos(npc.position());
        entity.setYRot(npc.getYRot());
        entity.yRotO = npc.yRotO;
        entity.setXRot(npc.getXRot());
        entity.xRotO = npc.xRotO;
        entity.yHeadRot = npc.yHeadRot;
        entity.yHeadRotO = npc.yHeadRotO;
        entity.yBodyRot = npc.yBodyRot;
        entity.yBodyRotO = npc.yBodyRotO;
        entity.tickCount = npc.tickCount;
        entity.setInvisible(npc.isInvisible());
        Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity).render(entity, yaw, partialTick, pose, buffers, light);
        ci.cancel();
    }
}
