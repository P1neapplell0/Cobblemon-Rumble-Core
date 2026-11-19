package com.p1nero.cobblemon.rumblecore.mixin.rct;

import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = TrainerMob.class, remap = false)
public interface TrainerOpponent {
    @Invoker("setOpponent")
    void rumble$setOpponent(Player player);
}
