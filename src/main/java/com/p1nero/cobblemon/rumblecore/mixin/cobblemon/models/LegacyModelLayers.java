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
import java.util.Map;
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
    @Unique private static final ResourceLocation RUMBLE_MEGA_PYROAR = ResourceLocation.parse("cobblemon:pyroar_mega.geo");
    @Unique private static final Set<ResourceLocation> RUMBLE_PYROAR_ALPHA = Set.of(
            ResourceLocation.parse("cobblemon:textures/pokemon/0668_pyroar/pyroar_male_alpha.png"),
            ResourceLocation.parse("cobblemon:textures/pokemon/0668_pyroar/pyroar_female_alpha.png"));

    @Unique private static final Set<String> RUMBLE_GRENINJA_FORMS = Set.of(
            "ashgreninja.geo", "greninja_mega.geo", "greninja_winter.geo", "hero_style_greninja.geo");
    @Unique private static final Map<String, Set<String>> RUMBLE_GRENINJA_OVERLAYS = Map.of(
            "alpha_eyes", Set.of("greninja_alpha.png"),
            "emissive", Set.of("greninja_emissive.png", "greninja_emissive_shiny.png"),
            "transparency", Set.of("greninja_transparency.png", "greninja_transparency_shiny.png"));
    @Unique private static final Set<String> RUMBLE_HOOH_OVERLAYS = Set.of("ho_oh_emissive.png", "ho_oh_emissive_shiny.png");

    @Shadow public abstract ResourceLocation getResolvedModel(PosableState state);

    @ModifyReturnValue(method = "getResolvedLayers", at = @At("RETURN"))
    private Iterable<ModelLayer> rumble$dropObsoleteOverlays(Iterable<ModelLayer> layers, PosableState state) {
        var model = getResolvedModel(state);
        if (model == null || !"cobblemon".equals(model.getNamespace())) return layers;

        var remaining = new ArrayList<ModelLayer>();
        for (var layer : layers) {
            if (!rumble$isObsoleteOverlay(model, layer, state)) remaining.add(layer);
        }
        return remaining;
    }

    @Unique
    private static boolean rumble$isObsoleteOverlay(ResourceLocation model, ModelLayer layer, PosableState state) {
        // Later forms inherit earlier layers by name. Match the legacy texture as well as the model
        // so that a replacement layer already fitted to the new model can still be rendered.
        String name = layer.getName();
        var legacyTextures = "armored".equals(name) && RUMBLE_ARMORED_MODELS.contains(model) ? RUMBLE_LEGACY_ARMOR
                : "".equals(name) && RUMBLE_ETERNAL_MODELS.contains(model) ? RUMBLE_LEGACY_FLOWERS
                : "alpha_eyes".equals(name) && RUMBLE_MEGA_PYROAR.equals(model) ? RUMBLE_PYROAR_ALPHA : null;
        if (legacyTextures != null) {
            var texture = layer.getTexture().invoke(state);
            return texture != null && legacyTextures.contains(texture);
        }

        String modelPath = model.getPath();
        if (RUMBLE_GRENINJA_FORMS.contains(modelPath)) {
            return rumble$matchesOverlay(layer, state, "0658_greninja", RUMBLE_GRENINJA_OVERLAYS.get(name));
        }
        if ("hooh_art.geo".equals(modelPath) && "emissive".equals(name)) {
            return rumble$matchesOverlay(layer, state, "0250_hooh", RUMBLE_HOOH_OVERLAYS);
        }
        if (("hooh_art.geo".equals(modelPath) || "ho_oh_steampunk.geo".equals(modelPath)) && "alpha_eyes".equals(name)) {
            return rumble$matchesOverlay(layer, state, "0250_hooh", Set.of("ho_oh_alpha.png"));
        }
        return false;
    }

    @Unique
    private static boolean rumble$matchesOverlay(ModelLayer layer, PosableState state, String folder, Set<String> filenames) {
        if (filenames == null) return false;
        var texture = layer.getTexture().invoke(state);
        String prefix = "textures/pokemon/" + folder + "/";
        return texture != null && "cobblemon".equals(texture.getNamespace()) && texture.getPath().startsWith(prefix)
                && filenames.contains(texture.getPath().substring(prefix.length()));
    }
}
