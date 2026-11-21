package com.p1nero.cobblemon.rumblecore.client.menu;

import com.cobblemon.mod.common.api.scheduling.ClientTaskTracker;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockAnimation;
import com.cobblemon.mod.common.client.render.models.blockbench.bedrock.animation.BedrockBoneTimeline;
import com.cobblemon.mod.common.client.render.models.blockbench.repository.RenderContext;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** A level-free Cobblemon stage. Positions are the same camera-space units as the HTML. */
final class TitleFighters {
    private record Spot(float x, float depth, float altitude) {}
    private record Fighter(int dex, String name, Spot wide, Spot portrait, float size, float yaw, int shadow) {}
    private static final float FOV = (float) Math.toRadians(43);
    private static final float HALF_FOV = (float) Math.tan(FOV / 2);
    private static final float CYCLE = 13, CHARGE = 1.7f, ROAR = 7.15f;
    private static final float IDLE_SPEED = .75f;
    private static final Fighter[] LINEUP = {
            flyer(373, "salamence", .19f, 205, 38, .258f, 202, 28, .53f, .96f),
            ground(212, "scizor", .153f, 181, .159f, 174, .77f, .67f, 44),
            ground(908, "meowscarada", .326f, 178, .371f, 158, .72f, .78f, 36),
            ground(237, "hitmontop", .096f, 121, .119f, 108, .88f, .65f, 30),
            ground(448, "lucario", .228f, 133, .265f, 110, .57f, .89f, 36),
            ground(25, "pikachu", .365f, 103, .381f, 88, .52f, 1.05f, 31),
            flyer(6, "charizard", .803f, 203, 26, .733f, 202, 26, .80f, -.97f),
            ground(445, "garchomp", .864f, 187, .865f, 178, .92f, -.75f, 44),
            ground(257, "blaziken", .694f, 187, .608f, 178, .75f, -.64f, 42),
            ground(658, "greninja", .611f, 115, .592f, 94, .62f, -.61f, 47),
            ground(937, "ceruledge", .759f, 136, .715f, 120, .63f, -.83f, 37),
            ground(815, "cinderace", .862f, 133, .833f, 109, .50f, -.72f, 35),
    };
    private final TitlePoseState[] states = new TitlePoseState[LINEUP.length];
    private final Set<String> failures = new HashSet<>();
    private final List<SimpleSoundInstance> sounds = new ArrayList<>();
    private float previous;
    private final TitleRig[] rigs = new TitleRig[LINEUP.length];

    private static Fighter ground(int dex, String name, float wx, int wz, float px, int pz, float size, float yaw, int shadow) {
        return new Fighter(dex, name, new Spot(wx, wz, 0), new Spot(px, pz, 0), size, yaw, shadow);
    }

    private static Fighter flyer(int dex, String name, float wx, int wz, int wy, float px, int pz, int py, float size, float yaw) {
        return new Fighter(dex, name, new Spot(wx, wz, wy), new Spot(px, pz, py), size, yaw, 0);
    }

    void reset() { previous = 0; failures.clear(); }

    void stopSounds() {
        var manager = Minecraft.getInstance().getSoundManager();
        sounds.forEach(manager::stop);
        sounds.clear();
    }

    void render(GuiGraphics g, int width, int height, float elapsed) {
        float aspect = width / (float) height;
        boolean portrait = aspect < 5f / 6;
        float floor = portrait ? -16 : -24;
        float fit = portrait ? .43f * Math.min(1, aspect / .46f) : Math.min(1, aspect / 1.62f);
        float beat = elapsed % CYCLE;
        cue(elapsed, CHARGE + .53f, "pikachu", .45f);
        cue(elapsed, CHARGE + 1.10f, "lightning", .56f);
        cue(elapsed, ROAR + 1.0833f, "charizard", .45f);
        previous = elapsed;
        drawShadows(g, width, height, portrait, floor, fit);
        g.flush();
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        var camera = new Matrix4f().perspective(FOV, aspect, 60, 300);
        RenderSystem.backupProjectionMatrix();
        var view = RenderSystem.getModelViewStack();
        view.pushMatrix().identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(camera, VertexSorting.DISTANCE_TO_ORIGIN);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderLights(new Vector3f(-.48f, .68f, .55f), new Vector3f(.69f, .38f, -.60f));
        try {
            for (int i = 0; i < LINEUP.length; i++) {
                Fighter fighter = LINEUP[i];
                if (failures.contains(fighter.name)) continue;
                Spot spot = portrait ? fighter.portrait : fighter.wide;
                float phase = i * .47f;
                float idleTime = elapsed * IDLE_SPEED + phase;
                float flightTime = fighter.name.equals("charizard") ? elapsed * IDLE_SPEED : idleTime;
                float fly = spot.altitude == 0 ? 0 : 2.9f * Mth.sin(flightTime * Mth.HALF_PI);
                var pose = new PoseStack();
                pose.translate((2 * spot.x - 1) * HALF_FOV * spot.depth * aspect, floor + spot.altitude + fly, -spot.depth);
                pose.mulPose(Axis.YP.rotation(fighter.yaw));
                if (spot.altitude != 0) pose.mulPose(Axis.ZP.rotation(.035f * Mth.sin(flightTime * 1.17f)));
                float size = 16 * fighter.size * fit;
                // Cobblemon's ModelPart coordinates point down and back relative to the preview.
                pose.scale(size, -size, -size);
                try {
                    drawFighter(i, fighter, pose, buffers, idleTime, beat);
                } catch (IOException | RuntimeException e) {
                    failures.add(fighter.name);
                    LogUtils.getLogger().warn("Could not draw {} on the Rumble title", fighter.name, e);
                }
            }
        } finally {
            buffers.endBatch();
            RenderSystem.restoreProjectionMatrix();
            view.popMatrix();
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();
        }
        drawElectric(g, width, height, portrait, floor, fit, beat - CHARGE);
    }

