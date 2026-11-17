package com.p1nero.cobblemon.rumblecore.combat.reward;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonGainedEvent;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

final class KnockoutCapture {
    private KnockoutCapture() {}

    static void settle(ServerPlayer player, PokemonEntity target) {
        if (!UncatchableProperty.INSTANCE.isCatchable(target)) return;
        var pokemon = target.getPokemon();
        var storage = Cobblemon.INSTANCE.getStorage();
        var pc = storage.getPC(player);
        // Each knockout is a new individual, sent straight to the PC even if the party has space.
        // Clone away from the dying entity so its faint/removal cannot mutate the stored Pokémon.
        Pokemon captured = pokemon.clone(true, player.registryAccess());
        captured.heal();
        captured.setOriginalTrainer(player.getUUID());
        captured.refreshOriginalTrainer();
        boolean observes = false;
        if (pc != null) for (var observer : pc.getObservingPlayers()) if (observer == player) observes = true;
        if (pc != null && pc.add(captured)) {
            // Store coordinates notify PC observers; keep the dex/gained event when the owner isn't observing.
            if (!observes) CobblemonEvents.POKEMON_GAINED.post(new PokemonGainedEvent(player.getUUID(), captured));
            player.sendSystemMessage(Component.translatable("rumble.combat.captured", captured.getDisplayName(false)));
        } else player.sendSystemMessage(Component.translatable("rumble.combat.pc_full"));
    }
}
