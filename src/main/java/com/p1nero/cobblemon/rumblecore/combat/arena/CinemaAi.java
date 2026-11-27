package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.TypeChart;
import com.johnn.realtimecobblemon.moves.Anims;
import com.johnn.realtimecobblemon.moves.Fx;
import com.johnn.realtimecobblemon.moves.Fx3D;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import com.johnn.realtimecobblemon.moves.MoveRegistry;
import com.johnn.realtimecobblemon.moves.MoveStyle;
import com.johnn.realtimecobblemon.moves.Pops;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeAiDamage;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeSignatureMoves;
import com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat.CinemaMoveAccess;
import com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat.CinemaSoundAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.StreamSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** RTC's Brawl is locked to player targets; movie actors need the same cast flow for either kind of target. */
final class CinemaAi {
    private record Attack(MoveInfo info, MoveStyle style, double range, Move slot) {}
    private record Cast(Attack move, LivingEntity target, long releaseAt) {}
    private record Shot(Attack move, LivingEntity target, Vec3 position, Vec3 velocity, double remaining, int seed) {}

    private final PokemonEntity pokemon;
    private final List<Attack> moves;
    private final List<Shot> shots = new ArrayList<>();
    private Cast cast;
    private Cast meleeImpact;
    private long firstCastAt = -1;
    private long nextRepositionAt;
    private int strafeSide = 1;

    private final boolean limitedPp;
    private final Attack basic;

    CinemaAi(PokemonEntity pokemon) { this(pokemon, false); }

    CinemaAi(PokemonEntity pokemon, boolean limitedPp) {
        this.pokemon = pokemon;
        this.limitedPp = limitedPp;
        basic = limitedPp ? attack(MoveInfo.basic("tackle", "Tackle", "normal", "physical", 35, 100, 0, "normal"), null) : null;
        moves = pokemon.getPokemon().getMoveSet().getMoves().stream()
                .map(move -> {
                    var info = RealtimeSignatureMoves.resolve(pokemon.getPokemon(), move, MoveRegistry.info(move));
                    return info.isStatus() || info.power() <= 0 ? null : attack(info, move);
                }).filter(java.util.Objects::nonNull).toList();
    }

    private static Attack attack(MoveInfo info, Move slot) {
        var style = slot == null ? new MoveStyle(MoveStyle.Shape.MELEE, 2, 0, 0, 1, 0) : MoveRegistry.style(info);
        return new Attack(info, style, Math.max(2, Math.max(style.range(), style.radius())), slot);
    }

    void tick(ServerLevel level, LivingEntity target, BiPredicate<Entity, Entity> opponents, boolean playerHit) {
        for (var it = shots.listIterator(); it.hasNext();) {
            var next = advance(level, it.next(), opponents, playerHit);
            if (next == null) it.remove(); else it.set(next);
        }

        long now = level.getGameTime();
        if (RealtimeMoveWindup.crowdControlled(pokemon) || pokemon.isNoAi()) {
            cancel();
            return;
        }
        if (meleeImpact != null && now >= meleeImpact.releaseAt()) {
            var pending = meleeImpact;
            meleeImpact = null;
            var victim = pending.target();
            if (valid(victim, opponents) && inRange(pending.move(), victim, pokemon.distanceTo(victim)) && pokemon.hasLineOfSight(victim)) {
                var center = victim.getBoundingBox().getCenter();
                Fx3D.withPalette(pokemon, () -> impact(level, pending.move(), victim, center,
                        center.subtract(pokemon.getBoundingBox().getCenter()).normalize(), playerHit));
            }
        }
        if (cast != null) {
            if (!valid(cast.target(), opponents)) { cancel(); return; }
            face(cast.target());
            if (now >= cast.releaseAt()) {
                var released = cast;
                cast = null;
                var slot = released.move().slot();
                if (limitedPp && slot != null) slot.setCurrentPp(Math.max(0, slot.getCurrentPp() - 1));
                RealtimeMoveWindup.npcReleased(pokemon, now);
                Fx3D.withPalette(pokemon, () -> release(level, released, playerHit));
            }
            return;
        }
        if (!valid(target, opponents) || moves.isEmpty() && basic == null) {
            pokemon.getNavigation().stop();
            return;
        }
        // Stagger the opening so both sides do not all become invulnerable on the same tick.
        if (firstCastAt < 0) firstCastAt = now + pokemon.getRandom().nextInt(13);
        face(target);
        double distance = pokemon.distanceTo(target);
        var stocked = limitedPp ? moves.stream().filter(move -> move.slot().getCurrentPp() > 0).toList() : moves;
        if (stocked.isEmpty() && basic != null) stocked = List.of(basic);
        var available = stocked.stream().filter(move -> inRange(move, target, distance)).toList();
        if (available.isEmpty() || !pokemon.hasLineOfSight(target)) {
            if (now % 5 == 0) pokemon.getNavigation().moveTo(target, 1.1);
        } else if (now >= firstCastAt && !RealtimeMoveWindup.npcCoolingDown(pokemon, now)) {
            var move = available.get(pokemon.getRandom().nextInt(available.size()));
            int ticks = RealtimeMoveWindup.npcMove(pokemon, level, now, move.info());
            if (ticks > 0) {
                if (limitedPp) pokemon.getNavigation().stop();
                cast = new Cast(move, target, now + ticks);
                Anims.send(pokemon, 4);
                Fx3D.withPalette(pokemon, () -> Fx3D.chargeTint(pokemon, move.info(), ticks));
            }
        } else if (limitedPp && target instanceof PokemonEntity && meleeImpact == null && now >= nextRepositionAt) {
            reposition(target, stocked, now);
        }
    }

