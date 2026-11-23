package com.p1nero.cobblemon.rumblecore.client.combat;

import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUI;
import com.johnn.realtimecobblemon.client.ClientSmash;
import com.nbp.cobblemon_smartphone.client.scanner.ScannerManager;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** A held combat guard must not turn into a new vanilla use action after combat or an arena exit. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class BattleUseGuard {
    private static boolean needsRelease, wasInInstance;
    private static int returnTicks;

    private BattleUseGuard() {}

    public static boolean blocksVanillaUse() { return ClientSmash.active() || needsRelease; }

    @SubscribeEvent
    public static void beforeInput(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        boolean inInstance = mc.level != null && InstanceManager.isInstanceDimension(mc.level.dimension());
        if (wasInInstance && !inInstance && mc.level != null) returnTicks = 5;
        wasInInstance = inInstance;
        if (ClientSmash.active() || returnTicks > 0) {
            needsRelease = true;
            if (ScannerManager.INSTANCE.isActive()) ScannerManager.INSTANCE.finishDeactivation();
        }
    }

    @SubscribeEvent
    public static void afterInput(ClientTickEvent.Post event) {
        if (returnTicks > 0) returnTicks--;
        // Clear after input processing, so a press/release queued on the same tick cannot leak through.
        if (!ClientSmash.active() && returnTicks == 0 && !Minecraft.getInstance().options.keyUse.isDown()) needsRelease = false;
    }

    @SubscribeEvent
    public static void preventLateDexOpen(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof PokedexGUI && blocksVanillaUse()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        needsRelease = wasInInstance = false;
        returnTicks = 0;
    }
}
