package com.p1nero.cobblemon.rumblecore.phone;

import com.p1nero.cobblemon.rumblecore.guide.RumbleGuides;
import com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.arcadia.arc_quest.guide.runtime.GuideUnlockService;

/** Give each new trainer their own storage network and keep the starter grant idempotent. */
final class StarterStorage {
    private static final String GRANTED = "rumble.starter_cloud";

    private StarterStorage() {}

    static boolean granted(ServerPlayer player) { return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(GRANTED); }

    static void grant(ServerPlayer player) {
        if (granted(player)) return;
        var net = DimensionsNet.getPrimaryNetFromPlayer(player);
        if (net == null) net = DimensionsNet.createNewNetForPlayer(player, Long.MAX_VALUE, Integer.MAX_VALUE);
        if (net == null) return;
        new GuideUnlockService().grant(player, RumbleGuides.CLOUD_STORAGE);
        var persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putBoolean(GRANTED, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
