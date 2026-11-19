package com.p1nero.cobblemon.rumblecore.prologue;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Defense;
import com.johnn.realtimecobblemon.moves.Fx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A server-traced practice projectile; its trajectory is fixed at launch and can be dodged. */
final class TrainingBolt {
    enum Result { FLYING, MISSED, HIT, GUARDED, PARRIED }

    private static final float DAMAGE = 5.0F;
    private static final double SPEED = 0.28;
    private final PokemonEntity caster;
    private final Vec3 velocity;
    private long lastTick;
    private Vec3 position;
    private int age;

    TrainingBolt(ServerLevel level, PokemonEntity caster, Smash.Session battle) {
        this.caster = caster;
        this.lastTick = level.getGameTime();
        position = origin(caster);
        velocity = battle.player.getBoundingBox().getCenter().subtract(position).normalize().scale(SPEED);
        Fx.animate(caster, "special", "attack", "physical");
        level.playSound(null, caster, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 0.45F, 1.7F);
        burst(level, position, 12);
    }

    static Vec3 origin(PokemonEntity caster) { return caster.getBoundingBox().getCenter().add(0, .1, 0); }

    static void burst(ServerLevel level, Vec3 point, int count) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, count, .11, .11, .11, .06);
    }

    Result tick(ServerLevel level, Smash.Session battle) {
        if (level.getGameTime() == lastTick) return Result.FLYING;
        lastTick = level.getGameTime();
        if (++age > 40 || !caster.isAlive()) return Result.MISSED;
        var next = position.add(velocity);
        var wall = level.clip(new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        var end = wall.getType() == HitResult.Type.MISS ? next : wall.getLocation();
        var collision = battle.player.getBoundingBox().inflate(.12).clip(position, end);
        if (collision.isPresent() || battle.player.getBoundingBox().contains(position)) {
            var source = level.damageSources().mobProjectile(caster, caster);
            // Use RTC's real guard/parry resolution. Merely holding the key, missing, or dodging never passes.
            boolean guarding = battle.combat.blocking && level.getGameTime() >= battle.combat.dodgeUntil;
            float remaining = Defense.defend(battle, source, DAMAGE);
            boolean guarded = guarding && remaining < DAMAGE && battle.combat.blocking;
            float safeDamage = Math.min(remaining, battle.pokemon.getPokemon().getCurrentHealth() - 1);
            if (safeDamage > 0) Smash.hurtPokemon(battle, safeDamage, source);
            burst(level, collision.orElse(position), 12);
            if (!guarded) {
                level.playSound(null, battle.player, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 0.3F, 1.6F);
                return Result.HIT;
            }
            // For this positive-damage shot, a held guard that takes zero damage is RTC's parry.
            return remaining == 0F ? Result.PARRIED : Result.GUARDED;
        }
        burst(level, end, 4);
        level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 1, .02, .02, .02, 0);
        position = next;
        return wall.getType() == HitResult.Type.MISS ? Result.FLYING : Result.MISSED;
    }
}
