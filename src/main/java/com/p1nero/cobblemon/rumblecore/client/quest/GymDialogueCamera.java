package com.p1nero.cobblemon.rumblecore.client.quest;

import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import com.p1nero.cobblemon.rumblecore.prologue.ProfessorRestock;
import com.p1nero.cobblemon.rumblecore.prologue.MegaIntroduction;
import com.p1nero.cobblemon.rumblecore.prologue.TeraChapter;
import com.p1nero.cobblemon.rumblecore.prologue.ProfessorEntity;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.arcadia.arc_quest.dialogue.network.ClientDialogueCache;

/** Moves the view once per dialogue beat, then gives control back to the player. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class GymDialogueCamera {
    private static final float TURN_TICKS = 9.0F;
    private static final double PARTNER_SEARCH_RADIUS = 6.0;
    private static Beat beat;
    private static float elapsed, fromYaw, yawDelta, fromPitch, toPitch;

    private record Beat(ClientLevel level, UUID session, String node, int sourceId) {}

    private GymDialogueCamera() {}

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        var mc = Minecraft.getInstance();
        var session = ClientDialogueCache.INSTANCE.getCurrentSession();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || mc.getCameraEntity() != player || session == null) {
            beat = null;
            return;
        }
        boolean restock = session.treeId.startsWith(ProfessorRestock.TREE + "/");
        boolean mega = session.treeId.equals(MegaIntroduction.TREE);
        boolean tera = session.treeId.startsWith(TeraChapter.TREE + "/");
        if (!restock && !mega && !tera && !("start".equals(session.nodeId) || "partner".equals(session.nodeId))) {
            beat = null;
            return;
        }

        var current = new Beat(level, session.sessionId, session.nodeId, session.entityId);
        if (!current.equals(beat)) {
            beat = null;
            Entity source = level.getEntity(session.entityId);
            if (source == null) return;
            Vec3 target;
            if (restock) {
                target = restockTarget(level, source, session.nodeId);
            } else if (tera) {
                target = teraTarget(level, source, session.nodeId);
            } else if (mega) {
                target = session.nodeId.startsWith("professor_")
                        ? level.getEntitiesOfClass(ProfessorEntity.class, source.getBoundingBox().inflate(PARTNER_SEARCH_RADIUS))
                                .stream().min(Comparator.comparingDouble(source::distanceToSqr)).map(Entity::getEyePosition).orElse(null)
                        : source.getEyePosition();
            } else {
                if (!(source instanceof NPCEntity npc) || !GymStory.talkable(npc)) return;
                var partner = level.getEntitiesOfClass(NPCEntity.class, source.getBoundingBox().inflate(PARTNER_SEARCH_RADIUS),
                                candidate -> candidate.isAlive() && GymStory.isDialogueActor(npc, candidate, true))
                        .stream().min(Comparator.comparingDouble(npc::distanceToSqr)).orElse(null);
                target = partner == null ? null : ("partner".equals(session.nodeId) ? partner : npc).getEyePosition();
            }
            // Entity tracking can lag behind the dialogue packet; try again on the next frame.
            if (target == null) return;
            var delta = target.subtract(player.getEyePosition());
            if (delta.lengthSqr() < 1.0E-6) return;
            var targetYaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F;
            fromYaw = player.getYRot();
            yawDelta = Mth.wrapDegrees(targetYaw - fromYaw);
            fromPitch = player.getXRot();
            toPitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z)));
            elapsed = 0.0F;
            beat = current;
        }

        if (elapsed >= TURN_TICKS) return;
        elapsed = Math.min(TURN_TICKS, elapsed + Mth.clamp(event.getPartialTick().getRealtimeDeltaTicks(), 0.0F, 2.0F));
        float t = elapsed / TURN_TICKS;
        float eased = t * t * (3.0F - 2.0F * t);
        player.setYRot(fromYaw + yawDelta * eased);
        player.setXRot(Mth.lerp(eased, fromPitch, toPitch));
        player.yRotO = player.getYRot();
        player.xRotO = player.getXRot();
    }

    private static Vec3 restockTarget(ClientLevel level, Entity doctor, String node) {
        if (node.startsWith("jessie_")) return actorEye(level, doctor, ProfessorRestock.JESSIE);
        if (node.startsWith("james_")) return actorEye(level, doctor, ProfessorRestock.JAMES);
        if ("meowth".equals(node)) return actorEye(level, doctor, ProfessorRestock.MEOWTH);
        if (node.startsWith("duo_") || "coins".equals(node)) {
            Vec3 a = actorEye(level, doctor, ProfessorRestock.JESSIE), b = actorEye(level, doctor, ProfessorRestock.JAMES);
            return a == null || b == null ? null : a.lerp(b, 0.5).add(0, -0.25, 0);
        }
        return doctor.getEyePosition();
    }

    private static Vec3 teraTarget(ClientLevel level, Entity leader, String node) {
        if (node.startsWith("cassidy_")) return actorEye(level, leader, TeraChapter.CASSIDY_NAME);
        if (node.startsWith("butch_")) return actorEye(level, leader, TeraChapter.BUTCH_NAME);
        if ("duo_retreat".equals(node)) {
            var a = actorEye(level, leader, TeraChapter.CASSIDY_NAME);
            var b = actorEye(level, leader, TeraChapter.BUTCH_NAME);
            return a == null || b == null ? null : a.lerp(b, .5);
        }
        return leader.getEyePosition();
    }

    private static Vec3 actorEye(ClientLevel level, Entity doctor, String name) {
        return level.getEntities(doctor, doctor.getBoundingBox().inflate(PARTNER_SEARCH_RADIUS), e -> hasActorName(e, name))
                .stream().min(Comparator.comparingDouble(doctor::distanceToSqr)).map(Entity::getEyePosition).orElse(null);
    }

    public static boolean hasActorName(Entity entity, String key) {
        var label = entity.getCustomName();
        return label != null && label.getContents() instanceof TranslatableContents contents && key.equals(contents.getKey());
    }

    /** The pose is evaluated by each viewer; two simultaneous conversations cannot fight over yaw. */
    public static boolean facesViewer(Entity entity) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level != entity.level() || entity.distanceToSqr(mc.player) > 9 * 9
                || !entity.level().dimension().equals(PrologueWorld.GYMS)) return false;
        var session = ClientDialogueCache.INSTANCE.getCurrentSession();
        if (session == null) return false;
        var source = mc.level.getEntity(session.entityId);
        if (source == null) return false;
        if (source instanceof ProfessorEntity && session.treeId.startsWith(ProfessorRestock.TREE + "/")) {
            return source == entity || hasActorName(entity, ProfessorRestock.JESSIE)
                    || hasActorName(entity, ProfessorRestock.JAMES) || hasActorName(entity, ProfessorRestock.MEOWTH);
        }
        if (session.treeId.startsWith(TeraChapter.TREE + "/")) {
            return source == entity || hasActorName(entity, TeraChapter.CASSIDY_NAME) || hasActorName(entity, TeraChapter.BUTCH_NAME);
        }
        if (source == entity) return entity instanceof ProfessorEntity || GymStory.talkable(entity);
        if (MegaIntroduction.TREE.equals(session.treeId) && entity instanceof ProfessorEntity) return MegaIntroduction.isMay(source);
        return entity instanceof NPCEntity npc && GymStory.isDialogueActor(source, npc, true);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        beat = null;
    }
}