    private void drawFighter(int index, Fighter fighter, PoseStack pose, MultiBufferSource.BufferSource buffers,
                             float time, float beat) throws IOException {
        if (rigs[index] == null) rigs[index] = TitleRig.load(fighter.dex, fighter.name, fighter.wide.altitude != 0);
        var rig = rigs[index];
        var id = ResourceLocation.fromNamespaceAndPath("cobblemon", fighter.name);
        var state = states[index];
        if (state == null) {
            state = states[index] = new TitlePoseState();
            state.setCurrentAspects(rig.aspects());
        }
        var model = rig.model();
        var texture = rig.skin();
        var context = new RenderContext();
        var keys = RenderContext.Companion;
        context.put(keys.getTEXTURE(), texture);
        context.put(keys.getSPECIES(), id);
        context.put(keys.getASPECTS(), state.getCurrentAspects());
        context.put(keys.getSCALE(), 1f);
        context.put(keys.getPOSABLE_STATE(), state);
        context.put(keys.getDO_QUIRKS(), false);
        state.setCurrentModel(model);
        model.setContext(context);
        model.setDefault();
        float age = fighter.name.equals("pikachu") ? beat - CHARGE : beat - ROAR;
        var cheeks = new ArrayList<ModelPart>();
        try {
            var idle = rig.idle();
            float length = (float) idle.getAnimationLength();
            state.evaluate(model, idle, length > 0 ? time % length : time);
            var move = rig.move();
            float moveWeight = 0;
            if (move != null && age >= 0 && age < move.getAnimationLength()) {
                float remaining = (float) move.getAnimationLength() - age;
                moveWeight = smooth(age / .18f) * smooth(remaining / .42f);
                blendMove(model, state, move, age, moveWeight);
            } else {
                var blink = rig.blink();
                float blinkTime = time % 5.3f - 4.9f;
                if (blink != null && blinkTime >= 0 && blinkTime < blink.getAnimationLength()) {
                    state.evaluate(model, blink, blinkTime);
                }
            }
            if (fighter.name.equals("pikachu") && moveWeight == 0) {
                model.getRelevantPartsByName().forEach((name, part) -> {
                    if (name.startsWith("cheek_active_") && part.visible) {
                        part.visible = false;
                        cheeks.add(part);
                    }
                });
            }
            // A flat Bedrock cube already supplies front and back faces with their own UVs.
            // Cull the backface so the two coplanar faces do not compete in the depth buffer.
            drawModel(model, context, pose, buffers, RenderType.entityCutout(texture));
            var glow = rig.glowAt(time);
            if (glow != null) drawModel(model, context, pose, buffers, TitleRenderLayers.glow(glow));
        } finally {
            state.sampleAt(time);
            cheeks.forEach(part -> part.visible = true);
            model.setDefault();
        }
    }

    private static final class TitlePoseState extends PosableState {
        @Override public Entity getEntity() { return null; }
        @Override public ClientTaskTracker getSchedulingTracker() { return ClientTaskTracker.INSTANCE; }

        @Override public void updatePartialTicks(float partialTicks) {
            // The title supplies a fraction of the current tick, not a frame delta.
            setCurrentPartialTicks(partialTicks);
        }

        void evaluate(PosableModel model, BedrockAnimation clip, float seconds) {
            // MoLang query.anim_time reads the state, while keyframes take seconds directly.
            sampleAt(seconds);
            clip.run(model, this, seconds, 0, 0, seconds * 20, 1);
        }

