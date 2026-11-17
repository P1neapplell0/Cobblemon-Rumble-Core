package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.riding;

import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.client.riding.RidingLocatorRepairs;
import java.util.function.BiFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = VaryingModelRepository.class, remap = false)
abstract class RidingModelLocators {
    @WrapOperation(method = "registerModels", at = @At(value = "INVOKE",
            target = "Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object rumble$restoreSeatLocators(BiFunction<?, ?, ?> factory, Object identifier, Object resource,
                                               Operation<Object> original) {
        return original.call(factory, identifier,
                RidingLocatorRepairs.repair((ResourceLocation) identifier, (Resource) resource));
    }
}
