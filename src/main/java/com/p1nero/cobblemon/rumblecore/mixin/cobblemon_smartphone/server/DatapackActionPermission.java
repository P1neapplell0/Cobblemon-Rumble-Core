package com.p1nero.cobblemon.rumblecore.mixin.cobblemon_smartphone.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nbp.cobblemon_smartphone.api.DatapackActionFunction;
import com.nbp.cobblemon_smartphone.network.handler.ExecuteDatapackActionHandler;
import com.p1nero.cobblemon.rumblecore.phone.PhoneApp;
import com.p1nero.cobblemon.rumblecore.phone.PhoneState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ExecuteDatapackActionHandler.class, remap = false)
abstract class DatapackActionPermission {
    // Datapack functions call the native implementations without going through their packet handlers.
    @WrapOperation(method = "handle$lambda$2", at = @At(value = "INVOKE", target = "Lcom/nbp/cobblemon_smartphone/api/DatapackActionFunction;execute(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V"))
    private static void rumble$requireApp(DatapackActionFunction function, MinecraftServer server, ServerPlayer player, Operation<Void> original) {
        PhoneApp app = switch (function) {
            case OPEN_PC -> PhoneApp.PC;
            case HEAL_PARTY -> PhoneApp.HEALING;
            case OPEN_POKEDEX -> PhoneApp.POKEDEX;
            case OPEN_ENDER_CHEST -> PhoneApp.CLOUD;
            case OPEN_POKENAV -> PhoneApp.POKEMON_TRACKER;
            case OPEN_WAYSTONE -> PhoneApp.WAYSTONE;
            default -> null;
        };
        if (app == null || PhoneState.allow(player, app)) original.call(function, server, player);
    }
}
