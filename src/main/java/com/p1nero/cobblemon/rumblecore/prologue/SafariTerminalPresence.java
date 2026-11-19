package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.mixin.cobblesafari.SafariTeleporterAccess;
import net.minecraft.server.level.ServerPlayer;

final class SafariTerminalPresence {
    private SafariTerminalPresence() {}

    static void keepAlive(ServerPlayer player) {
        if (SafariTeleporterAccess.rumble$pending().contains(player.getUUID())) {
            SafariTeleporterAccess.rumble$presence().put(player.getUUID(), SafariTeleporterAccess.rumble$tick());
        }
    }
}
