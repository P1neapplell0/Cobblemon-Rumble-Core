package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.rayquaza;

import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimationGroup;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimationRepository;
import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = BedrockAnimationRepository.class, remap = false)
abstract class CafeRayquazaAnimations {
    @Shadow @Final private static Gson gson;
    @Shadow @Final private static Map<String, BedrockAnimationGroup> animationGroups;

    @Unique private static final String RUMBLE_GROUP = "rumble_cafe_rayquaza";
    @Unique private static final ResourceLocation RUMBLE_CAFE_MODEL = ResourceLocation.parse(
            "cobblemon:bedrock/pokemon/models/cosmetic/cobblecafe/lunar_rayquaza.geo.json");
    @Unique private static final ResourceLocation RUMBLE_STOCK_ANIMATIONS = ResourceLocation.parse(
            "cobblemon:bedrock/pokemon/animations/0384_rayquaza/rayquaza.animation.json");

    @Inject(method = "loadAnimations", at = @At("TAIL"))
    private void rumble$loadCafeRigAnimations(ResourceManager resources, List<String> directories, CallbackInfo ci) {
        if (resources.getResource(RUMBLE_CAFE_MODEL).isEmpty()) return;

        // Café's rig uses Cobblemon's stock bones. MSD's same-named group instead animates its own rig.
        for (var resource : resources.getResourceStack(RUMBLE_STOCK_ANIMATIONS)) {
            if (!resource.sourcePackId().equals("mod/cobblemon")) continue;
            try (var reader = resource.openAsReader()) {
                var group = gson.fromJson(reader, BedrockAnimationGroup.class);
                Map<String, BedrockAnimation> clips = new HashMap<>();
                group.getAnimations().forEach((name, clip) -> {
                    String aliasedName = name.replace("animation.rayquaza.", "animation." + RUMBLE_GROUP + ".");
                    clip.setName(aliasedName);
                    clips.put(aliasedName, clip);
                });
                animationGroups.put(RUMBLE_GROUP, new BedrockAnimationGroup(group.getFormatVersion(), clips));
            } catch (IOException | RuntimeException e) {
                LogUtils.getLogger().warn("Could not load the stock Rayquaza animations for Cobblemon Cafe", e);
            }
            return;
        }
    }
}
