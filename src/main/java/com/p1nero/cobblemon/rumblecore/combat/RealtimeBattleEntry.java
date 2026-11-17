package com.p1nero.cobblemon.rumblecore.combat;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.server.BattleChallengePacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.duel.Duels;
import java.util.Map;
import java.util.WeakHashMap;
import kotlin.Unit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;

/** Server-thread challenge entry. A regular send-out is never a request to take control. */
public final class RealtimeBattleEntry {
    private static final Map<ServerPlayer, Object> PENDING = new WeakHashMap<>();
    private RealtimeBattleEntry() {}

    public static void cancelPending(ServerPlayer player) { PENDING.remove(player); }

    public static void challenge(ServerPlayer player, BattleChallengePacket packet) {
        var level = player.serverLevel();
        if (!(level.getEntity(packet.getTargetedEntityId()) instanceof PokemonEntity target)
                || !validTarget(player, target) || Smash.active(player) || Duels.groupOf(player) != null
                || !player.isAlive() || player.isPassenger() || player.isSleeping()) return;
        Pokemon selected = Cobblemon.INSTANCE.getStorage().getParty(player).get(packet.getSelectedPokemonId());
        if (selected == null || selected.isFainted()) return;
        Object request = new Object();
        PENDING.put(player, request);
        PokemonEntity out = selected.getEntity();
        if (out != null) {
            enter(player, selected, target, out, level, request);
            return;
        }
        var position = PlayerExtensionsKt.raycastSafeSendout(player, selected, 12.0, 5.0, ClipContext.Fluid.ANY);
        if (position == null) { cancelPending(player); return; }
        // Keep the original throw animation. Completion must still match this challenge and this world.
        selected.sendOutWithAnimation(player, level, position, null, true, null, entity -> Unit.INSTANCE)
                .whenCompleteAsync((entity, error) -> {
                    if (error == null) enter(player, selected, target, entity, level, request);
                    else PENDING.remove(player, request);
                }, player.getServer());
    }

    private static boolean validTarget(ServerPlayer player, PokemonEntity target) {
        return target.level() == player.level() && target.getPokemon().isWild() && target.canBattle(player)
                && player.distanceToSqr(target) <= Math.pow(Cobblemon.INSTANCE.getConfig().getBattleWildMaxDistance(), 2)
                && player.hasLineOfSight(target) && !player.isSpectator();
    }

    private static void enter(ServerPlayer player, Pokemon selected, PokemonEntity target, PokemonEntity out,
                              ServerLevel level, Object request) {
        if (!PENDING.remove(player, request)) return;
        if (player.getServer().getPlayerList().getPlayer(player.getUUID()) != player || player.serverLevel() != level
                || !player.isAlive() || out == null || !out.isAlive() || out.level() != level
                || selected.getEntity() != out || selected.getOwnerPlayer() != player
                || Cobblemon.INSTANCE.getStorage().getParty(player).get(selected.getUuid()) != selected
                || !validTarget(player, target) || Duels.groupOf(player) != null || Smash.active(player)) return;
        var failure = Smash.start(player, out);
        if (failure != null) player.displayClientMessage(failure, true);
    }
}
