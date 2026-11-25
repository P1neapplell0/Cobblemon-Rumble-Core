package com.p1nero.cobblemon.rumblecore.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class TrialsPokemonDamage {
    private TrialsPokemonDamage() {}

    @SubscribeEvent
    public static void allowRealtimeHits(EntityInvulnerabilityCheckEvent event) {
        // The spawner sets the vanilla flag even if the per-species flag is false
        // when spawnedPokemonMustBeDefeatedInBattle is enabled globally.
        if (!event.getOriginalInvulnerability() || !(event.getEntity() instanceof PokemonEntity target)
                || !(target.level() instanceof ServerLevel) || target.isRemoved() || !target.isInvulnerable()
                || !target.getPokemon().getPersistentData().getBoolean("is_spawned_from_trial_spawner")) return;
        var source = event.getSource();
        if (source.is(DamageTypes.MOB_ATTACK) && source.getDirectEntity() instanceof PokemonEntity attacker
                && attacker != target && attacker.level() == target.level() && Smash.sessionOf(attacker) != null) {
            event.setInvulnerable(false);
        }
    }
}
