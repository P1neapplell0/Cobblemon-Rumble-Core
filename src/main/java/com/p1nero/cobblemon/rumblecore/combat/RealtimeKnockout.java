package com.p1nero.cobblemon.rumblecore.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Conditions;
import com.johnn.realtimecobblemon.moves.Fx3D;
import com.johnn.realtimecobblemon.moves.Motion;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaKnockoutVisual;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.arena.FrontierKnockout;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining;
import java.util.HashSet;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.phys.Vec3;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class RealtimeKnockout {
    public static final int FLIGHT_TICKS = FrontierKnockout.CUE_TICKS;
    private record Flight(long until, int attackerId) {}
    private static final Map<PokemonEntity, Flight> flights = Collections.synchronizedMap(new WeakHashMap<>());

    private static final ThreadLocal<PokemonEntity> applying = new ThreadLocal<>();

    private RealtimeKnockout() {}

    public static boolean applyingTo(Entity body) { return applying.get() == body; }

    public static boolean wasLaunched(PokemonEntity body) { return flights.containsKey(body); }

    public static boolean inFlight(PokemonEntity body) {
        var flight = body == null ? null : flights.get(body);
        return flight != null && body.level().getGameTime() < flight.until();
    }

    public static boolean holdFlightStep(PokemonEntity body) {
        if (!inFlight(body)) return false;
        long age = body.level().getGameTime() - (flights.get(body).until() - FLIGHT_TICKS);
        return age < FrontierKnockout.SLOW_TICKS && age % FrontierKnockout.FLIGHT_STEP != 0;
    }

    public static int attackerId(PokemonEntity body) {
        var flight = body == null ? null : flights.get(body);
        return flight == null ? -1 : flight.attackerId();
    }

    private static void showToCombatants(PokemonEntity body, Smash.Session attacker, Smash.Session victim) {
        if (ArenaMatch.onKnockoutLaunch(body) || PrologueTraining.target(body)) return;
        var players = new HashSet<ServerPlayer>();
        if (attacker != null) players.add(attacker.player);
        if (victim != null) players.add(victim.player);
        var cue = new FrontierKnockout.Cue(body.level().dimension().location(), FrontierKnockout.CUE_TICKS,
                body.getId(), attackerId(body), List.of(body.getId()));
        for (ServerPlayer viewer : players) if (viewer.level() == body.level()) PacketDistributor.sendToPlayer(viewer, cue);
    }

    @SubscribeEvent
    public static void protectBody(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof PokemonEntity body && inFlight(body)) event.setCanceled(true);
    }

    public static void launch(PokemonEntity body, Entity source) {
        if (inFlight(body)) return;
        var attacker = source instanceof ServerPlayer player ? Smash.session(player) : source == null ? null : Smash.sessionOf(source);
        var victim = Smash.sessionOf(body);
        if (attacker != null) source = attacker.pokemon;
        Vec3 away = source == null ? body.getLookAngle().scale(-1) : body.position().subtract(source.position());
        Vec3 direction = new Vec3(away.x, 0, away.z).normalize();
        if (direction.lengthSqr() < 1e-6) direction = Vec3.directionFromRotation(0, body.getYRot()).scale(-1);
        int attackerId = source instanceof LivingEntity && source != body && source.level() == body.level() ? source.getId() : -1;
        flights.put(body, new Flight(body.level().getGameTime() + FLIGHT_TICKS, attackerId));
        ((ArenaKnockoutVisual) body).rumble$markKnockedOut();
        if (victim != null) {
            // RTC pins a controlled Pokémon to its trainer and disables physical simulation.
            // During KO, the trainer tick stops pinning it and the body becomes ballistic again.
            body.noPhysics = false;
            body.setNoGravity(false);
        }
        body.getNavigation().stop();
        // Same impulse, airborne condition and tumbling reaction as Combo's finisher.
        // Do not enqueue Combo.Flight: its landing can deal a second hit.
        applying.set(body);
        try {
            Motion.knock(body, direction.scale(2.6 * Fx3D.weightScale(body)).add(0, .55, 0));
            Conditions.launch(body, FLIGHT_TICKS);
            Fx3D.fall(body, direction, FrontierKnockout.TUMBLE_TICKS);
        } finally {
            applying.remove();
        }
        showToCombatants(body, attacker, victim);
    }
}
