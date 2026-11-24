package com.p1nero.cobblemon.rumblecore.combat.arena.frontier;

import com.mojang.logging.LogUtils;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import com.p1nero.misc.custom_instances.instance.InstanceSession;
import com.p1nero.misc.custom_instances.network.InstanceNetwork;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

/** Observers visit the arena without becoming Custom Instances members or combatants. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class FrontierSpectators {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RETURN = "rumble_frontier_spectator_return";
    private static final Map<UUID, ArenaMatch> WATCHING = new HashMap<>();
    private record Transfer(ServerPlayer player, ServerLevel destination) {}
    private static final ThreadLocal<Transfer> ENTRY = new ThreadLocal<>();

    private FrontierSpectators() {}

    public static boolean permitsEntry(ServerPlayer player, ServerLevel destination) {
        var entry = ENTRY.get();
        return entry != null && entry.player() == player && entry.destination() == destination && player.isSpectator();
    }

    public static boolean isEntering(ServerPlayer player) { return permitsEntry(player, player.serverLevel()); }

    public static boolean isWatching(ServerPlayer player, InstanceSession session) {
        return session != null && session == WATCHING.get(player.getUUID()) && player.serverLevel() == session.getLevel() && player.isSpectator();
    }

    public static void enter(ArenaMatch match, List<UUID> ids) {
        var level = match.getLevel();
        var spawn = match.getSpawn();
        for (var id : ids) {
            var player = level.getServer().getPlayerList().getPlayer(id);
            if (player == null || player.hasDisconnected() || !ArenaMatch.available(player) || snapshot(player).contains(RETURN)
                    || InstanceManager.isInstanceDimension(player.level().dimension()) || FrontierRooms.inWaitingRoom(id)) continue;
            var data = new CompoundTag();
            data.putString("dimension", player.level().dimension().location().toString());
            data.putInt("mode", player.gameMode.getGameModeForPlayer().getId());
            data.putDouble("x", player.getX());
            data.putDouble("y", player.getY());
            data.putDouble("z", player.getZ());
            data.putFloat("yaw", player.getYRot());
            data.putFloat("pitch", player.getXRot());
            var persisted = snapshot(player);
            persisted.put(RETURN, data);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
            try {
                player.setGameMode(GameType.SPECTATOR);
                ENTRY.set(new Transfer(player, level));
                InstanceNetwork.sendEnvironment(player, match);
                player.teleportTo(level, spawn.getX() + .5, spawn.getY() + 6, spawn.getZ() + .5, player.getYRot(), 25);
                if (player.serverLevel() != level) throw new IllegalStateException("Spectator transfer cancelled");
                WATCHING.put(id, match);
                player.sendSystemMessage(Component.translatable("rumble.frontier.spectating_match")
                        .withStyle(style -> style.withUnderlined(true).withClickEvent(
                                new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rumble_frontier_leave_spectator"))));
            } catch (RuntimeException failure) {
                LOGGER.warn("Could not transfer Frontier spectator {}", id, failure);
                tryRestore(player);
                player.sendSystemMessage(Component.translatable("rumble.frontier.error.spectator_entry"));
            } finally {
                ENTRY.remove();
            }
        }
    }

    public static void end(MinecraftServer server, ArenaMatch match) {
        for (var id : List.copyOf(WATCHING.keySet())) {
            if (!WATCHING.remove(id, match)) continue;
            var player = server.getPlayerList().getPlayer(id);
            if (player != null && !player.hasDisconnected()) tryRestore(player);
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !snapshot(player).contains(RETURN, Tag.TAG_COMPOUND)) return;
        var match = WATCHING.get(player.getUUID());
        if (match == null) { // Reconnect, crash, or a replacement player after respawn.
            if (player.tickCount % 20 == 1) tryRestore(player);
        } else if (player.serverLevel() != match.getLevel() || !player.isSpectator()) tryRestore(player);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) recoverNextTick(player);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) recoverNextTick(player);
    }

    private static void recoverNextTick(ServerPlayer player) {
        if (!snapshot(player).contains(RETURN, Tag.TAG_COMPOUND)) return;
        WATCHING.remove(player.getUUID());
        player.server.tell(new TickTask(player.server.getTickCount(), () -> {
            if (!player.hasDisconnected()) tryRestore(player);
        }));
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) WATCHING.remove(player.getUUID());
        // Leave the return record in player NBT. Do not teleport a player whose connection is closing.
    }

    @SubscribeEvent
    public static void stop(ServerStoppingEvent event) {
        for (var player : event.getServer().getPlayerList().getPlayers())
            if (!player.hasDisconnected()) tryRestore(player);
        WATCHING.clear();
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("rumble_frontier_leave_spectator").executes(ctx ->
                tryRestore(ctx.getSource().getPlayerOrException()) ? 1 : 0));
    }

    private static CompoundTag snapshot(ServerPlayer player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static boolean tryRestore(ServerPlayer player) {
        try {
            return restore(player);
        } catch (RuntimeException failure) {
            LOGGER.warn("Could not restore Frontier spectator {}; retaining the return record", player.getUUID(), failure);
            return false;
        }
    }

    private static boolean restore(ServerPlayer player) {
        var persisted = snapshot(player);
        if (!persisted.contains(RETURN, Tag.TAG_COMPOUND)) return false;
        var data = persisted.getCompound(RETURN);
        WATCHING.remove(player.getUUID());
        var key = ResourceLocation.tryParse(data.getString("dimension"));
        var target = key == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, key));
        if (target == null) {
            target = player.server.overworld();
            var spawn = target.getSharedSpawnPos();
            player.teleportTo(target, spawn.getX() + .5, spawn.getY(), spawn.getZ() + .5, 0, 0);
        } else {
            player.teleportTo(target, data.getDouble("x"), data.getDouble("y"), data.getDouble("z"),
                    data.getFloat("yaw"), data.getFloat("pitch"));
        }
        if (player.serverLevel() != target) return false; // Keep recovery data for the next tick/login.
        player.setCamera(player);
        var mode = GameType.byId(data.getInt("mode"));
        player.setGameMode(mode);
        if (player.gameMode.getGameModeForPlayer() != mode) return false;
        persisted.remove(RETURN);
        return true;
    }
}
