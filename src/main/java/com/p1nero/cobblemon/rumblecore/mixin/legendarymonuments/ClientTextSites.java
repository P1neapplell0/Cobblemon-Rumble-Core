package com.p1nero.cobblemon.rumblecore.mixin.legendarymonuments;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/** Legendary Monuments 8.2 text producers; only their component/draw calls are adapted. */
@Pseudo
@Mixin(remap = false, targets = {
        "com.jorgaomc.legendarymonuments.client.ClientHudEvents",
        "com.jorgaomc.legendarymonuments.client.SanctuaryClientPacketHandler",
        "com.jorgaomc.legendarymonuments.client.SanctuaryScreen$1",
        "com.jorgaomc.legendarymonuments.client.SanctuaryScreen$2",
        "com.jorgaomc.legendarymonuments.client.SanctuaryScreen",
        "com.jorgaomc.legendarymonuments.client.screen.LegendaryMonumentsConfigScreen$SettingEntry",
        "com.jorgaomc.legendarymonuments.client.screen.LegendaryMonumentsConfigScreen",
        "com.jorgaomc.legendarymonuments.screens.ArcPhoneScreen",
        "com.jorgaomc.legendarymonuments.screens.ArcPhoneUi",
        "com.jorgaomc.legendarymonuments.screens.ChunkImage",
        "com.jorgaomc.legendarymonuments.screens.KeyItemsScreen",
        "com.jorgaomc.legendarymonuments.screens.LegendaryTrackingScreen",
        "com.jorgaomc.legendarymonuments.screens.QuestsScreen",
        "com.jorgaomc.legendarymonuments.screens.RegiStatueScreen",
        "com.jorgaomc.legendarymonuments.screens.SystemUpgradesScreen"
})
abstract class ClientTextSites {}
