package com.p1nero.cobblemon.rumblecore.mixin.legendarymonuments;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.jorgaomc.legendarymonuments.entities.CalyrexNpcManager$Option", remap = false)
public interface CalyrexOptionAccess {
    @Accessor("id") String rumble$optionId();
    @Accessor("label") String rumble$optionLabel();
}
