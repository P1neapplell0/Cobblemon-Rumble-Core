package com.p1nero.cobblemon.rumblecore.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

/** Physical-pixel edge coverage keeps tiny reticle movements visible at every GUI scale. */
public final class SubpixelReticleGraphics {
    // At most three bands per axis: fractional first pixel, opaque interior, fractional last pixel.
    private static final double[][] X_BANDS = new double[3][3];
    private static final double[][] Y_BANDS = new double[3][3];
    private SubpixelReticleGraphics() {}

    public static void fill(GuiGraphics graphics, int x1, int y1, int x2, int y2, int argb) {
        var matrix = graphics.pose().last().pose();
        // The normal HUD is unscaled in GUI space. Preserve other renderers' arbitrary transforms.
        if (matrix.m00() != 1 || matrix.m11() != 1 || matrix.m01() != 0 || matrix.m10() != 0) {
            graphics.fill(x1, y1, x2, y2, argb);
            return;
        }
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        double tx = matrix.m30(), ty = matrix.m31();
        int nx = bands((Math.min(x1, x2) + tx) * scale, (Math.max(x1, x2) + tx) * scale, X_BANDS);
        int ny = bands((Math.min(y1, y2) + ty) * scale, (Math.max(y1, y2) + ty) * scale, Y_BANDS);
        var vertices = graphics.bufferSource().getBuffer(RenderType.gui());
        for (int i = 0; i < nx; i++) for (int j = 0; j < ny; j++) {
            double[] xb = X_BANDS[i], yb = Y_BANDS[j];
            int alpha = (int) Math.round((argb >>> 24) * xb[2] * yb[2]);
            if (alpha == 0) continue;
            int color = argb & 0xFFFFFF | alpha << 24;
            float left = (float) (xb[0] / scale - tx), right = (float) (xb[1] / scale - tx);
            float top = (float) (yb[0] / scale - ty), bottom = (float) (yb[1] / scale - ty);
            vertices.addVertex(matrix, left, top, 0).setColor(color);
            vertices.addVertex(matrix, left, bottom, 0).setColor(color);
            vertices.addVertex(matrix, right, bottom, 0).setColor(color);
            vertices.addVertex(matrix, right, top, 0).setColor(color);
        }
    }

    static int bands(double from, double to, double[][] result) {
        if (to <= from) return 0;
        double first = Math.floor(from), last = Math.ceil(to) - 1;
        if (first == last) { set(result[0], first, first + 1, to - from); return 1; }
        int count = 0;
        double start = Math.ceil(from), end = Math.floor(to);
        if (start > from) set(result[count++], first, start, start - from);
        if (end > start) set(result[count++], start, end, 1);
        if (to > end) set(result[count++], end, end + 1, to - end);
        return count;
    }

    private static void set(double[] band, double from, double to, double coverage) {
        band[0] = from; band[1] = to; band[2] = coverage;
    }
}
