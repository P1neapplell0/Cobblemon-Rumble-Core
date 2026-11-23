package com.p1nero.cobblemon.rumblecore.combat.arena;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.moves.Brawl;
import com.johnn.realtimecobblemon.moves.Conditions;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Grants a short get-up window after RTC hitstun, without cancelling damage. */
@EventBusSubscriber(modid = "cobblemon_rumble_core")
public final class ArenaTenacity {
    private static final int DURATION = 80;
    private static final Map<PokemonEntity, State> SERVER = new WeakHashMap<>();

    private ArenaTenacity() {}

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PokemonEntity pokemon) || pokemon.level().isClientSide
                || !tracked(pokemon)) return;
        update(pokemon);
    }

    public static void staggered(PokemonEntity pokemon) {
        if (tracked(pokemon)) update(pokemon);
    }

    private static boolean tracked(PokemonEntity pokemon) {
        return !pokemon.level().isClientSide && (Smash.sessionOf(pokemon) != null || Brawl.fighting(pokemon));
    }

    private static State update(PokemonEntity pokemon) {
        boolean stunned = Conditions.stunned(pokemon);
        State state = SERVER.get(pokemon);
        if (state == null) {
            if (!stunned) return null;
            SERVER.put(pokemon, state = new State());
        }
        if (state.wasStunned && !stunned && pokemon.isAlive()) {
            state.until = pokemon.level().getGameTime() + DURATION;
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(pokemon,
                    new ArenaPackets.Tenacity(pokemon.getId(), pokemon.level().dimension().location(), DURATION));
        }
        state.wasStunned = stunned;
        if (!stunned && pokemon.level().getGameTime() >= state.until) SERVER.remove(pokemon);
        return state;
    }

    public static boolean active(PokemonEntity pokemon) {
        if (pokemon.level().isClientSide) return false;
        // Also detect recovery on a hit arriving before this entity's first free tick.
        State state = SERVER.containsKey(pokemon) ? update(pokemon) : null;
        return state != null && pokemon.level().getGameTime() < state.until;
    }

    private static final class State {
        boolean wasStunned;
        long until;
    }
}