    private void reposition(LivingEntity target, List<Attack> stocked, long now) {
        var delta = pokemon.position().subtract(target.position());
        var outward = new Vec3(delta.x, 0, delta.z);
        if (outward.lengthSqr() < .01) {
            var look = pokemon.getLookAngle();
            outward = new Vec3(-look.x, 0, -look.z);
        }
        outward = outward.normalize();
        double hitboxes = (pokemon.getBbWidth() + target.getBbWidth()) / 2.0;
        double attackRange = stocked.stream().mapToDouble(Attack::range).max().orElse(2);
        double preferred = hitboxes + Math.clamp(attackRange * .65, 1.25, 5);
        var side = new Vec3(-outward.z, 0, outward.x).scale(1.8 * strafeSide);
        var forward = outward.scale(Math.clamp(preferred - delta.horizontalDistance(), -1.75, 1.75));
        var step = pokemon.position().add(forward).add(side);
        if (!pokemon.getNavigation().moveTo(step.x, step.y, step.z, 1.15)) {
            strafeSide = -strafeSide;
            step = step.subtract(side.scale(2));
            pokemon.getNavigation().moveTo(step.x, step.y, step.z, 1.15);
        } else if (pokemon.getRandom().nextInt(4) == 0) strafeSide = -strafeSide;
        nextRepositionAt = now + 8 + pokemon.getRandom().nextInt(7);
    }

    private void cancel() {
        if (cast != null) RealtimeMoveWindup.npcInterrupted(pokemon);
        cast = null;
        meleeImpact = null;
    }

    private void face(Entity target) { pokemon.getLookControl().setLookAt(target, 30, 30); }

    private boolean valid(LivingEntity target, BiPredicate<Entity, Entity> opponents) {
        return target != null && target.level() == pokemon.level() && target.isAlive() && !target.isSpectator()
                && (!(target instanceof PokemonEntity body) || !body.getPokemon().isFainted())
                && (!(target instanceof ServerPlayer player) || Smash.active(player)) && opponents.test(pokemon, target);
    }

    private boolean inRange(Attack move, LivingEntity target, double distance) {
        return distance <= move.range() + (pokemon.getBbWidth() + target.getBbWidth()) / 2.0;
    }

    private void release(ServerLevel level, Cast released, boolean playerHit) {
        var move = released.move();
        var target = released.target();
        Vec3 from = pokemon.getBoundingBox().getCenter(), center = target.getBoundingBox().getCenter();
        Vec3 direction = center.subtract(from).normalize();
        Fx.animateMove(pokemon, move.info(), 0);
        Anims.send(pokemon, 5);
        Fx3D.cast(level, move.info(), pokemon.position(), direction, Math.max(0.8F, pokemon.getBbHeight()));
        CinemaSoundAccess.rumble$move(level, move.info(), false, from);
        switch (move.style().shape()) {
            case MELEE -> meleeImpact = new Cast(move, target, level.getGameTime() + RealtimeMoveWindup.MELEE_IMPACT_TICKS);
            case PROJECTILE -> {
                double speed = Math.clamp(move.style().speed(), 0.6, 1.5);
                shots.add(new Shot(move, target, from, direction.scale(speed), move.range() + target.getBbWidth(), pokemon.getRandom().nextInt()));
            }
            case BEAM, STREAM -> {
                Vec3 end = blockEnd(level, from, from.add(direction.scale(move.range())));
                boolean hits = target.getBoundingBox().inflate(0.25).clip(from, end).isPresent();
                var span = (hits ? center : end).subtract(from);
                if (!Fx3D.stream(level, move.info(), from, span, move.style().size(), 0))
                    Fx3D.send(level, 3, from, span, move.info().type(), .22F * move.style().size(), 9);
                if (hits) impact(level, move, target, center, direction, playerHit);
            }
            default -> {
                if (inRange(move, target, pokemon.distanceTo(target)) && pokemon.hasLineOfSight(target)) {
                    if (move.style().shape() == MoveStyle.Shape.DASH) {
                        var flat = direction.multiply(1, 0, 1).normalize();
                        pokemon.setDeltaMovement(flat.scale(.8).add(0, .1, 0));
                        pokemon.hurtMarked = true;
                    }
                    impact(level, move, target, center, direction, playerHit);
                }
            }
        }
    }

