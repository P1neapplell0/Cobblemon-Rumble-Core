package com.p1nero.cobblemon.rumblecore.client.combat;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Matrix4fc;
import org.joml.Vector4d;

/** Render-thread projection snapshot, retained in double precision until GUI vertices are submitted. */
public final class ReticleProjection {
    private static final Matrix4d VIEW = new Matrix4d();
    private static final Matrix4d PROJECTION = new Matrix4d();
    private static final Vector4d CLIP = new Vector4d();
    private static double x = 0.5, y = 0.5;
    private static boolean visible;

    private ReticleProjection() {}

    public static void reset() { visible = false; x = y = 0.5; }
    public static double x() { return visible ? x : 0.5; }
    public static double y() { return visible ? y : 0.5; }
    public static boolean visible() { return visible; }

    public static void project(Vec3 camera, Matrix4fc view, Matrix4fc projection, Vec3 point) {
        // Subtract world coordinates before any float conversion, including far from world origin.
        CLIP.set(point.x - camera.x, point.y - camera.y, point.z - camera.z, 1);
        VIEW.set(view).transform(CLIP);
        PROJECTION.set(projection).transform(CLIP);
        visible = Double.isFinite(CLIP.w) && CLIP.w > 1.0e-6;
        if (!visible) return;
        x = (CLIP.x / CLIP.w + 1) * 0.5;
        y = (1 - CLIP.y / CLIP.w) * 0.5;
        visible = Double.isFinite(x) && Double.isFinite(y) && x > 0.02 && x < 0.98 && y > 0.02 && y < 0.98;
    }
}
