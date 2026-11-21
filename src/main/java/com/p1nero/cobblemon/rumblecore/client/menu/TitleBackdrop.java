package com.p1nero.cobblemon.rumblecore.client.menu;

import net.minecraft.client.gui.GuiGraphics;

/** The preblurred arena plate shared with the early loader. */
public final class TitleBackdrop {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private TitleBackdrop() {}

    public static void render(GuiGraphics graphics) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float scale = Math.max(width / (float) WIDTH, height / (float) HEIGHT);
        int w = (int) Math.ceil(WIDTH * scale);
        int h = (int) Math.ceil(HEIGHT * scale);
        RumbleTitleScreen.image(graphics, "arena-blur", (width - w) / 2, (height - h) / 2, w, h, WIDTH, HEIGHT);
    }

    public static void shade(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0x60050912);
    }
}