        private void sampleAt(float seconds) {
            float ticks = seconds * 20;
            int wholeTicks = (int) ticks;
            updateAge(wholeTicks);
            updatePartialTicks(ticks - wholeTicks);
        }
    }

    private static void drawModel(PosableModel model, RenderContext context, PoseStack pose,
                                  MultiBufferSource.BufferSource buffers, RenderType layer) {
        model.render(context, pose, buffers.getBuffer(layer), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
        buffers.endBatch(layer);
    }

    private static float smooth(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static void blendMove(PosableModel model, TitlePoseState state, BedrockAnimation move, float age, float weight) {
        // Like the PK preview, blend from the idle pose to an independently evaluated move.
        // Adding the two rotations together over-rotates the neck during Charizard's cry.
        var parts = model.getRelevantPartsByName();
        var idlePose = new ArrayList<IdlePart>(parts.size());
        parts.forEach((name, part) -> idlePose.add(new IdlePart(part, move.getBoneTimelines().get(name))));
        model.setDefault();
        state.evaluate(model, move, age);
        idlePose.forEach(part -> part.blend(weight));
    }

    private record IdlePart(ModelPart part, BedrockBoneTimeline timeline, float x, float y, float z,
                            float pitch, float yaw, float roll, float sx, float sy, float sz) {
        IdlePart(ModelPart part, BedrockBoneTimeline timeline) {
            this(part, timeline, part.x, part.y, part.z, part.xRot, part.yRot, part.zRot, part.xScale, part.yScale, part.zScale);
        }

        void blend(float weight) {
            float position = timeline == null || timeline.getPosition().isEmpty() ? 0 : weight;
            float rotation = timeline == null || timeline.getRotation().isEmpty() ? 0 : weight;
            float scale = timeline == null || timeline.getScale().isEmpty() ? 0 : weight;
            part.setPos(Mth.lerp(position, x, part.x), Mth.lerp(position, y, part.y), Mth.lerp(position, z, part.z));
            part.setRotation(blendAngle(rotation, pitch, part.xRot), blendAngle(rotation, yaw, part.yRot),
                    blendAngle(rotation, roll, part.zRot));
            part.xScale = Mth.lerp(scale, sx, part.xScale);
            part.yScale = Mth.lerp(scale, sy, part.yScale);
            part.zScale = Mth.lerp(scale, sz, part.zScale);
        }

        private static float blendAngle(float weight, float from, float to) {
            return from + weight * (float) Math.IEEEremainder(to - from, Math.TAU);
        }
    }

    private static void drawShadows(GuiGraphics g, int width, int height, boolean portrait, float floor, float fit) {
        for (Fighter fighter : LINEUP) {
            if (fighter.shadow == 0) continue;
            Spot spot = portrait ? fighter.portrait : fighter.wide;
            float scale = height / (2 * HALF_FOV * spot.depth);
            int w = Math.round(fighter.shadow * fighter.size * fit * scale);
            int h = Math.max(2, Math.round(w * -floor / spot.depth * .72f));
            int x = Math.round(width * spot.x), y = Math.round(height / 2f - floor * scale);
            RumbleTitleScreen.image(g, "shadow", x - w / 2, y - h / 2, w, h, 64, 64);
        }
    }

    private static void drawElectric(GuiGraphics g, int width, int height, boolean portrait, float floor, float fit, float age) {
        float strength = Mth.clamp((age - .42f) / .42f, 0, 1) * Mth.clamp((2.12f - age) / .42f, 0, 1);
        if (strength <= 0) return;
        Fighter pikachu = LINEUP[5];
        Spot spot = portrait ? pikachu.portrait : pikachu.wide;
        float scale = height / (2 * HALF_FOV * spot.depth);
        int size = Math.round((30 + 8 * strength) * fit * scale);
        int x = Math.round(width * spot.x), y = Math.round(height / 2f - (floor + 8 * fit) * scale);
        int frame = (int) (age * 17) % 8;
        RenderSystem.setShaderColor(1, 1, 1, strength);
        try {
            RumbleTitleScreen.image(g, "electric-" + frame, x - size / 2, y - size / 2, size, size, 128, 128);
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    private void cue(float elapsed, float time, String name, float volume) {
        if (previous == 0 || elapsed - previous > .5f || (int) ((elapsed + CYCLE - time) / CYCLE) == (int) ((previous + CYCLE - time) / CYCLE)) return;
        var manager = Minecraft.getInstance().getSoundManager();
        sounds.removeIf(sound -> !manager.isActive(sound));
        var event = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cobblemon_rumble_core", "title." + name));
        var sound = SimpleSoundInstance.forUI(event, 1, volume);
        sounds.add(sound);
        manager.play(sound);
    }
}
