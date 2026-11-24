package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.models;

import com.cobblemon.mod.common.client.render.VaryingRenderableResolver;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.VaryingModelRepository;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = VaryingRenderableResolver.class, remap = false)
abstract class GreninjaPoserRouting {
    @Unique private static final ResourceLocation RUMBLE_GRENINJA = ResourceLocation.parse("cobblemon:greninja");
    @Unique private static final ResourceLocation RUMBLE_BASE_RIG = ResourceLocation.parse("cobblemon:greninja.geo");
    @Unique private static final ResourceLocation RUMBLE_BASE_POSER = ResourceLocation.parse("cobblemon_rumble_core:greninja_base");
    @Unique private static final ResourceLocation RUMBLE_ASH_POSER = ResourceLocation.parse("cobblemon:ashgreninja");
    @Unique private static final Set<ResourceLocation> RUMBLE_NORMAL_SKIN_RIGS = Set.of(
            ResourceLocation.parse("cobblemon:hero_style_greninja.geo"), ResourceLocation.parse("cobblemon:greninja_winter.geo"));

    @Shadow public abstract ResourceLocation getName();
    @Shadow public abstract ResourceLocation getResolvedModel(PosableState state);
    @Shadow public abstract VaryingModelRepository getRepository();

    @ModifyReturnValue(method = "getResolvedPoser", at = @At("RETURN"))
    private ResourceLocation rumble$pairGreninjaRigAndPose(ResourceLocation poser, PosableState state) {
        if (!RUMBLE_GRENINJA.equals(getName())) return poser;
        var model = getResolvedModel(state);
        // Both old packs claim cobblemon:greninja even though their files live at different paths.
        if (RUMBLE_GRENINJA.equals(poser) && RUMBLE_BASE_RIG.equals(model)
                && getRepository().getPosers().containsKey(RUMBLE_BASE_POSER)) return RUMBLE_BASE_POSER;
        // Applying a normal-rig costume to Ash Greninja keeps the costume's rig, not Ash's prop bones.
        if (RUMBLE_ASH_POSER.equals(poser) && model != null && RUMBLE_NORMAL_SKIN_RIGS.contains(model)) return RUMBLE_GRENINJA;
        return poser;
    }
}
