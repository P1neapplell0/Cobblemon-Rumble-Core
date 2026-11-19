package com.p1nero.cobblemon.rumblecore.combat.reward;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.pokemon.stats.SidemodEvSource;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

final class RealtimeEffort {
    private static final Map<Item, Stats> POWER = Map.of(
            CobblemonItems.POWER_WEIGHT, Stats.HP, CobblemonItems.POWER_BRACER, Stats.ATTACK,
            CobblemonItems.POWER_BELT, Stats.DEFENCE, CobblemonItems.POWER_LENS, Stats.SPECIAL_ATTACK,
            CobblemonItems.POWER_BAND, Stats.SPECIAL_DEFENCE, CobblemonItems.POWER_ANKLET, Stats.SPEED);

    private RealtimeEffort() {}

    static void award(ServerPlayer player, Pokemon attacker, Pokemon defeated) {
        if (!player.getUUID().equals(attacker.getOwnerUUID())) return;
        var yield = new HashMap<>(defeated.getForm().getEvYield());
        var power = POWER.get(attacker.heldItem().getItem());
        if (power != null) yield.merge(power, 8, Integer::sum);
        if (ModList.get().isLoaded("cobbleeffortvr")) {
            int points = yield.values().stream().mapToInt(Integer::intValue).sum();
            if (points > 0) Pool.award(player, points);
        } else {
            var source = new SidemodEvSource(CobblemonRumbleCore.MOD_ID, attacker);
            yield.forEach((stat, points) -> { if (points > 0) attacker.getEvs().add(stat, points, source); });
        }
    }

    private static final class Pool {
        private static void award(ServerPlayer player, int points) {
            xiaocaoawa.minecraft.mod.cobbleeffortvr.pool.EvPool.addPoints(player, points);
        }
    }
}
