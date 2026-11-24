package com.p1nero.cobblemon.rumblecore.combat;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.server.BattleChallengePacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.SmashConfig;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.johnn.realtimecobblemon.duel.Duels;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaPackets;
import com.p1nero.misc.custom_instances.api.InstanceApi;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueTraining;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import kotlin.Unit;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-authoritative free-roam deployment; ordinary sends still leave the Pokémon following. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class RealtimeBattleEntry {
    private static final Map<ServerPlayer, Object> PENDING = new WeakHashMap<>();
    private static final Map<ServerPlayer, WildChoice> CHOICES = new WeakHashMap<>();
    private record WildChoice(UUID token, ServerLevel level, Map<Integer, UUID> slots) {}
    private RealtimeBattleEntry() {}

    public static void cancelPending(ServerPlayer player) {
        PENDING.remove(player);
        closeChoice(player, CHOICES.remove(player));
    }

    private static boolean freeRoam(ServerPlayer player) {
        var level = player.serverLevel();
        return ArenaMatch.inArena(player) == null && InstanceApi.find(level).isEmpty() && !PrologueTraining.isTrainingLevel(level)
                && !PrologueTraining.restrict(player) && Duels.groupOf(player) == null;
    }

    private static boolean canDeploy(ServerPlayer player) {
        return freeRoam(player) && player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isSleeping()
                && !Smash.active(player) && !Smash.inBattle(player) && SmashConfig.<Boolean>get(SmashConfig.ENABLED);
    }

    /** Encoded negative target IDs carry the client's highlighted party slot, not an arbitrary Pokémon. */
    public static boolean enterSelected(ServerPlayer player, int target) {
        if (target > -2 || target < -7 || !canDeploy(player) || Smash.findOwn(player) != null) return false;
        if (PENDING.containsKey(player)) return true;
        closeChoice(player, CHOICES.remove(player));
        deploy(player, -2 - target);
        return true;
    }

    private static boolean usable(Pokemon mon) {
        return mon != null && !mon.isFainted() && mon.getTetheringId() == null;
    }

    private static void deploy(ServerPlayer player, int slot) {
        var mon = Cobblemon.INSTANCE.getStorage().getParty(player).get(slot);
        if (!usable(mon)) {
            player.displayClientMessage(Component.translatable("rumble.realtime.invalid_selected"), true);
            return;
        }
        var level = player.serverLevel();
        Object request = new Object();
        PENDING.put(player, request);
        if (mon.getEntity() != null) {
            enterFreeRoam(player, mon, mon.getEntity(), level, request);
            return;
        }
        var position = PlayerExtensionsKt.raycastSafeSendout(player, mon, 12.0, 5.0, ClipContext.Fluid.ANY);
        if (position == null) {
            PENDING.remove(player, request);
            player.displayClientMessage(Component.translatable("rumble.realtime.no_send_space"), true);
            return;
        }
        mon.sendOutWithAnimation(player, level, position, null, true, null, entity -> Unit.INSTANCE)
                .whenCompleteAsync((entity, error) -> {
                    if (error == null) enterFreeRoam(player, mon, entity, level, request);
                    else PENDING.remove(player, request);
                }, player.getServer());
    }

    private static void enterFreeRoam(ServerPlayer player, Pokemon mon, PokemonEntity body, ServerLevel level, Object request) {
        if (!PENDING.remove(player, request) || player.hasDisconnected() || player.serverLevel() != level || !canDeploy(player)
                || !usable(mon) || body == null || body.level() != level || !body.isAlive() || mon.getEntity() != body
                || mon.getOwnerPlayer() != player || Cobblemon.INSTANCE.getStorage().getParty(player).get(mon.getUuid()) != mon) return;
        var failure = Smash.start(player, body);
        if (failure != null) player.displayClientMessage(failure, true);
    }

    public static void knockedOut(ServerPlayer player) {
        if (!canDeploy(player) || player.hasDisconnected()) return;
        PENDING.remove(player);
        var old = CHOICES.remove(player);
        closeChoice(player, old);
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);
        var roster = new ArrayList<ArenaPackets.Option>();
        var slots = new java.util.HashMap<Integer, UUID>();
        for (int i = 0; i < 6; i++) {
            var mon = party.get(i);
            if (mon == null || mon.getTetheringId() != null) continue;
            roster.add(new ArenaPackets.Option(i, mon));
            if (usable(mon)) slots.put(i, mon.getUuid());
        }
        if (slots.isEmpty()) return;
        var choice = new WildChoice(UUID.randomUUID(), player.serverLevel(), Map.copyOf(slots));
        CHOICES.put(player, choice);
        PacketDistributor.sendToPlayer(player, new ArenaPackets.Switch(choice.token(), ArenaPackets.WILD_ROUND, roster));
    }

    public static void pick(ServerPlayer player, ArenaPackets.Pick packet) {
        var choice = CHOICES.get(player);
        if (choice == null || !choice.token().equals(packet.match())) return;
        CHOICES.remove(player);
        closeChoice(player, choice);
        if (packet.index() < 0 || packet.index() >= 6 || !canDeploy(player) || choice.level() != player.serverLevel()) return;
        var mon = Cobblemon.INSTANCE.getStorage().getParty(player).get(packet.index());
        if (usable(mon) && mon.getUuid().equals(choice.slots().get(packet.index()))) deploy(player, packet.index());
    }

    @SubscribeEvent
    public static void validateChoice(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var choice = CHOICES.get(player);
        if (choice != null && (choice.level() != player.serverLevel() || !canDeploy(player))) closeChoice(player, CHOICES.remove(player));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PENDING.remove(player);
        CHOICES.remove(player);
    }

    private static void closeChoice(ServerPlayer player, WildChoice choice) {
        if (choice != null && !player.hasDisconnected()) PacketDistributor.sendToPlayer(player,
                new ArenaPackets.Switch(choice.token(), ArenaPackets.WILD_ROUND, List.of()));
    }

    public static void challenge(ServerPlayer player, BattleChallengePacket packet) {
        var level = player.serverLevel();
        var targeted = level.getEntity(packet.getTargetedEntityId());
        var opponent = targeted instanceof PokemonEntity pokemon ? pokemon.getOwner() : targeted;
        if (opponent instanceof ServerPlayer other) {
            ArenaMatch.challenge(player, other, packet.getSelectedPokemonId());
            return;
        }
        if (ArenaMatch.inArena(player) != null) return;
        if (!(targeted instanceof PokemonEntity target)
                || !validTarget(player, target) || Smash.active(player) || Duels.groupOf(player) != null
                || !player.isAlive() || player.isPassenger() || player.isSleeping()) return;
        Pokemon selected = Cobblemon.INSTANCE.getStorage().getParty(player).get(packet.getSelectedPokemonId());
        if (selected == null || selected.isFainted()) return;
        closeChoice(player, CHOICES.remove(player));
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
        return PrologueTraining.allowChallenge(player, target) && target.level() == player.level() && target.getPokemon().isWild() && target.canBattle(player)
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
        else {
            PrologueTraining.challenged(player, target);
            if (!PrologueTraining.isTrainingLevel(level)) BattleMusic.engage(Smash.session(player), target, true);
        }
    }
}
