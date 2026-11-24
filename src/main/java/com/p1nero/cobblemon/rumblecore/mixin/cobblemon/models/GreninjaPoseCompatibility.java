package com.p1nero.cobblemon.rumblecore.mixin.cobblemon.models;

import com.cobblemon.mod.common.client.render.models.blockbench.JsonPose;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.PoseAdapter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PoseAdapter.class, remap = false)
abstract class GreninjaPoseCompatibility {
    @WrapOperation(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lcom/cobblemon/mod/common/client/render/models/blockbench/pose/Pose;",
            at = @At(value = "NEW", target = "(Lcom/cobblemon/mod/common/client/render/models/blockbench/PosableModel;Lcom/google/gson/JsonObject;)Lcom/cobblemon/mod/common/client/render/models/blockbench/JsonPose;"))
    private JsonPose rumble$ignoreAbsentShurikens(PosableModel model, JsonObject json, Operation<JsonPose> original) {
        // Some packs supply the battle pose without supplying the matching Greninja rig.
        if (json.get("transformedParts") instanceof JsonArray parts) {
            JsonObject compatible = null;
            for (int i = parts.size() - 1; i >= 0; --i) {
                JsonElement entry = parts.get(i);
                if (!(entry instanceof JsonObject part) || !(part.get("part") instanceof JsonPrimitive name)
                        || !name.isString()) continue;
                String bone = name.getAsString();
                if (!(bone.equals("hand_shuriken_left") || bone.equals("hand_shuriken_right"))
                        || model.getRelevantPartsByName().containsKey(bone)) continue;
                if (compatible == null) compatible = json.deepCopy();
                compatible.getAsJsonArray("transformedParts").remove(i);
            }
            if (compatible != null) json = compatible;
        }
        return original.call(model, json);
    }
}
