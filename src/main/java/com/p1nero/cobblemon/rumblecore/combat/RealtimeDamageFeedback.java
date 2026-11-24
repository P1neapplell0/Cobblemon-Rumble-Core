package com.p1nero.cobblemon.rumblecore.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Brawl;
import com.johnn.realtimecobblemon.moves.Fx3D;
import com.johnn.realtimecobblemon.moves.MoveEngine;
import com.johnn.realtimecobblemon.moves.Pops;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class RealtimeDamageFeedback {
    private static final Map<Entity, ArrayDeque<Float>> PENDING = new IdentityHashMap<>();

    private RealtimeDamageFeedback() {}

    @SubscribeEvent
    public static void onEntityDamage(LivingDamageEvent.Post event) {
        record(event.getEntity(), event.getSource(), event.getNewDamage());
    }

    public static void record(LivingEntity target, DamageSource source, float hp) {
        if (!(hp > 0) || !(target.level() instanceof ServerLevel)) return;
        Entity actor = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        var attacker = session(actor);
        if (attacker == null && session(target) == null && !(target instanceof PokemonEntity pokemon
                && (ArenaMatch.group(pokemon) != null || RealtimeKnockout.inFlight(pokemon) || Brawl.fighting(pokemon)))) return;

        var visible = Fx3D.visible(target);
        PENDING.computeIfAbsent(visible, ignored -> new ArrayDeque<>()).addLast(hp);
        if (attacker != null && visible instanceof PokemonEntity opponent && opponent != attacker.pokemon)
            MoveEngine.markTarget(attacker, opponent);
    }

    private static Smash.Session session(Entity entity) {
        return entity instanceof ServerPlayer player ? Smash.session(player)
                : entity instanceof PokemonEntity ? Smash.sessionOf(entity) : null;
    }

    public static void nativePop(Entity target) {
        // The normal hit path emits its pop immediately after applying the damage; keep its type/crit styling.
        var key = Fx3D.visible(target);
        var hits = PENDING.get(key);
        if (hits == null) return;
        hits.pollLast();
        if (hits.isEmpty()) PENDING.remove(key);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void flush(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        var unshown = new IdentityHashMap<>(PENDING);
        PENDING.clear();
        unshown.forEach((target, hits) -> {
            if (target.level().getServer() != event.getServer()) return;
            for (float hp : hits) Pops.damage(target, hp, 1, false);
        });
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) { PENDING.clear(); }
}
