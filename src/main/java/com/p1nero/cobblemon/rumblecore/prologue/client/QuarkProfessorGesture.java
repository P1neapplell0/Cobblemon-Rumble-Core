package com.p1nero.cobblemon.rumblecore.prologue.client;

import com.cobblemon.mod.common.client.render.models.blockbench.PosableModel;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.p1nero.cobblemon.rumblecore.prologue.ProfessorEntity;
import com.p1nero.cobblemon.rumblecore.prologue.MegaIntroduction;
import com.p1nero.cobblemon.rumblecore.client.quest.GymDialogueCamera;
import com.p1nero.cobblemon.rumblecore.prologue.TeraChapter;
import com.p1nero.cobblemon.rumblecore.prologue.MegaTrainingQuest;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import java.util.UUID;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import org.arcadia.arc_quest.client.hud.dialogue.DialogueScreen;
import org.arcadia.arc_quest.dialogue.network.ClientDialogueCache;
import org.violetmoon.quark.content.tweaks.client.emote.EmoteBase;
import org.violetmoon.quark.content.tweaks.client.emote.EmoteHandler;
import org.violetmoon.quark.content.tweaks.client.emote.ModelAccessor;

/** Optional client bridge, ported from CIV's Quark timeline integration. */
public final class QuarkProfessorGesture implements ProfessorRenderer.Gesture {
    private static QuarkProfessorGesture trainerGesture;
    private final ModelPart root, armorRoot, legsRoot;
    private final HumanoidModel<ProfessorEntity> rig, armor, legs;
    private UUID sessionId;
    private long revision;
    private EmoteBase active;
    private boolean drawPose;

    QuarkProfessorGesture(EntityRendererProvider.Context context) {
        this(context::bakeLayer);
    }

    private QuarkProfessorGesture(java.util.function.Function<ModelLayerLocation, ModelPart> bake) {
        root = bake.apply(ModelLayers.PLAYER);
        armorRoot = bake.apply(ModelLayers.PLAYER_OUTER_ARMOR);
        legsRoot = bake.apply(ModelLayers.PLAYER_INNER_ARMOR);
        rig = new PlayerModel<>(root, false);
        armor = new HumanoidModel<>(armorRoot);
        legs = new HumanoidModel<>(legsRoot);
    }

    @Override public void prepare(ProfessorEntity doctor) {
        prepare(doctor, QuarkProfessorGesture::gestureFor);
    }

    private void prepare(Entity entity, BiFunction<String, String, String> gestures) {
        drawPose = false;
        var mc = Minecraft.getInstance();
        var session = ClientDialogueCache.INSTANCE.getCurrentSession();
        if (!(mc.screen instanceof DialogueScreen) || session == null || mc.level != entity.level()) {
            sessionId = null;
            active = null;
            return;
        }
        if (session.entityId != entity.getId() && !(entity instanceof ProfessorEntity
                && MegaIntroduction.TREE.equals(session.treeId) && session.nodeId.startsWith("professor_")
                && MegaIntroduction.isMay(entity.level().getEntity(session.entityId))
                && entity.distanceToSqr(entity.level().getEntity(session.entityId)) < 36)) return;
        if (!session.sessionId.equals(sessionId)) {
            sessionId = session.sessionId;
            revision = -1;
            active = null;
        }
        if (revision != session.revision) {
            revision = session.revision;
            String gesture = gestures.apply(session.treeId, session.nodeId);
            if (gesture != null) start(gesture);
        }
        // Quark's isDone also checks the local player actor for hurt/attack. Only use the timeline here.
        if (active == null || active.timeDone >= active.totalTime) return;
        try {
            active.update();
            drawPose = true;
        } catch (RuntimeException exception) {
            active = null;
            LogUtils.getLogger().warn("Could not update the professor's Quark gesture", exception);
        }
    }

