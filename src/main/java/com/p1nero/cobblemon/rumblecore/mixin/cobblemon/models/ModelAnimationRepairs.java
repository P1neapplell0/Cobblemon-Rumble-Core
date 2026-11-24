package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.models;

import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimationGroup;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimationRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(value = BedrockAnimationRepository.class, remap = false)
abstract class ModelAnimationRepairs {
    @Shadow @Final private static Gson gson;
    @Shadow @Final private static Map<String, BedrockAnimationGroup> animationGroups;

    @Unique private static final String RUMBLE_MSD = "mod/mega_showdown";
    @Unique private static final String RUMBLE_CCC = "file/CCCwLegendSpawns_2.1.zip";

    @WrapOperation(method = "loadAnimations", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/packs/resources/Resource;open()Ljava/io/InputStream;"))
    private InputStream rumble$fixTruncatedExpressions(Resource resource, Operation<InputStream> original,
                                                       @Local ResourceLocation id) throws IOException {
        InputStream stream = original.call(resource);
        if (!"cobblemon".equals(id.getNamespace())) return stream;
        String path = id.getPath();
        boolean mew = path.equals("bedrock/pokemon/animations/mew/mew.animation.json");
        boolean talonflame = path.equals("bedrock/pokemon/animations/0663_talonflame/talonflame_unite.animation.json");
        if (!mew && !talonflame) return stream;
        try (stream) {
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            try {
                var animationFile = JsonParser.parseString(json).getAsJsonObject();
                var animations = animationFile.getAsJsonObject("animations");
                if (mew) {
                    for (String side : List.of("left", "right")) {
                        var bones = rumble$animationBones(animations, "animation.mew.shoulder_" + side);
                        // Keep the constant angle; the source provides no multiplier for this oscillation.
                        rumble$replaceRotation(bones, "foot_right", 0, "-2.5+math.cos((query.anim_time - 0.2) * 90) *", -2.5);
                        rumble$replaceRotation(bones, "foot_left", 0, "-5+math.cos((query.anim_time - 0.2) * 90) *", -5);
                    }
                } else {
                    var bones = rumble$animationBones(animations, "animation.talonflame_unite.ride_jump");
                    rumble$replaceRotation(bones, "wing_open_right5", 1, "5-", 5);
                }
                json = animationFile.toString();
            } catch (RuntimeException ex) {
                LogUtils.getLogger().warn("Could not apply animation repair to {} from {}", id, resource.sourcePackId(), ex);
            }
            return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Unique
    private static JsonObject rumble$animationBones(JsonObject animations, String name) {
        return animations != null && animations.get(name) instanceof JsonObject animation
                && animation.get("bones") instanceof JsonObject bones ? bones : null;
    }

    @Unique
    private static void rumble$replaceRotation(JsonObject bones, String name, int axis, String broken, double value) {
        if (bones != null && bones.get(name) instanceof JsonObject bone
                && bone.get("rotation") instanceof JsonArray rotation && rotation.size() > axis
                && rotation.get(axis) instanceof JsonPrimitive component && component.isString()
                && broken.equals(component.getAsString().strip())) {
            rotation.set(axis, new JsonPrimitive(value));
        }
    }

    @Inject(method = "loadAnimations", at = @At("TAIL"), order = 1100)
    private void rumble$repairModelAnimations(ResourceManager resources, List<String> directories, CallbackInfo ci) {
        // Run after RTC merges lower-priority clips. Only touch known pairs of models and animations.
        if (!directories.contains("bedrock/pokemon/animations")) return;
        rumble$repairGroup(resources, "0648_meloetta", "meloetta_pirouette", "meloetta_pirouette", Set.of("air_idle", "sleep"));
        rumble$repairGroup(resources, "0670_floette", "floette_eternal_mega", "floette_eternal_mega", Set.of("cry", "recoil"));
        rumble$repairGroup(resources, "0373_salamence", "salamencemega", "salamence_mega",
                Set.of("ground_idle", "ground_walk", "battle_idle", "air_idle", "air_fly"));
    }

    @Unique
    private static void rumble$repairGroup(ResourceManager resources, String folder, String model, String groupName,
                                           Set<String> clipNames) {
        var group = animationGroups.get(groupName);
        if (group == null) return;

        var geometry = resources.getResource(ResourceLocation.fromNamespaceAndPath("cobblemon",
                "bedrock/pokemon/models/" + folder + "/" + model + ".geo.json")).orElse(null);
        var animation = resources.getResource(ResourceLocation.fromNamespaceAndPath("cobblemon",
                "bedrock/pokemon/animations/" + folder + "/" + groupName + ".animation.json")).orElse(null);
        if (geometry == null || animation == null) return;
        String geometryPack = geometry.sourcePackId();
        String animationPack = animation.sourcePackId();

        try {
            if (RUMBLE_CCC.equals(animationPack) && RUMBLE_CCC.equals(geometryPack)
                    && "meloetta_pirouette".equals(groupName)) {
                // This older rig ships its own matching clips, but under the Aria Forme prefix.
                try (var reader = animation.openAsReader()) {
                    var legacy = gson.fromJson(reader, BedrockAnimationGroup.class);
                    var clips = new LinkedHashMap<>(group.getAnimations());
                    final String oldPrefix = "animation.meloetta.";
                    legacy.getAnimations().forEach((name, clip) -> {
                        if (!name.startsWith(oldPrefix)) return;
                        String alias = "animation.meloetta_pirouette." + name.substring(oldPrefix.length());
                        if (legacy.getAnimations().containsKey(alias)) return;
                        clip.setName(alias);
                        clips.put(alias, clip);
                    });
                    animationGroups.put(groupName, new BedrockAnimationGroup(group.getFormatVersion(), clips));
                }
            } else if (RUMBLE_MSD.equals(animationPack) && (RUMBLE_MSD.equals(geometryPack)
                    || ("salamence_mega".equals(groupName) && RUMBLE_CCC.equals(geometryPack)))) {
                try (var reader = geometry.openAsReader()) {
                    var bones = new HashSet<String>();
                    var definition = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("minecraft:geometry");
                    for (var bone : definition.get(0).getAsJsonObject().getAsJsonArray("bones")) {
                        bones.add(bone.getAsJsonObject().get("name").getAsString());
                    }
                    var clips = new LinkedHashMap<>(group.getAnimations());
                    boolean changed = false;
                    for (String clipName : clipNames) {
                        String key = "animation." + groupName + "." + clipName;
                        var clip = clips.get(key);
                        if (clip == null || bones.containsAll(clip.getBoneTimelines().keySet())) continue;

                        var timelines = new LinkedHashMap<>(clip.getBoneTimelines());
                        timelines.keySet().retainAll(bones);
                        if (timelines.isEmpty()) continue;
                        var repaired = new BedrockAnimation(clip.getShouldLoop(), clip.getAnimationLength(),
                                clip.getEffects(), timelines);
                        repaired.setName(clip.getName());
                        clips.put(key, repaired);
                        changed = true;
                    }
                    if (changed) animationGroups.put(groupName, new BedrockAnimationGroup(group.getFormatVersion(), clips));
                }
            }
        } catch (IOException | RuntimeException e) {
            LogUtils.getLogger().warn("Could not repair the {} animation group", groupName, e);
        }
    }
}
