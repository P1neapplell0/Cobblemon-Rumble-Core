package com.p1nero.cobblemon.rumblecore.combat.reward;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.duel.Duels;
import com.johnn.realtimecobblemon.moves.Decoys;
import com.johnn.realtimecobblemon.moves.Experience;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining;
import com.p1nero.cobblemon.rumblecore.prologue.SquadQuest;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** A knockout leaves the original wild entity available to Cobblemon's thrown-ball pipeline. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class WildCapture {
    public static final int WINDOW_SECONDS = 30;
    public static final int INACTIVE = -1;
    private static final int WINDOW_TICKS = WINDOW_SECONDS * 20;
    private static final String UNTIL = CobblemonRumbleCore.MOD_ID + ":capture_until";
    private static final String WINNER = CobblemonRumbleCore.MOD_ID + ":capture_winner";
    private static final String ATTEMPT = CobblemonRumbleCore.MOD_ID + ":capture_attempt";

    private WildCapture() {}

    public interface Synced {
        int rumble$captureSeconds();
        void rumble$setCaptureSeconds(int seconds);
    }

    public static int seconds(PokemonEntity target) {
        return ((Synced) target).rumble$captureSeconds();
    }

    public static boolean stunned(PokemonEntity target) {
        return target.level().isClientSide() ? seconds(target) != INACTIVE : target.getPersistentData().contains(UNTIL);
    }

    public static boolean canThrowAt(PokemonEntity target) {
        return target.isAlive() && !target.getPersistentData().getBoolean(ATTEMPT)
                && target.getPersistentData().getLong(UNTIL) > target.level().getGameTime();
    }

    public static void initialize() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(event -> {
            var player = event.getPlayer();
            if (player.getUUID().equals(event.getPokemon().getOwnerUUID())) {
                SquadQuest.captured(player);
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDefeat(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof PokemonEntity target) || !(target.level() instanceof ServerLevel level)
                || stunned(target) || !target.getPokemon().isWild() || target.getBattleId() != null || target.isBusy()
                || Decoys.isDecoy(target) || PrologueTraining.target(target) || ArenaMatch.isArena(level)
                || !UncatchableProperty.INSTANCE.isCatchable(target)
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || !(event.getSource().getEntity() instanceof PokemonEntity attacker)
                || !(attacker.getOwner() instanceof ServerPlayer player)) return;
        Smash.Session session = Smash.session(player);
        if (session == null || session.pokemon != attacker || player.level() != level || Duels.groupOf(player) != null
                || ArenaMatch.group(player) != null) return;

        event.setCanceled(true);
        target.getPersistentData().putUUID(WINNER, player.getUUID());
        resetWindow(target);
        target.setHealth(Math.max(.01f, target.getMaxHealth() * .01f));
        target.getPokemon().setCurrentHealth(Math.max(1, Math.round(target.getPokemon().getMaxHealth() * .01f)));
        target.setNoAi(true);
        target.setTarget(null);
        target.getNavigation().stop();
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        // RTC's after-hit test only sees a living target. Preserve its normal XP and wallet hooks once.
        Experience.knockedOut(session, target);
        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * .85,
                target.getZ(), 12, .28, .15, .28, .10);
        level.playSound(null, target, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, .9f, .65f);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protectStunned(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof PokemonEntity target && stunned(target)
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) event.setCanceled(true);
    }

    public static void attemptStarted(PokemonEntity target) {
        target.getPersistentData().putBoolean(ATTEMPT, true);
        ((Synced) target).rumble$setCaptureSeconds(0);
    }

    public static void attemptFailed(PokemonEntity target) {
        if (!target.isAlive() || !target.getPersistentData().getBoolean(ATTEMPT)) return;
        target.getPersistentData().remove(ATTEMPT);
        resetWindow(target);
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof PokemonEntity target) || !(target.level() instanceof ServerLevel level)
                || !stunned(target) || !target.isAlive()) return;
        var data = target.getPersistentData();
        if (data.getBoolean(ATTEMPT)) {
            boolean catching = false;
            for (var lock : target.getBusyLocks()) {
                if (lock instanceof EmptyPokeBallEntity ball && ball.isAlive()) catching = true;
            }
            if (catching) return;
            // Recover if the unsaved ball vanished (for example, when the chunk unloaded mid-capture).
            target.getBusyLocks().removeIf(EmptyPokeBallEntity.class::isInstance);
            target.setInvisible(false);
            target.setSilent(false);
            target.setPhasingTargetId(-1);
            target.setBeamMode(0);
            attemptFailed(target);
        }
        long remaining = data.getLong(UNTIL) - level.getGameTime();
        if (remaining > 0) {
            ((Synced) target).rumble$setCaptureSeconds((int) ((remaining + 19) / 20));
            if (target.tickCount % 5 == 0 && !target.isInvisible()) {
                double angle = target.tickCount * .27;
                double radius = Math.max(.32, target.getBbWidth() * .44);
                double x = Math.cos(angle) * radius, z = Math.sin(angle) * radius;
                double y = target.getY() + target.getBbHeight() + .13;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX() + x, y, target.getZ() + z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX() - x, y, target.getZ() - z, 1, 0, 0, 0, 0);
            }
            return;
        }
        Entity winner = data.hasUUID(WINNER) ? level.getPlayerByUUID(data.getUUID(WINNER)) : null;
        target.setHealth(0);
        target.getPokemon().setCurrentHealth(0);
        // Keep the flag through die() so this deferred defeat cannot open a new capture window.
        target.die(winner instanceof ServerPlayer player ? level.damageSources().playerAttack(player) : level.damageSources().generic());
        data.remove(UNTIL);
        data.remove(WINNER);
        ((Synced) target).rumble$setCaptureSeconds(INACTIVE);
    }

    private static void resetWindow(PokemonEntity target) {
        target.getPersistentData().putLong(UNTIL, target.level().getGameTime() + WINDOW_TICKS);
        ((Synced) target).rumble$setCaptureSeconds(WINDOW_SECONDS);
    }
}
