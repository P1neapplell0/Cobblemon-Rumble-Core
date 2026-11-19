package com.p1nero.cobblemon.rumblecore.client.quest;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.jetbrains.annotations.Nullable;

/** Per-viewer render pose for shared dialogue actors. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class DialogueActorPose {
    private static final float HEAD_LIMIT = 65.0F;
    private static final Map<LivingEntity, Sample> SAMPLES = new WeakHashMap<>();

    public record Pose(float body, float head, float pitch) {}
    private record Sample(Pose pose, long time) {}

    private DialogueActorPose() {}

    public static @Nullable Pose sample(LivingEntity actor, float partialTick) {
        boolean talking = GymDialogueCamera.facesViewer(actor);
        var previous = SAMPLES.get(actor);
        if (!talking && previous == null) return null;

        var idle = new Pose(Mth.rotLerp(partialTick, actor.yBodyRotO, actor.yBodyRot),
                Mth.rotLerp(partialTick, actor.yHeadRotO, actor.yHeadRot),
                Mth.lerp(partialTick, actor.xRotO, actor.getXRot()));
        long now = Util.getMillis();
        // A culled actor starts from its current pose when it comes back into view.
        if (previous == null || now - previous.time() > 250) {
            if (!talking) {
                SAMPLES.remove(actor);
                return null;
            }
            previous = new Sample(idle, now);
        }

        var target = idle;
        if (talking) {
            var delta = Minecraft.getInstance().player.getEyePosition(partialTick)
                    .subtract(actor.getEyePosition(partialTick));
            if (delta.lengthSqr() > 1.0E-6) {
                float turn = (float) Math.toDegrees(Mth.atan2(delta.z, delta.x)) - 90.0F;
                float tilt = Mth.clamp((float) -Math.toDegrees(Mth.atan2(delta.y, delta.horizontalDistance())), -45.0F, 45.0F);
                target = new Pose(turn, turn, tilt);
            }
        }

        float seconds = (now - previous.time()) / 1000.0F;
        var from = previous.pose();
        float body = Mth.rotLerp(blend(seconds, 7.0F), from.body(), target.body());
        float head = Mth.rotLerp(blend(seconds, 16.0F), from.head(), target.head());
        float pitch = Mth.lerp(blend(seconds, 14.0F), from.pitch(), target.pitch());
        if (!talking && Mth.degreesDifferenceAbs(body, idle.body()) < 0.5F
                && Mth.degreesDifferenceAbs(head, idle.head()) < 0.5F && Math.abs(pitch - idle.pitch()) < 0.5F) {
            SAMPLES.remove(actor);
            return null;
        }
        head = body + Mth.clamp(Mth.wrapDegrees(head - body), -HEAD_LIMIT, HEAD_LIMIT);
        var pose = new Pose(body, head, pitch);
        SAMPLES.put(actor, new Sample(pose, now));
        return pose;
    }

    private static float blend(float seconds, float response) {
        return (float) -Math.expm1(-seconds * response);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        SAMPLES.clear();
    }
}
