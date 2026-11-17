package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.localization;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(remap = false, targets = {
        "com.johnn.realtimecobblemon.client.ClashHud",
        "com.johnn.realtimecobblemon.client.SettingsScreen",
        "com.johnn.realtimecobblemon.client.SummaryScreen",
        "com.johnn.realtimecobblemon.client.anime.WorldText"
})
abstract class ClientTextSites {}
