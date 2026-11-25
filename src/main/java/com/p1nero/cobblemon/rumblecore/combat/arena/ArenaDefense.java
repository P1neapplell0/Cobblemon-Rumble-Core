package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Anims;
import com.johnn.realtimecobblemon.moves.Conditions;
import com.johnn.realtimecobblemon.moves.Fx3D;
import com.johnn.realtimecobblemon.moves.Sounds;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** Reads a committed wind-up, never incoming damage, to decide whether to defend. */
public final class ArenaDefense {
    private static final Map<PokemonEntity, Reaction> REACTIONS = new WeakHashMap<>();
    private ArenaDefense() {}

    public static boolean react(PokemonEntity foe, Smash.Session attacker, ServerLevel level) {
        int tier = ArenaMatch.defenseTier(foe);
        if (tier == 0) return false;
        long now = level.getGameTime();
        var state = REACTIONS.computeIfAbsent(foe, ignored -> new Reaction());
        if (now < state.activeUntil) return true;
        long skillStrike = Math.max(RealtimeMoveWindup.playerWindupUntil(attacker.player), attacker.combat.chargingUntil);
        long strike = Math.max(skillStrike, attacker.combat.heavyFireAt);
        if (strike <= now) return false;
        if (strike != state.strike || !attacker.player.getUUID().equals(state.attacker)) {
            state.strike = strike;
            state.attacker = attacker.player.getUUID();
            state.noticeAt = now + (tier == 2 ? 3 : 5);
            state.attempted = false;
        }
        if (state.attempted || now < state.noticeAt || now < state.readyAt || strike - now > 8) return false;
        Vec3 towardsFoe = foe.position().subtract(attacker.player.position());
        double distance = towardsFoe.length();
        if (distance > 14 || distance < .01 || !attacker.player.hasLineOfSight(foe)
                || attacker.combat.targetId != foe.getId() && attacker.player.getLookAngle().dot(towardsFoe.scale(1 / distance)) < .76) return false;
        state.attempted = true;
        if (foe.getRandom().nextFloat() >= (tier == 2 ? .55f : .30f)) return false;
        state.readyAt = now + (tier == 2 ? 46 : 64);
        foe.getNavigation().stop();
        boolean dodge = foe.onGround() && foe.getRandom().nextFloat() < .85f;
        if (dodge) {
            Vec3 sideways = new Vec3(-towardsFoe.z, 0, towardsFoe.x).normalize().scale(.85);
            if (foe.getRandom().nextBoolean()) sideways = sideways.reverse();
            if (!clear(level, foe, sideways)) sideways = sideways.reverse();
            if (clear(level, foe, sideways)) {
                state.activeUntil = now + 7;
                state.guard = false;
                foe.setDeltaMovement(sideways.x, .10, sideways.z);
                foe.hurtMarked = true;
                Anims.send(foe, 0);
                Fx3D.dust(level, foe.position(), .5f);
                Sounds.play(level, foe.position(), "whoosh", .8f, 1.2f);
                return true;
            }
        }
        // A missed dodge opportunity should not almost always become a guard on a skill cast.
        if (strike == skillStrike && foe.getRandom().nextFloat() >= .10f) return false;
        state.activeUntil = Math.max(now + 6, strike + 3);
        state.guard = true;
        foe.setDeltaMovement(0, foe.getDeltaMovement().y, 0);
        Vec3 center = foe.getBoundingBox().getCenter();
        float radius = Math.max(foe.getBbWidth(), foe.getBbHeight()) * .75f + .35f;
        Fx3D.send(level, 40, center, Vec3.ZERO, 0, 0, radius, Math.max(6, (int) (state.activeUntil - now)), foe.getId());
        Fx3D.send(level, 42, center, Vec3.ZERO, 0, 0, radius, 6, foe.getId());
        Sounds.play(level, foe.position(), "shield_hit", .45f, .85f);
        return true;
    }

    private static boolean clear(ServerLevel level, PokemonEntity foe, Vec3 delta) {
        var box = foe.getBoundingBox().move(delta.scale(2));
        return level.getWorldBorder().isWithinBounds(box) && level.noCollision(foe, box);
    }

    public static boolean guards(PokemonEntity foe, Smash.Session attacker) {
        var state = REACTIONS.get(foe);
        if (state == null || !state.guard || foe.level().getGameTime() >= state.activeUntil
                || !attacker.player.getUUID().equals(state.attacker) || ArenaMatch.defenseTier(foe) == 0) return false;
        return !Conditions.stunned(foe);
    }

    private static final class Reaction {
        UUID attacker;
        long strike, noticeAt, activeUntil, readyAt;
        boolean attempted, guard;
    }
}
