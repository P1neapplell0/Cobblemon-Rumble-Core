package com.p1nero.cobblemon.rumblecore.client.ui.ore;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;

/** Two expanding echoes around an active tutorial target; no change to its hit box. */
public final class TutorialPulse {
    private TutorialPulse() {}

    public static void render(GuiGraphics graphics, int x, int y, int width, int height) {
        double clock = (Util.getMillis() % 1600L) / 1600.0;
        graphics.renderOutline(x - 1, y - 1, width + 2, height + 2, 0xFFFFE29C);
        for (int i = 0; i < 2; i++) {
            double phase = (clock + i * .5) % 1.0;
            int pad = 2 + (int) (phase * 7);
            int alpha = (int) (190 * (1 - phase) * (1 - phase));
            graphics.renderOutline(x - pad, y - pad, width + 2 * pad, height + 2 * pad, (alpha << 24) | 0xFFE29C);
        }
    }
}
