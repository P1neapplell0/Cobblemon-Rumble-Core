package com.p1nero.cobblemon.rumblecore.mixin.rct;

import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TrainerMob.class, remap = false)
public interface TrainerAppearanceAccess {
    @Accessor("DATA_TRAINER_ID")
    static EntityDataAccessor<String> rumble$trainerId() {
        throw new AssertionError();
    }
}
