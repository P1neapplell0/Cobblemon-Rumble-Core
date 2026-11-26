package com.p1nero.cobblemon.rumblecore.combat;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public final class RealtimeSoundDistance {
    private RealtimeSoundDistance() {}

    public static double squared(AABB listener, double x, double z) {
        double dx = x - Mth.clamp(x, listener.minX, listener.maxX);
        double dz = z - Mth.clamp(z, listener.minZ, listener.maxZ);
        return dx * dx + dz * dz;
    }

    public static double forPlayer(Entity listener, double x, double y, double z) {
        return squared(listener.getBoundingBox(), x, z) +
                Math.pow(y - Mth.clamp(y, listener.getBoundingBox().minY, listener.getBoundingBox().maxY), 2);
    }
}
