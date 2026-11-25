package com.p1nero.cobblemon.rumblecore.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.MoveCombat;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.SmashConfig;
import com.johnn.realtimecobblemon.duel.Duels;
import com.johnn.realtimecobblemon.moves.Conditions;
import com.johnn.realtimecobblemon.moves.MoveEngine;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import com.johnn.realtimecobblemon.moves.MoveRegistry;
import com.johnn.realtimecobblemon.moves.MoveStyle;
import com.johnn.realtimecobblemon.moves.Pops;
import com.johnn.realtimecobblemon.network.SmashPayloads;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat.ConditionTimers;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class RealtimeMoveWindup {
    private static final int STANDARD_TICKS = 10;
    private static final int MELEE_TICKS = 4;
    private static final double WARNING_RADIUS = 96.0;
    private static final int MAX_KNOCKBACK_AIR_TICKS = 40;
    private static final Map<Entity, Knockback> KNOCKBACKS = new WeakHashMap<>();
    private static final Map<Entity, Lock> LOCKS = new WeakHashMap<>();
    private static final Map<PokemonEntity, Long> NPC_COOLDOWNS = new WeakHashMap<>();
    private static final Map<PokemonEntity, MoveInfo> NPC_PENDING_MOVES = new WeakHashMap<>();
    private static final ThreadLocal<ServerPlayer> RELEASED_PLAYER_MOVE = new ThreadLocal<>();

    private RealtimeMoveWindup() {}

    private record Lock(ResourceKey<Level> dimension, long until) {}

    private record Knockback(ResourceKey<Level> dimension, long recoveryUntil, long airborneUntil) {
        boolean active(Entity entity) {
            if (!(entity.level() instanceof ServerLevel level) || !dimension.equals(level.dimension())
                    || entity.isRemoved()) return false;
            long now = level.getGameTime();
            return now < recoveryUntil || (now < airborneUntil && !entity.onGround());
        }
    }

    public static boolean playerCastBlocked(ServerPlayer player) {
        Smash.Session session = Smash.session(player);
        return locked(player) || crowdControlled(player) || (session != null
                && (locked(session.pokemon) || crowdControlled(session.pokemon)));
    }

    public static boolean crowdControlled(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        long now = level.getGameTime();
        if (Conditions.stunned(entity) || Conditions.juggled(entity)) return true;
        var state = (ConditionTimers) (Object) Conditions.get(entity);
        if (state != null && now < state.rumble$launchedUntil()) return true;

        Knockback hit = KNOCKBACKS.get(entity);
        if (hit == null) return false;
        if (hit.active(entity)) return true;
        KNOCKBACKS.remove(entity);
        return false;
    }

    /** Only hit-induced displacement is tracked; a normal jump or lunge does not lock casting. */
    public static void recordKnockback(Entity entity, int recoveryTicks, boolean airborne) {
        if (!(entity.level() instanceof ServerLevel level) || locked(entity)) return;
        if (!(entity instanceof PokemonEntity) && !(entity instanceof ServerPlayer p && Smash.active(p))) return;
        long now = level.getGameTime();
        long recoveryUntil = now + recoveryTicks;
        long airborneUntil = airborne ? now + MAX_KNOCKBACK_AIR_TICKS : now;
        Knockback previous = KNOCKBACKS.get(entity);
        if (previous != null && previous.active(entity)) {
            recoveryUntil = Math.max(recoveryUntil, previous.recoveryUntil());
            airborneUntil = Math.max(airborneUntil, previous.airborneUntil());
        }
        KNOCKBACKS.put(entity, new Knockback(level.dimension(), recoveryUntil, airborneUntil));
    }

    public static boolean locked(Entity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        Lock lock = LOCKS.get(entity);
        if (lock == null) return false;
        // Keep protection through the release tick until the cast actually begins.
        if (!lock.dimension().equals(level.dimension()) || lock.until() < level.getGameTime()) {
            LOCKS.remove(entity);
            return false;
        }
        return true;
    }

    public static long playerWindupUntil(ServerPlayer player) {
        return locked(player) ? LOCKS.get(player).until() : 0;
    }

    public static boolean releasingPlayerMove(ServerPlayer player) {
        return RELEASED_PLAYER_MOVE.get() == player;
    }

    /** Called only after RTC has validated the requested move. */
    public static boolean playerMove(ServerPlayer player, MoveInfo move, Runnable release) {
        Smash.Session session = Smash.session(player);
        if (session == null || !player.isAlive() || !session.pokemon.isAlive()
                || playerCastBlocked(player)) return false;
        ServerLevel level = player.serverLevel();
        boolean melee = isMelee(move);
        int ticks = melee ? MELEE_TICKS : STANDARD_TICKS;
        Lock lock = new Lock(level.dimension(), level.getGameTime() + ticks);
        LOCKS.put(player, lock);
        LOCKS.put(session.pokemon, lock);
        player.stopUsingItem();
        warn(level, session.pokemon, player, ticks, melee);
        MoveEngine.later(level, ticks, session, () -> {
            LOCKS.remove(player, lock);
            LOCKS.remove(session.pokemon, lock);
            // Release client combat input lock in the same tick as the cast.
            if (player.level() == level) Smash.send(player, new RealtimeMoveWindupPacket(RealtimeMoveWindupPacket.RELEASE, false));
            if (player.level() != level || session.pokemon.level() != level
                    || !player.isAlive() || !session.pokemon.isAlive() || session.pokemon.isRemoved()) return;
            RELEASED_PLAYER_MOVE.set(player);
            try {
                release.run();
            } finally {
                RELEASED_PLAYER_MOVE.remove();
            }
        });
        return true;
    }

    public static boolean npcHasPp(PokemonEntity pokemon, MoveInfo info) {
        var learned = npcLearnedMove(pokemon, info);
        return learned == null || learned.getCurrentPp() > 0;
    }

    private static com.cobblemon.mod.common.api.moves.Move npcLearnedMove(PokemonEntity pokemon, MoveInfo info) {
        return pokemon.getPokemon().getMoveSet().getMoves().stream()
                .filter(move -> RealtimeSignatureMoves.usesSlot(pokemon.getPokemon(), move, info)).findFirst().orElse(null);
    }

    /** Returns the duration to schedule the cast for, or zero if the cast was rejected. */
    public static int npcMove(PokemonEntity pokemon, ServerLevel level, long now, MoveInfo move) {
        if (npcCoolingDown(pokemon, now) || locked(pokemon) || crowdControlled(pokemon) || !npcHasPp(pokemon, move)) return 0;
        boolean melee = isMelee(move);
        int ticks = melee ? MELEE_TICKS : STANDARD_TICKS;
        LOCKS.put(pokemon, new Lock(level.dimension(), now + ticks));
        NPC_PENDING_MOVES.put(pokemon, move);
        stopPokemon(pokemon);
        warn(level, pokemon, null, ticks, melee);
        return ticks;
    }

    private static boolean isMelee(MoveInfo move) {
        // DASH uses the standard warning, even when the move makes contact.
        return move != null && MoveRegistry.style(move).shape() == MoveStyle.Shape.MELEE;
    }

    public static void npcInterrupted(PokemonEntity pokemon) {
        if (NPC_PENDING_MOVES.remove(pokemon) != null) LOCKS.remove(pokemon);
    }

    public static void npcReleased(PokemonEntity pokemon, long now) {
        MoveInfo move = NPC_PENDING_MOVES.remove(pokemon);
        if (move == null) return;
        LOCKS.remove(pokemon);
        var learned = npcLearnedMove(pokemon, move);
        if (learned != null) learned.setCurrentPp(Math.max(0, learned.getCurrentPp() - 1));
        int cooldown = MoveCombat.cooldownTicks(move.power(), move.pp(), move.isStatus(), SmashConfig.get(SmashConfig.COOLDOWN_SCALE));
        if (move.priority() > 0) cooldown = (int) (cooldown * 0.75);
        if (move.isStatus() && move.has("heal")) {
            cooldown = (int) Math.round(cooldown * SmashConfig.get(SmashConfig.HEAL_COOLDOWN_SCALE));
        }
        NPC_COOLDOWNS.put(pokemon, now + cooldown);
    }

    public static boolean npcCoolingDown(PokemonEntity pokemon, long now) {
        long until = NPC_COOLDOWNS.getOrDefault(pokemon, 0L);
        if (until <= now) {
            NPC_COOLDOWNS.remove(pokemon);
            return false;
        }
        return true;
    }

    private static void stopPokemon(PokemonEntity pokemon) {
        pokemon.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pokemon.getNavigation().stop();
        stopMotion(pokemon);
    }

    private static void stopMotion(Entity entity) {
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
    }

    private static void warn(ServerLevel level, PokemonEntity pokemon, ServerPlayer caster, int ticks, boolean melee) {
        Vec3 at = pokemon.position().add(0.0, pokemon.getBbHeight() + 0.2, 0.0);
        var pop = new SmashPayloads.Pop(pokemon.getId(), at.x, at.y, at.z, "!", 0xFFD43B, Pops.WORD_BIG, ticks);
        var arena = caster == null ? null : ArenaMatch.pvpWarningGroup(caster);
        var duel = caster == null || arena != null ? null : Duels.groupOf(caster);
        for (ServerPlayer viewer : level.players()) {
            boolean nearby = viewer == caster || viewer.distanceToSqr(pokemon) <= WARNING_RADIUS * WARNING_RADIUS;
            if (nearby) Smash.send(viewer, pop);
            if (nearby || (arena != null && ArenaMatch.pvpWarningGroup(viewer) == arena)
                    || (duel != null && Duels.groupOf(viewer) == duel)) {
                Smash.send(viewer, new RealtimeMoveWindupPacket(viewer == caster ? ticks : 0, melee));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventDamage(LivingIncomingDamageEvent event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void clearRecoveredKnockback(ServerTickEvent.Post event) {
        KNOCKBACKS.entrySet().removeIf(entry -> !entry.getValue().active(entry.getKey()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void trackVanillaKnockback(LivingKnockBackEvent event) {
        if (!event.isCanceled() && event.getStrength() > 0.0F) {
            recordKnockback(event.getEntity(), 5, event.getEntity().onGround());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventKnockback(LivingKnockBackEvent event) {
        if (locked(event.getEntity())) event.setCanceled(true);
    }
}
