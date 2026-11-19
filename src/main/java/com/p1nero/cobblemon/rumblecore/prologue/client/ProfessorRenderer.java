package com.p1nero.cobblemon.rumblecore.prologue.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.p1nero.cobblemon.rumblecore.prologue.ProfessorEntity;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

final class ProfessorRenderer extends MobRenderer<ProfessorEntity, PlayerModel<ProfessorEntity>> {
    private static final ResourceLocation SKIN = PrologueWorld.id("textures/entity/npc/prof_p1nero.png");
    private final Gesture gesture;

    ProfessorRenderer(EntityRendererProvider.Context context) {
        this(context, ModList.get().isLoaded("quark") ? new QuarkProfessorGesture(context) : null);
    }

    private ProfessorRenderer(EntityRendererProvider.Context context, Gesture gesture) {
        super(context, new Model(context.bakeLayer(ModelLayers.PLAYER), gesture), .4f);
        this.gesture = gesture;
    }

    @Override public ResourceLocation getTextureLocation(ProfessorEntity entity) { return SKIN; }

    @Override public void render(ProfessorEntity doctor, float yaw, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light) {
        if (gesture != null) gesture.prepare(doctor);
        super.render(doctor, yaw, partialTick, pose, buffers, light);
    }

    @Override protected void setupRotations(ProfessorEntity doctor, PoseStack pose, float age, float yaw,
            float partialTick, float scale) {
        super.setupRotations(doctor, pose, age, yaw, partialTick, scale);
        if (gesture != null) gesture.transform(pose);
    }

    interface Gesture {
        void prepare(ProfessorEntity doctor);
        void transform(PoseStack pose);
        void apply(PlayerModel<ProfessorEntity> model);
    }

    private static final class Model extends PlayerModel<ProfessorEntity> {
        private final Gesture gesture;
        private final ModelPart root;

        Model(ModelPart root, Gesture gesture) {
            super(root, false);
            this.root = root;
            this.gesture = gesture;
        }

        @Override public void setupAnim(ProfessorEntity doctor, float swing, float amount, float age,
                float headYaw, float headPitch) {
            if (gesture != null) root.getAllParts().forEach(ModelPart::resetPose);
            super.setupAnim(doctor, swing, amount, age, headYaw, headPitch);
            if (gesture != null) gesture.apply(this);
        }
    }
}
