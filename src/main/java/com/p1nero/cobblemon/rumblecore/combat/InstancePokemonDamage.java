package com.p1nero.cobblemon.rumblecore.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.Smash;
import com.p1nero.misc.custom_instances.api.InstanceApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

/** Bridges modded damage sources without reporting vanilla damage a second time. */
public final class InstancePokemonDamage {
    private InstancePokemonDamage() {}

    public static void initialize() { InstanceApi.registerDamageOwnerResolver(InstancePokemonDamage::owner); }

    private static ServerPlayer owner(ServerLevel level, DamageSource source) {
        ServerPlayer owner = owner(level, source.getEntity());
        return owner != null ? owner : owner(level, source.getDirectEntity());
    }

    private static ServerPlayer owner(ServerLevel level, Entity entity) {
        if (entity instanceof Projectile projectile) entity = projectile.getOwner();
        if (!(entity instanceof PokemonEntity pokemon) || pokemon.level() != level) return null;
        if (pokemon.getOwner() instanceof ServerPlayer player) return player;
        // Detached competition teams have no Cobblemon player owner. RTC still knows who controls them.
        for (var session : Smash.sessions()) {
            if (session.player.serverLevel() == level && (session.pokemon == pokemon || session.combat.decoys.contains(pokemon))) {
                return session.player;
            }
        }
        return null;
    }
}
