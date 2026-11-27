package com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.johnn.realtimecobblemon.moves.Brawl$Foe", remap = false)
public interface BrawlFoeAccess {
    // RTC 1.0.3alpha.1: current player target, used to collect wild cutscene opponents.
    @Accessor("player") net.minecraft.server.level.ServerPlayer rumble$player();
    @Accessor("mon") PokemonEntity rumble$mon();
    @Accessor("windup") int rumble$windup();
    @Accessor("actAt") long rumble$actAt();
    @Accessor("moves") List<?> rumble$moves();
}
