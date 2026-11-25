package com.p1nero.cobblemon.rumblecore.item;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.moves.Sounds;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.reward.WildCapture;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ThrownPokeBrick extends ThrowableItemProjectile {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, CobblemonRumbleCore.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownPokeBrick>> TYPE =
            TYPES.register("thrown_pokebrick", () -> EntityType.Builder.<ThrownPokeBrick>of(ThrownPokeBrick::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(2).noSave()
                    .build(CobblemonRumbleCore.MOD_ID + ":thrown_pokebrick"));

    private ThrownPokeBrick(EntityType<? extends ThrownPokeBrick> type, Level level) {
        super(type, level);
    }

    ThrownPokeBrick(ServerLevel level, ServerPlayer owner) {
        super(TYPE.get(), owner, level);
    }

    static void register(IEventBus bus) {
        TYPES.register(bus);
    }

    @Override protected Item getDefaultItem() { return PokeBrick.ITEM.get(); }
    @Override protected double getDefaultGravity() { return 0.06; }

    @Override
    public void tick() {
        if (!level().isClientSide() && tickCount >= 100) {
            discard();
            return;
        }
        super.tick();
    }

    @Override
    protected void onHit(HitResult hit) {
        if (!(level() instanceof ServerLevel level) || isRemoved()) return;
        var impact = hit.getLocation();
        var bricks = Blocks.BRICKS.defaultBlockState();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, bricks),
                impact.x, impact.y, impact.z, 20, 0.14, 0.14, 0.14, 0.13);
        level.playSound(null, impact.x, impact.y, impact.z, bricks.getSoundType().getBreakSound(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PokemonEntity pokemon) {
            Sounds.play(level, impact, "punch", 0.85F, 1.0F);
            if (getOwner() instanceof ServerPlayer player && !WildCapture.tryBrickStun(player, pokemon)
                    && player.level() == level) {
                player.displayClientMessage(Component.translatable("rumble.combat.brick_stun_blocked"), true);
            }
        }
        discard();
    }
}