    public static void poseTrainer(NPCEntity npc, PosableModel model) {
        var session = ClientDialogueCache.INSTANCE.getCurrentSession();
        if (session == null) return;
        // In duo dialogue the second line belongs to the partner, not the entity that opened the screen.
        boolean partner = "partner".equals(session.nodeId);
        Entity source = npc.level().getEntity(session.entityId);
        if (source == null) return;
        if (session.treeId.startsWith(TeraChapter.TREE + "/")) {
            if (!GymDialogueCamera.facesViewer(npc)) return;
            boolean speaking = session.nodeId.startsWith("cassidy_") ? GymDialogueCamera.hasActorName(npc, TeraChapter.CASSIDY_NAME)
                    : session.nodeId.startsWith("butch_") ? GymDialogueCamera.hasActorName(npc, TeraChapter.BUTCH_NAME) : source == npc;
            if (!speaking) return;
        } else if (!GymStory.isDialogueActor(source, npc, partner)) return;
        if (trainerGesture == null) trainerGesture = new QuarkProfessorGesture(Minecraft.getInstance().getEntityModels()::bakeLayer);
        var pose = trainerGesture;
        pose.prepare(source, (tree, node) -> {
            if (tree.startsWith(TeraChapter.TREE + "/")) return switch (node) {
                case "explain", "introduce", "cassidy_3", "butch_3" -> "point";
                case "cassidy_1", "cassidy_5", "butch_4" -> "wave";
                case "recognize", "congrats" -> "yes";
                default -> null;
            };
            if (MegaIntroduction.TREE.equals(tree)) return switch (node) {
                case "may_welcome" -> "wave";
                case "may_gift" -> "yes";
                case "may_explain" -> "point";
                default -> null;
            };
            if (tree.startsWith(MegaTrainingQuest.TREE + "/")) return switch (node) {
                case "ask" -> "think";
                case "explain", "trial_hint", "craft_hint" -> "point";
                case "congrats" -> "cheer";
                case "goodbye" -> "wave";
                default -> null;
            };
            return tree.endsWith("/win") || tree.endsWith("/win_reward") ? "yes" : tree.endsWith("/lose") ? "cheer" : partner ? "point" : "wave";
        });
        if (!pose.drawPose) return;
        var bones = model.getRelevantPartsByName();
        addBone(bones, "head", pose.rig.head);
        addBone(bones, "body", pose.rig.body);
        addBone(bones, "arm_left", pose.rig.leftArm);
        addBone(bones, "arm_right", pose.rig.rightArm);
        addBone(bones, "leg_left", pose.rig.leftLeg);
        addBone(bones, "leg_right", pose.rig.rightLeg);
    }

    private static void addBone(Map<String, ModelPart> bones, String id, ModelPart animated) {
        var bone = bones.get(id);
        if (bone != null) addRotation(bone, animated);
    }

    private static String gestureFor(String tree, String node) {
        if (node == null) return null;
        if (MegaIntroduction.TREE.equals(tree)) return switch (node) {
            case "professor_intro" -> "yes";
            case "professor_simulator" -> "point";
            default -> null;
        };
        if (PrologueWorld.id("prologue/welcome").toString().equals(tree)) {
            return switch (node) {
                case "intro_3" -> "wave";
                case "name" -> "think";
                case "trainer" -> "yes";
                default -> null;
            };
        }
        if (PrologueWorld.id("prologue/briefing").toString().equals(tree)) return "point";
        if (PrologueWorld.id("prologue/done").toString().equals(tree)) return "cheer";
        return null;
    }

    private void start(String gesture) {
        active = null;
        var descriptor = EmoteHandler.emoteMap.get(gesture);
        var player = Minecraft.getInstance().player;
        if (descriptor == null || player == null) return;
        reset(root, rig);
        reset(armorRoot, armor);
        reset(legsRoot, legs);
        try {
            var emote = descriptor.instantiate(player, rig, armor, legs);
            emote.startAllTimelines();
            active = emote;
        } catch (RuntimeException exception) {
            LogUtils.getLogger().warn("Could not start the professor's Quark gesture '{}'", gesture, exception);
        }
    }

    private static void reset(ModelPart root, HumanoidModel<?> model) {
        root.getAllParts().forEach(ModelPart::resetPose);
        ModelAccessor.INSTANCE.resetModel(model);
    }

    @Override public void transform(PoseStack pose) {
        if (drawPose) active.rotateAndOffset(pose);
    }

    @Override public void apply(PlayerModel<ProfessorEntity> model) {
        if (!drawPose) return;
        // Overlay Quark's joint rotations on the vanilla gaze instead of replacing the head angles.
        addRotation(model.head, rig.head);
        addRotation(model.body, rig.body);
        addRotation(model.leftArm, rig.leftArm);
        addRotation(model.rightArm, rig.rightArm);
        addRotation(model.leftLeg, rig.leftLeg);
        addRotation(model.rightLeg, rig.rightLeg);
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
    }

    private static void addRotation(ModelPart part, ModelPart animated) {
        part.xRot += animated.xRot;
        part.yRot += animated.yRot;
        part.zRot += animated.zRot;
    }
}
