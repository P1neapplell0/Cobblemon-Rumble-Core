package com.p1nero.cobblemon.rumblecore.client.ui.ore;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Shared Ore UI palette and pixel-depth controls. Foreign screens keep their own hit targets. */
public final class OreTheme {
    public static final int TEXT = 0xFFF7F7F7, MUTED = 0xFFBBBABF, GREEN = 0xFF8CDB55, COINS = 0xFFFFD76C;
    private static final int EDGE = 0xFF171719, PANEL = 0xFF37363B, HEADER = 0xFF4B4A50, WELL = 0xFF252529;

    private OreTheme() {}

    public static void panel(GuiGraphics g, int x, int y, int w, int h, int header) {
        g.fill(x + 1, y + 3, x + w + 2, y + h + 4, 0x990B0B0C);
        g.fill(x, y, x + w, y + h, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 2, PANEL);
        if (header > 1) g.fill(x + 1, y + 1, x + w - 1, y + header, HEADER);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF77767C);
    }

    public static void well(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, WELL);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, HEADER);
    }

    public static void row(GuiGraphics g, int x, int y, int w, int h, boolean selected, boolean hover) {
        g.fill(x, y, x + w, y + h, EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, selected ? 0xFF30532C : hover ? HEADER : PANEL);
        if (selected) g.fill(x + 1, y + 1, x + 3, y + h - 1, GREEN);
    }

    public static void button(GuiGraphics g, AbstractWidget widget, Component label, boolean primary, boolean enabled) {
        int x = widget.getX(), y = widget.getY(), w = widget.getWidth(), h = widget.getHeight();
        boolean focus = enabled && widget.isHoveredOrFocused();
        int fill = !enabled ? 0xFF454449 : primary ? (focus ? 0xFF4E8B37 : 0xFF3C782B) : focus ? 0xFF68676D : HEADER;
        g.fill(x, y, x + w, y + h, focus ? TEXT : EDGE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        if (h > 7) {
            g.fill(x + 1, y + 1, x + w - 1, y + 2, enabled ? (primary ? GREEN : 0xFF85848B) : HEADER);
            g.fill(x + 1, y + h - 3, x + w - 1, y + h - 1, primary && enabled ? 0xFF264D1C : WELL);
        }
        if (label != null) {
            Font font = Minecraft.getInstance().font;
            g.drawCenteredString(font, font.substrByWidth(label, Math.max(1, w - 4)).getString(), x + w / 2,
                    y + (h - 8) / 2, enabled ? TEXT : MUTED);
        }
    }

    public static Button action(int x, int y, int w, int h, Component label, boolean primary, Button.OnPress press) {
        return new Button(x, y, w, h, label, press, narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
                button(g, this, getMessage(), primary, active);
            }
        };
    }
}
