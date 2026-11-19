package com.p1nero.cobblemon.rumblecore.mixin.cobblesafari;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import maxigregrze.cobblesafari.teleporter.TeleporterTickHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TeleporterTickHandler.class, remap = false)
public interface SafariTeleporterAccess {
    @Accessor("pendingConfirmation") static Set<UUID> rumble$pending() { throw new AssertionError(); }
    @Accessor("playersOnTeleporter") static Map<UUID, Long> rumble$presence() { throw new AssertionError(); }
    @Accessor("declinedOrFailed") static Set<UUID> rumble$declined() { throw new AssertionError(); }
    @Accessor("currentServerTick") static long rumble$tick() { throw new AssertionError(); }
}
