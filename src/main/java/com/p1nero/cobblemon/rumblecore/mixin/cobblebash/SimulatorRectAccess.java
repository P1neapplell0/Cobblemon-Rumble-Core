package com.p1nero.cobblemon.rumblecore.mixin.cobblebash;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.p1nero.cobblebashscreen.simulator.TrainingSimulatorScreen$Rect", remap = false)
public interface SimulatorRectAccess {
    @Accessor("x") int rumble$x();
    @Accessor("y") int rumble$y();
    @Accessor("width") int rumble$width();
    @Accessor("height") int rumble$height();
}
