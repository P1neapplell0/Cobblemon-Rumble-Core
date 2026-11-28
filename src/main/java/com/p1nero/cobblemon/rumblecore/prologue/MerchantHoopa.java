package com.p1nero.cobblemon.rumblecore.prologue;

import com.mojang.logging.LogUtils;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import maxigregrze.cobblesafari.block.dungeon.DungeonPortalBlockEntity;
import maxigregrze.cobblesafari.compat.EntryFeeHelper;
import maxigregrze.cobblesafari.dungeon.DungeonConfig;
import maxigregrze.cobblesafari.dungeon.DungeonDimensions;
import maxigregrze.cobblesafari.dungeon.DungeonTeleportCountdown;
import maxigregrze.cobblesafari.dungeon.DungeonTpAcceptHandler;
import maxigregrze.cobblesafari.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class MerchantHoopa {
    public static final int PRICE = 5000;
    private static final String KEY = "rumble.hoopa.";
    private record Pending(ServerLevel level, DungeonPortalBlockEntity portal) {}
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private MerchantHoopa() {}

    private static boolean available(ServerPlayer player) {
        return LegendaryCommission.unlocked(player) && MerchantPanels.nearby(player) != null
                && ArenaMatch.available(player);
    }

    static ListTag destinations(MinecraftServer server) {
        var rows = new ListTag();
        for (var id : DungeonDimensions.getPortalDestinationIdsSorted()) {
            var config = DungeonDimensions.getDungeonById(id);
            if (!usable(server, config) || id.length() > 128) continue;
            var row = new CompoundTag();
            row.putString("id", id);
            var key = config.getDimensionKey().location();
            row.putString("name", "dimension." + key.getNamespace() + "." + key.getPath());
            rows.add(row);
            if (rows.size() >= 128) break;
        }
        return rows;
    }

    private static boolean usable(MinecraftServer server, DungeonConfig config) {
        return config != null && !config.isExternallyManaged() && server.getLevel(config.getDimensionKey()) != null;
    }

    static void open(ServerPlayer player) {
        if (available(player)) MerchantPanels.showHoopa(player, destinations(player.server));
    }

    static void enter(ServerPlayer player, String id) {
        if (!available(player)) { tell(player, "unavailable"); return; }
        var uuid = player.getUUID();
        if (DungeonTeleportCountdown.isTeleporting(uuid) || DungeonTpAcceptHandler.hasPendingConfirmation(uuid)) {
            tell(player, "busy");
            return;
        }
        var config = DungeonDimensions.getDungeonById(id);
        if (!usable(player.server, config) || !DungeonDimensions.getPortalDestinationIdsSorted().contains(id)) {
            tell(player, "unavailable");
            return;
        }
        var cost = BigInteger.valueOf(PRICE);
        var balance = PlayerExtensionKt.getCobbleDollars(player);
        if (balance.compareTo(cost) < 0) { tell(player, "funds"); return; }
        if (start(player, config, balance, cost)) tell(player, "preparing");
        else tell(player, "unavailable");
    }

    private static boolean start(ServerPlayer player, DungeonConfig config, BigInteger balance, BigInteger cost) {
        var uuid = player.getUUID();
        var level = player.serverLevel();
        var portal = new DungeonPortalBlockEntity(player.blockPosition(), ModBlocks.DUNGEON_PORTAL.defaultBlockState());
        portal.setLevel(level);
        portal.setPortalId(UUID.randomUUID());
        portal.setDungeonDimensionId(config.getId());
        portal.setSpawnTick(level.getGameTime());
        var fee = new DungeonTpAcceptHandler.ChargedFee(EntryFeeHelper.FeeType.COBBLEDOLLAR, "", cost.intValueExact());
        PENDING.put(uuid, new Pending(level, portal));
        PlayerExtensionKt.setCobbleDollars(player, balance.subtract(cost));
        try {
            if (DungeonTeleportCountdown.startTeleportSequence(player, portal, fee)) return true;
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("Could not open the merchant Hoopa Ring for {}", uuid, e);
        }
        PENDING.remove(uuid);
        PlayerExtensionKt.setCobbleDollars(player, PlayerExtensionKt.getCobbleDollars(player).add(cost));
        return false;
    }

    public static DungeonPortalBlockEntity virtualPortal(ServerPlayer player, ServerLevel level, BlockPos pos) {
        var pending = PENDING.get(player.getUUID());
        return pending != null && pending.level() == level && pending.portal().getBlockPos().equals(pos)
                ? pending.portal() : null;
    }

    private static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(KEY + key), true);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        PENDING.entrySet().removeIf(row -> {
            var id = row.getKey();
            if (!DungeonTeleportCountdown.isTeleporting(id)) return true;
            var player = server.getPlayerList().getPlayer(id);
            if (player != null && (player.serverLevel() != row.getValue().level() || !ArenaMatch.available(player))) {
                DungeonTeleportCountdown.cancelTeleport(server, id);
                return true;
            }
            return false;
        });
    }

    // Cancel while the player's CobbleDollars account can still receive Safari's native refund.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PENDING.remove(player.getUUID()) != null)
            DungeonTeleportCountdown.cancelTeleport(player.server, player.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void stop(ServerStoppingEvent event) {
        PENDING.keySet().forEach(id -> DungeonTeleportCountdown.cancelTeleport(event.getServer(), id));
        PENDING.clear();
    }
}
