package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Conditions;
import com.johnn.realtimecobblemon.moves.React;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat.ConditionTimers;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class RealtimeProtection {
    public static final int MEGA_TICKS = 60;
    private static final int RECOVERY_TICKS = 100;
    private static final Map<LivingEntity, Control> CONTROLS = new WeakHashMap<>();
    private static final Map<PokemonEntity, Long> MEGA = new WeakHashMap<>();

    private static final class Control {
        final String effect;
        final long ends;
        long immuneUntil;
        int hits;
        boolean released;

        Control(String effect, long ends) {
            this.effect = effect;
            this.ends = ends;
            immuneUntil = ends + RECOVERY_TICKS;
        }
    }

    private RealtimeProtection() {}

    public static void beginMega(PokemonEntity body) {
        if (body.level() instanceof ServerLevel) MEGA.put(body, body.level().getGameTime() + MEGA_TICKS);
    }

    public static void endMega(PokemonEntity body) { MEGA.remove(body); }

    public static boolean megaProtected(LivingEntity target) {
        if (MEGA.isEmpty() || !(target.level() instanceof ServerLevel)) return false;
        var body = body(target);
        return body != null && body.level() instanceof ServerLevel
                && body.level().getGameTime() < MEGA.getOrDefault(body, 0L);
    }

    private static PokemonEntity body(LivingEntity target) {
        if (target instanceof PokemonEntity pokemon) return pokemon;
        if (target instanceof Player player) {
            var session = Smash.session(player);
            if (session != null) return session.pokemon;
        }
        return null;
    }

    public static boolean hardControl(String effect) {
        return switch (effect) {
            case "slp", "frz", "flinch", "confusion", "partiallytrapped", "attract", "encore", "bind" -> true;
            default -> false;
        };
    }

    private static LivingEntity controller(PokemonEntity body) {
        var session = Smash.sessionOf(body);
        return session == null ? null : session.player;
    }

    public static boolean rejects(LivingEntity target, String effect) {
        if (megaProtected(target)) return true;
        PokemonEntity body = body(target);
        if (body == null || !hardControl(effect) || !(body.level() instanceof ServerLevel)) return false;
        long now = body.level().getGameTime();
        return protectedFromControl(body, now) || protectedFromControl(target, now)
                || target == body && protectedFromControl(controller(body), now);
    }

    private static boolean protectedFromControl(LivingEntity target, long now) {
        var control = CONTROLS.get(target);
        return control != null && now < control.immuneUntil;
    }

    public static void applied(LivingEntity target, String effect) {
        if (body(target) == null || !hardControl(effect)) return;
        var state = timers(target);
        if (state == null) return;
        long ends = switch (effect) {
            case "slp", "frz" -> state.rumble$statusUntil();
            case "confusion" -> state.rumble$confusedUntil();
            case "bind" -> state.rumble$boundUntil();
            default -> state.rumble$stunUntil();
        };
        if (ends > target.level().getGameTime()) CONTROLS.put(target, new Control(effect, ends));
    }

    private static ConditionTimers timers(LivingEntity target) {
        return (ConditionTimers) (Object) Conditions.get(target);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventMegaDamage(LivingIncomingDamageEvent event) {
        if (megaProtected(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void vanillaHit(LivingDamageEvent.Post event) {
        hit(event.getEntity(), event.getSource(), event.getNewDamage());
    }

    public static void hit(LivingEntity target, DamageSource source, float damage) {
        if (CONTROLS.isEmpty() || !(target.level() instanceof ServerLevel) || damage <= 0) return;
        PokemonEntity body = body(target);
        Entity attacker = source.getEntity();
        if (body == null || attacker == null || !body.isAlive() || body.getPokemon().isFainted()) return;
        var proxy = controller(body);
        if (attacker == body || attacker == proxy) return;
        var attackerSession = Smash.sessionOf(attacker);
        if (attackerSession != null && attackerSession.pokemon == body) return;
        countHit(body);
        if (proxy != null) countHit(proxy);
    }

    private static void countHit(LivingEntity target) {
        var control = CONTROLS.get(target);
        long now = target.level().getGameTime();
        if (control == null || control.released || now >= control.ends) return;
        var state = timers(target);
        if (state == null || !stillControlled(target, control.effect, now)) return;
        if (++control.hits < 3) return;
        control.released = true;
        control.immuneUntil = now + RECOVERY_TICKS;
        switch (control.effect) {
            case "slp", "frz" -> {
                React.clear(target, control.effect);
                if (target instanceof PokemonEntity pokemon) pokemon.getPokemon().setStatus(null);
                if (control.effect.equals("slp")) Conditions.insomnia(target, RECOVERY_TICKS);
                else target.setTicksFrozen(0);
                // RTC treats the expiry tick itself as active. Set it after insomnia rewrites it.
                state.rumble$expireStatus(now - 1);
                Conditions.unstun(target);
            }
            case "confusion" -> state.rumble$clearConfusion(0);
            case "bind" -> Conditions.unbind(target);
            default -> Conditions.unstun(target);
        }
        var body = body(target);
        var session = body == null ? null : Smash.sessionOf(body);
        if (session != null) Smash.sendHud(session);
    }

    private static boolean stillControlled(LivingEntity target, String effect, long now) {
        return switch (effect) {
            case "slp", "frz" -> effect.equals(Conditions.status(target)) && timers(target).rumble$statusUntil() > now;
            case "confusion" -> Conditions.confused(target);
            case "bind" -> Conditions.bound(target);
            default -> Conditions.stunned(target);
        };
    }
}
