package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.johnn.realtimecobblemon.Smash;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeSoundDistance;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** RTC 1.0.3alpha.1 Sounds.play emits a positional vanilla packet; keep NeoForge's sound event and replace only its recipient test. */
@Mixin(ServerLevel.class)
abstract class RealtimeBattleSoundRange {
    @WrapOperation(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcast(Lnet/minecraft/world/entity/player/Player;DDDDLnet/minecraft/resources/ResourceKey;Lnet/minecraft/network/protocol/Packet;)V"))
    private void rumble$combatRecipients(PlayerList players, Player excluded, double x, double y, double z, double range,
                                        ResourceKey<Level> dimension, Packet<?> packet, Operation<Void> original) {
        if (!(packet instanceof ClientboundSoundPacket sound)
                || !"realtimecobblemon".equals(sound.getSound().value().getLocation().getNamespace())) {
            original.call(players, excluded, x, y, z, range, dimension, packet);
            return;
        }
        var level = (ServerLevel) (Object) this;
        for (var player : level.players()) {
            if (player == excluded) continue;
            var session = Smash.session(player);
            var pokemon = session == null ? null : session.pokemon;
            double distance = pokemon != null && pokemon.level() == level && !pokemon.isRemoved()
                    ? RealtimeSoundDistance.squared(pokemon.getBoundingBox(), x, z)
                    : player.distanceToSqr(x, y, z);
            if (distance < range * range) player.connection.send(packet);
        }
    }
}
