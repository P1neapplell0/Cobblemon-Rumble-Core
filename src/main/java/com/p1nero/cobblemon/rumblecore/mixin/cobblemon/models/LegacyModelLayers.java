package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.models;

import com.cobblemon.mod.common.client.render.ModelLayer;
import com.cobblemon.mod.common.client.render.VaryingRenderableResolver;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.Set;

@Mixin(value = VaryingRenderableResolver.class, remap = false)
abstract class LegacyModelLayers {
    @Unique private static final Set<ResourceLocation> RUMBLE_ARMORED_MODELS = Set.of(
            ResourceLocation.parse("cobblemon:mewtwo_armored_ll.geo"),
            ResourceLocation.parse("cobblemon:mewtwo_armored_mega.geo"));
    @Unique private static final Set<ResourceLocation> RUMBLE_LEGACY_ARMOR = Set.of(
            ResourceLocation.parse("cobblemon:textures/pokemon/0150_mewtwo/mewtwo_armored.png"),
            ResourceLocation.parse("cobblemon:textures/pokemon/0150_mewtwo/mewtwo_armored_shiny.png"));
    @Unique private static final Set<ResourceLocation> RUMBLE_ETERNAL_MODELS = Set.of(
            ResourceLocation.parse("cobblemon:floette_eternal.geo"),
            ResourceLocation.parse("cobblemon:floette_eternal_mega.geo"));
    @Unique private static final Set<ResourceLocation> RUMBLE_LEGACY_FLOWERS = Set.of(
            ResourceLocation.parse("cobblemon:textures/pokemon/0670_floette/floette_eternal.png"),
            ResourceLocation.parse("cobblemon:textures/pokemon/0670_floette/floette_eternal_shiny.png"));

    @Shadow public abstract ResourceLocation getResolvedModel(PosableState state);

    @ModifyReturnValue(method = "getResolvedLayers", at = @At("RETURN"))
    private Iterable<ModelLayer> rumble$dropObsoleteOverlays(Iterable<ModelLayer> layers, PosableState state) {
        ModelLayer obsolete = null;
        for (var layer : layers) {
            // Cobblemon inherits layers by name even when a later resolver changes the entire model.
            var name = layer.getName();
            if (!"armored".equals(name) && !"".equals(name)) continue;
            var model = getResolvedModel(state);
            if (model == null) continue;
            var legacyTextures = "armored".equals(name) && RUMBLE_ARMORED_MODELS.contains(model) ? RUMBLE_LEGACY_ARMOR
                    : "".equals(name) && RUMBLE_ETERNAL_MODELS.contains(model) ? RUMBLE_LEGACY_FLOWERS : null;
            if (legacyTextures == null) continue;
            var texture = layer.getTexture().invoke(state);
            if (texture != null && legacyTextures.contains(texture)) {
                obsolete = layer;
                break;
            }
        }
        if (obsolete == null) return layers;

        var remaining = new ArrayList<ModelLayer>();
        for (var layer : layers) {
            if (layer != obsolete) remaining.add(layer);
        }
        return remaining;
    }
}