    private Shot advance(ServerLevel level, Shot shot, BiPredicate<Entity, Entity> opponents, boolean playerHit) {
        var move = shot.move();
        if (!valid(shot.target(), opponents) || shot.remaining() <= 0) return null;
        var next = shot.position().add(shot.velocity().normalize().scale(Math.min(shot.remaining(), shot.velocity().length())));
        var end = blockEnd(level, shot.position(), next);
        var box = shot.target().getBoundingBox().inflate(.15 + move.style().size() * .2);
        var hit = box.contains(shot.position()) ? shot.position() : box.clip(shot.position(), end).orElse(null);
        if (hit != null) {
            Fx3D.withPalette(pokemon, () -> impact(level, move, shot.target(), hit, shot.velocity().normalize(), playerHit));
            return null;
        }
        Fx3D.withPalette(pokemon, () -> {
            if (!Fx3D.head(level, move.info(), end, shot.velocity(), move.style().size(), shot.seed()))
                Fx3D.send(level, 8, end, Vec3.ZERO, move.info().type(), .25F * move.style().size(), 2);
        });
        return end.equals(next) ? new Shot(move, shot.target(), end, shot.velocity(), shot.remaining() - shot.velocity().length(), shot.seed()) : null;
    }

    private Vec3 blockEnd(ServerLevel level, Vec3 from, Vec3 to) {
        var block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pokemon));
        return block.getType() == HitResult.Type.MISS ? to : block.getLocation();
    }

    private void impact(ServerLevel level, Attack move, LivingEntity target, Vec3 position, Vec3 direction, boolean playerHit) {
        var info = move.info();
        Fx3D.impact(level, info, position, direction, move.style().size());
        CinemaSoundAccess.rumble$move(level, info, true, position);
        var session = target instanceof ServerPlayer player ? Smash.session(player) : null;
        var body = session != null ? session.pokemon : target instanceof PokemonEntity entity ? entity : null;
        if (body == null) return;
        // Actors perform their moves from the start, but don't drain one another's HP before a player lands a hit.
        if (session == null && !playerHit) { Anims.send(body, 2); return; }
        if (RealtimeMoveWindup.locked(target) || RealtimeMoveWindup.locked(body)) return;
        var user = pokemon.getPokemon();
        var userTypes = StreamSupport.stream(user.getTypes().spliterator(), false).map(ElementalType::getName).toList();
        var types = StreamSupport.stream(body.getPokemon().getTypes().spliterator(), false).map(ElementalType::getName).toList();
        float effectiveness = move.slot() == null ? 1 : TypeChart.multiplier(info.type(), types);
        if (effectiveness == 0) { Pops.word(body, "no_effect", 0xBBBDBB, false); return; }
        boolean crit = RealtimeAiDamage.critical(info, level.random);
        // Keep supporting actors' pacing, but use the same combat stats and critical rules as Brawl.
        float damage = RealtimeAiDamage.calculate(pokemon, body, target, info.power() * 1.15, !info.isPhysical(),
                userTypes.contains(info.type()), effectiveness, .85F + level.random.nextFloat() * .15F, crit)
                * (limitedPp ? .85F : session != null ? .85F : .45F);
        var source = level.damageSources().mobAttack(pokemon);
        if (session != null) {
            if (CinemaMoveAccess.rumble$receive(session, source, damage, info) > 0) Anims.send(body, 2);
        } else {
            float amount = damage * body.getMaxHealth() / Math.max(1, body.getPokemon().getMaxHealth());
            body.invulnerableTime = 0;
            if (body.hurt(source, amount)) Fx.flinch(body, damage / body.getPokemon().getMaxHealth());
        }
    }
}
