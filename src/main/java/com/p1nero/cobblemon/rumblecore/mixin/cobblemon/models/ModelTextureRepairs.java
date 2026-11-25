package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.models;

import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.google.gson.Gson;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(value = VaryingModelRepository.class, remap = false)
abstract class ModelTextureRepairs {
    @Unique private static final Map<String, String> RUMBLE_TEXTURE_FALLBACKS = Map.of(
            "0707_klefki/a/key_a21.png", "0707_klefki/a/key_a1.png",
            "0707_klefki/b/key_b21.png", "0707_klefki/b/key_b1.png",
            "0707_klefki/c/key_c21.png", "0707_klefki/c/key_c1.png",
            "0707_klefki/d/key_d20.png", "0707_klefki/d/key_d1.png",
            "0381_latios/latios_sygna_shiny.png", "0381_latios/latios_sygna.png",
            "cosmetic/morecosmetics/tinkaton/tinkaton_eternal_shiny.png",
                    "cosmetic/morecosmetics/tinkaton/tinkaton_eternal.png",
            "0197_umbreon/umbreon_starry_sky_emissive_shiny.png", "0197_umbreon/umbreon_starry_sky_shiny_emissive.png"
    );

    @WrapOperation(method = "registerVariations", at = @At(value = "INVOKE",
            target = "Lcom/google/gson/Gson;fromJson(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;"))
    private Object rumble$repairMissingTextures(Gson gson, String json, Class<?> type, Operation<Object> original,
                                                ResourceManager resources) {
        String prefix = "cobblemon:textures/pokemon/";
        for (var entry : RUMBLE_TEXTURE_FALLBACKS.entrySet()) {
            String missing = prefix + entry.getKey();
            String replacement = prefix + entry.getValue();
            String reference = '"' + missing + '"';
            if (json.contains(reference) && resources.getResource(ResourceLocation.parse(missing)).isEmpty()
                    && resources.getResource(ResourceLocation.parse(replacement)).isPresent()) {
                json = json.replace(reference, '"' + replacement + '"');
            }
        }
        return original.call(gson, json, type);
    }
}
