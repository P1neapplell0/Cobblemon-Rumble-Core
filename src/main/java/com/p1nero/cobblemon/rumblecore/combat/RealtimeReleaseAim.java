package com.p1nero.cobblemon.rumblecore.combat;

import com.p1nero.cobblemon.rumblecore.combat.arena.GimmickCinematics;
import com.johnn.realtimecobblemon.MoveCombat;
import com.johnn.realtimecobblemon.Smash;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaKnockoutVisual;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record RealtimeReleaseAim(int targetId) implements CustomPacketPayload {
    private static final Type<RealtimeReleaseAim> TYPE = new Type<>(PrologueWorld.id("realtime_release_aim"));
    private static final StreamCodec<RegistryFriendlyByteBuf, RealtimeReleaseAim> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RealtimeReleaseAim::targetId, RealtimeReleaseAim::new);
    private record Lock(ServerLevel level, int targetId) {}
    private static final double MAX_LOCK_DISTANCE_SQUARED = 72 * 72;
    private static final Map<Smash.Session, Lock> LOCKS = new WeakHashMap<>();

    @Override public Type<RealtimeReleaseAim> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player
                    && !GimmickCinematics.locked(player)) {
                Smash.Session session = Smash.session(player);
                if (session == null) return;
                if (packet.targetId() < 0) LOCKS.remove(session);
                else if (player.level().getEntity(packet.targetId()) instanceof LivingEntity target
                        && player.distanceToSqr(target) <= MAX_LOCK_DISTANCE_SQUARED) {
                    LOCKS.put(session, new Lock(player.serverLevel(), packet.targetId()));
                } else LOCKS.remove(session);
            }
        });
    }

    public static Vec3 direction(Smash.Session session, Vec3 eye, Vec3 fallback) {
        var target = lockedTarget(session);
        if (target == null) return fallback;
        Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
        return to.lengthSqr() > 1e-6 && to.lengthSqr() <= MAX_LOCK_DISTANCE_SQUARED ? to.normalize() : fallback;
    }

    static LivingEntity lockedTarget(Smash.Session session) {
        var lock = LOCKS.get(session);
        if (lock == null || lock.level() != session.player.level()
                || !(lock.level().getEntity(lock.targetId()) instanceof LivingEntity target)
                || target instanceof ArenaKnockoutVisual visual && visual.rumble$isKnockedOut()
                || !MoveCombat.canHit(session, target) || !session.player.hasLineOfSight(target)) return null;
        return target;
    }
}
