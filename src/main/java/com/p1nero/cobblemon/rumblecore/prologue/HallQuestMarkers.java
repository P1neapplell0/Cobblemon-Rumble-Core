package com.p1nero.cobblemon.rumblecore.prologue;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.arcadia.arc_quest.questmarker.api.MarkSpec;
import org.arcadia.arc_quest.questmarker.api.MarkTargetResolverRegistry;
import org.arcadia.arc_quest.questmarker.api.MarkableObject;
import org.arcadia.arc_quest.questmarker.api.QuestMarkerData;
import org.arcadia.arc_quest.questmarker.api.ResolvedMarkTarget;

public final class HallQuestMarkers {
    private static final String RESOLVER = PrologueWorld.id("hall_actor").toString();
    private static final String DIMENSION = PrologueWorld.GYMS.location().toString();

    private HallQuestMarkers() {}

    public static void register() {
        MarkTargetResolverRegistry.register(RESOLVER, (player, level, target) -> {
            var args = target.args();
            var hall = player.getServer().getLevel(PrologueWorld.GYMS);
            var actor = hall == null || hall != level ? null : FixedStoryActors.find(hall, args.get("role"));
            if (actor != null && FixedStoryActors.mayTrack(actor, player)) {
                return new ResolvedMarkTarget(actor.getX(), actor.getY() + actor.getBbHeight(), actor.getZ(), DIMENSION,
                        actor.getId(), actor.getUUID().toString(), "", QuestMarkerData.EntityAttachPoint.HEAD);
            }
            return ResolvedMarkTarget.position(Double.parseDouble(args.get("x")), Double.parseDouble(args.get("y")),
                    Double.parseDouble(args.get("z")), DIMENSION);
        });
    }

    public static boolean isActorTarget(MarkableObject target) {
        return target instanceof MarkableObject.CustomResolver custom && RESOLVER.equals(custom.resolverId());
    }

    public static MarkSpec actor(String marker, String label, String role, BlockPos pos) {
        Vec3 anchor = Vec3.atBottomCenterOf(pos).add(0, 2, 0);
        return MarkSpec.translated(marker, label, new MarkableObject.CustomResolver(RESOLVER, Map.of(
                "role", role, "x", Double.toString(anchor.x), "y", Double.toString(anchor.y), "z", Double.toString(anchor.z))));
    }
}
