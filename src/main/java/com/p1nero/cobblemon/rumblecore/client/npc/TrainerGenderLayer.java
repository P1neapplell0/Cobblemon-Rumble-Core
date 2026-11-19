package com.p1nero.cobblemon.rumblecore.client.npc;

import com.cobblemon.mod.common.client.render.models.blockbench.PosableEntityModel;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wildfire.main.Gender;
import com.wildfire.main.entitydata.EntityConfig;
import com.wildfire.render.GenderLayer;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Adapts Cobblemon's animated Bedrock torso to Female Gender's humanoid layer. */
public final class TrainerGenderLayer extends RenderLayer<NPCEntity, PosableEntityModel<NPCEntity>> {
    private static final String PREFIX = "rumble-gym-";
    private static final Set<String> FEMALE = Set.of("grass-leader", "ghost-leader", "ice-leader", "restock-jessie", "tera-cassidy", "may",
            "grass-1", "grass-2", "ghost-1a", "ghost-1b", "ghost-2a", "ghost-2b", "ice-2a", "fire-3a");
    private final Map<NPCEntity, Preset> configs = new WeakHashMap<>();
    private final HumanoidModel<NPCEntity> rig;
    private final GenderLayer<NPCEntity, HumanoidModel<NPCEntity>> layer;

    public TrainerGenderLayer(RenderLayerParent<NPCEntity, PosableEntityModel<NPCEntity>> parent, EntityRendererProvider.Context context) {
        super(parent);
        rig = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER));
        layer = new GenderLayer<>(new RenderLayerParent<>() {
            @Override public HumanoidModel<NPCEntity> getModel() { return rig; }
            @Override public ResourceLocation getTextureLocation(NPCEntity entity) { return parent.getTextureLocation(entity); }
        }, context.getModelManager());
    }

    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, NPCEntity npc,
                                 float limb, float stride, float tick, float age, float yaw, float pitch) {
        var actor = npc.getAspects().stream().filter(aspect -> aspect.startsWith(PREFIX) && FEMALE.contains(aspect.substring(PREFIX.length())))
                .findFirst().orElse(null);
        if (actor == null) return;
        var model = getParentModel().getPosableModel();
        if (model == null) return;
        var torso = model.getRelevantPartsByName().get("body");
        if (torso == null) return;
        rig.body.xRot = torso.xRot;
        rig.body.yRot = torso.yRot;
        rig.body.zRot = torso.zRot;
        rig.young = false;
        var config = configs.computeIfAbsent(npc, entity -> new Preset(entity.getUUID(), switch (actor.substring(PREFIX.length())) {
            case "restock-jessie", "tera-cassidy" -> 2;
            case "may" -> 0;
            default -> Math.floorMod(actor.hashCode(), 3);
        }));
        var old = EntityConfig.ENTITY_CACHE.put(npc.getUUID(), config);
        try {
            config.tick(npc);
            layer.render(pose, buffers, light, npc, limb, stride, tick, age, yaw, pitch);
        } finally {
            if (old == null) EntityConfig.ENTITY_CACHE.remove(npc.getUUID(), config);
            else EntityConfig.ENTITY_CACHE.put(npc.getUUID(), old);
        }
    }

    private static final class Preset extends EntityConfig {
        private int lastTick = Integer.MIN_VALUE;

        Preset(UUID id, int preset) {
            super(id);
            gender = Gender.FEMALE;
            pBustSize = switch (preset) { case 0 -> .4f; case 1 -> .6f; default -> .8f; };
            breasts.updateCleavage(preset == 2 ? .05f : .03f);
        }

        void tick(NPCEntity npc) {
            if (lastTick != npc.tickCount) {
                lastTick = npc.tickCount;
                tickBreastPhysics(npc);
            }
        }
    }
}
