package com.p1nero.cobblemon.rumblecore.combat.reward;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.duel.Duels;
import com.johnn.realtimecobblemon.moves.Decoys;
import com.p1nero.cobblemon.rumblecore.prologue.DailyCommissions;
import com.p1nero.cobblemon.rumblecore.wallet.KnockoutWallet;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;

public final class KnockoutRewards {
    private static final String SETTLED = "cobblemon_rumble_core:knockout_settled";

    private KnockoutRewards() {}

    public static void settle(Smash.Session session, PokemonEntity target) {
        if (!(target.level() instanceof ServerLevel) || session.player.level() != target.level()
                || Smash.session(session.player) != session || Duels.groupOf(session.player) != null
                || !target.getPokemon().isWild() || Decoys.isDecoy(target)
                || target.isAlive() && !target.getPokemon().isFainted() && !WildCapture.stunned(target)) return;
        var data = target.getPersistentData();
        // Several hits can observe a fainted entity before it disappears. Settle once on the server.
        if (data.getBoolean(SETTLED)) return;
        data.putBoolean(SETTLED, true);
        DailyCommissions.pokemon(session.player, "defeat", target.getPokemon());
        RealtimeEffort.award(session.player, session.pokemon.getPokemon(), target.getPokemon());
        if (ModList.get().isLoaded("cobbledollars")) KnockoutWallet.credit(session.player, target.getPokemon().getMaxHealth());
    }
}
