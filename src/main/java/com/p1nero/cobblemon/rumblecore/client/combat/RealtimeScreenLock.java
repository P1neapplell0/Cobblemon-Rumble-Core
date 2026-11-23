package com.p1nero.cobblemon.rumblecore.client.combat;

import com.cobblemon.mod.common.client.gui.summary.Summary;
import com.johnn.realtimecobblemon.client.ClientSmash;
import com.johnn.realtimecobblemon.client.SummaryScreen;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class RealtimeScreenLock {
    private RealtimeScreenLock() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockInspection(ScreenEvent.Opening event) {
        var screen = event.getNewScreen();
        // Check the destination rather than a particular key; Cobblemon can open Summary directly.
        if (ClientSmash.active() && (screen instanceof Summary || screen instanceof SummaryScreen
                || screen instanceof InventoryScreen)) event.setCanceled(true);
    }
}
