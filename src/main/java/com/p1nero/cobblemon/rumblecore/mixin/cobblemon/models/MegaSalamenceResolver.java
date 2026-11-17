package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.models;

import com.cobblemon.mod.common.client.render.ModelAssetVariation;
import com.cobblemon.mod.common.client.render.ModelVariationSet;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.google.gson.Gson;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = VaryingModelRepository.class, remap = false)
abstract class MegaSalamenceResolver {
    @Unique private static final ResourceLocation RUMBLE_SALAMENCE = ResourceLocation.parse("cobblemon:salamence");
    @Unique private static final ResourceLocation RUMBLE_MEGA_RIG = ResourceLocation.parse("cobblemon:salamencemega.geo");
    @Unique private static final ResourceLocation RUMBLE_MEGA_POSER = ResourceLocation.parse("cobblemon:salamence_mega");
    @Unique private static final ResourceLocation RUMBLE_MEGA_POSER_FILE = ResourceLocation.parse(
            "cobblemon:bedrock/pokemon/posers/0373_salamence/salamence_mega.json");

    @WrapOperation(method = "registerVariations", at = @At(value = "INVOKE",
            target = "Lcom/google/gson/Gson;fromJson(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;"))
    private Object rumble$restoreMegaPoser(Gson gson, String json, Class<?> type, Operation<Object> original,
                                         ResourceManager resources) {
        var result = original.call(gson, json, type);
        if (!(result instanceof ModelVariationSet set) || !RUMBLE_SALAMENCE.equals(set.getName())
                || set.getVariations().stream().noneMatch(MegaSalamenceResolver::rumble$isMissingMegaPoser)
                || resources.getResource(RUMBLE_MEGA_POSER_FILE).isEmpty()) return result;

        return new ModelVariationSet(set.getName(), set.getOrder(), set.getVariations().stream()
                .map(variation -> rumble$isMissingMegaPoser(variation)
                        ? new ModelAssetVariation(variation.getAspects(), variation.getCondition(), RUMBLE_MEGA_POSER,
                                variation.getModel(), variation.getTexture(), variation.getLayers(), variation.getSprites())
                        : variation)
                .toList());
    }

    @Unique
    private static boolean rumble$isMissingMegaPoser(ModelAssetVariation variation) {
        return variation.getPoser() == null && RUMBLE_MEGA_RIG.equals(variation.getModel())
                && variation.getAspects().contains("mega");
    }
}
