package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.localization;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(remap = false, targets = {
        "com.johnn.realtimecobblemon.arena.Arenas",
        "com.johnn.realtimecobblemon.field.Field",
        "com.johnn.realtimecobblemon.moves.Conditions",
        "com.johnn.realtimecobblemon.moves.Defense",
        "com.johnn.realtimecobblemon.moves.Experience",
        "com.johnn.realtimecobblemon.moves.Fields",
        "com.johnn.realtimecobblemon.moves.Fighting",
        "com.johnn.realtimecobblemon.moves.Legends",
        "com.johnn.realtimecobblemon.moves.MoveEngine",
        "com.johnn.realtimecobblemon.moves.Status",
        "com.johnn.realtimecobblemon.moves.Status$11",
        "com.johnn.realtimecobblemon.moves.Training",
        "com.johnn.realtimecobblemon.moves.Utility"
})
abstract class CommonTextSites {}
