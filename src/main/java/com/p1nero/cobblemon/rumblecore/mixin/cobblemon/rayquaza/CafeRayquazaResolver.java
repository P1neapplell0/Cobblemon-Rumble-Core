package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.rayquaza;

import com.cobblemon.mod.common.client.render.ModelAssetVariation;
import com.cobblemon.mod.common.client.render.ModelVariationSet;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.google.gson.Gson;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = VaryingModelRepository.class, remap = false)
abstract class CafeRayquazaResolver {
    @Unique private static final ResourceLocation RUMBLE_RAYQUAZA = ResourceLocation.parse("cobblemon:rayquaza");
    @Unique private static final ResourceLocation RUMBLE_CAFE_RIG = ResourceLocation.parse("cobblemon:lunar_rayquaza.geo");
    @Unique private static final ResourceLocation RUMBLE_CAFE_POSER = ResourceLocation.parse("cobblemon_rumble_core:cafe_rayquaza");

    @WrapOperation(method = "registerVariations", at = @At(value = "INVOKE",
            target = "Lcom/google/gson/Gson;fromJson(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;"))
    private Object rumble$pairCafeRigWithItsPoser(Gson gson, String json, Class<?> type, Operation<Object> original) {
        var result = original.call(gson, json, type);
        if (!(result instanceof ModelVariationSet set) || !RUMBLE_RAYQUAZA.equals(set.getName())
                || set.getVariations().stream().noneMatch(variation -> rumble$usesCafeRig(variation))) return result;

        return new ModelVariationSet(set.getName(), set.getOrder(), set.getVariations().stream()
                .map(variation -> rumble$usesCafeRig(variation)
                        ? new ModelAssetVariation(variation.getAspects(), variation.getCondition(), RUMBLE_CAFE_POSER,
                                variation.getModel(), variation.getTexture(), variation.getLayers(), variation.getSprites())
                        : variation)
                .toList());
    }

    @Unique
    private static boolean rumble$usesCafeRig(ModelAssetVariation variation) {
        return RUMBLE_CAFE_RIG.equals(variation.getModel()) && RUMBLE_RAYQUAZA.equals(variation.getPoser());
    }
}
