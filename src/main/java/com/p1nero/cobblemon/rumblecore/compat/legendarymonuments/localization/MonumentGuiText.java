package com.p1nero.cobblemon.rumblecore.compat.legendarymonuments.localization;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** String-only drawing calls in Legendary Monuments' screens. */
public final class MonumentGuiText {
    private MonumentGuiText() {}

    public static int drawString(GuiGraphics gui, Font font, String text, int x, int y, int color) {
        return drawString(gui, font, text, x, y, color, true);
    }

    public static int drawString(GuiGraphics gui, Font font, String text, int x, int y, int color, boolean shadow) {
        var translated = MonumentText.translationOrNull(text);
        return translated == null ? gui.drawString(font, text, x, y, color, shadow)
                : gui.drawString(font, translated, x, y, color, shadow);
    }

    public static void drawCenteredString(GuiGraphics gui, Font font, String text, int x, int y, int color) {
        var translated = MonumentText.translationOrNull(text);
        if (translated == null) gui.drawCenteredString(font, text, x, y, color);
        else gui.drawCenteredString(font, translated, x, y, color);
    }

    public static int width(Font font, String text) {
        var translated = MonumentText.translationOrNull(text);
        return translated == null ? font.width(text) : font.width(translated);
    }
}
