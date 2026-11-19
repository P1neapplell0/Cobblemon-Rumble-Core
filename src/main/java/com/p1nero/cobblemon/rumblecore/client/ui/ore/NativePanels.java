package com.p1nero.cobblemon.rumblecore.client.ui.ore;

import com.p1nero.cobblemon.rumblecore.shop.ProfessorCatalogue;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

/** Layout adapters for native menu coordinates; styling does not own any gameplay or input. */
public final class NativePanels {
    public static final int WAYSTONE_TITLE_Y = 7;
    public static final int WAYSTONE_HEADER_LINE_SPACING = 15;
    public static final int WAYSTONE_SIDE_INSET = 2;

    private NativePanels() {}

    public static Component categoryLabel(String name) {
        return name != null && (name.startsWith(ProfessorCatalogue.CATEGORY) || name.equals("rumble.merchant.category")) ? Component.translatable(name) : Component.nullToEmpty(name);
    }

    public static void shop(GuiGraphics g, Font font, int x, int y, int w, int h, List<Slot> slots) {
        OreTheme.panel(g, x, y, w, h, 17);
        g.drawString(font, Component.translatable("rumble.bank_prices.buy"), x + 6, y + 5, OreTheme.TEXT, false);
        OreTheme.well(g, x + 94, y + 2, 75, 13);
        OreTheme.well(g, x + 172, y + 2, w - 175, 13);
        OreTheme.well(g, x + 94, y + 17, 75, 76);
        OreTheme.well(g, x + 172, y + 17, w - 175, h - 21);
        OreTheme.well(g, x + 2, y + 38, 36, 36);
        OreTheme.well(g, x + 35, y + 61, 28, 12);
        g.drawString(font, Component.translatable("container.inventory"), x + 5, y + 86, OreTheme.MUTED, false);
        slots(g, x, y, slots);
    }

    public static void bank(GuiGraphics g, Font font, int x, int y, int w, int h, List<Slot> slots) {
        OreTheme.panel(g, x, y, w, h, 17);
        g.drawString(font, Component.translatable("container.inventory"), x + 5, y + 95, OreTheme.MUTED, false);
        slots(g, x, y, slots);
    }

    public static void waystones(GuiGraphics g, int x, int y, int w, int h) {
        OreTheme.panel(g, x, y, w, h, 36);
        OreTheme.well(g, x + 24, y + 62, w - 46, h - 87);
    }

    private static void slots(GuiGraphics g, int x, int y, List<Slot> slots) {
        for (var slot : slots) OreTheme.well(g, x + slot.x - 1, y + slot.y - 1, 18, 18);
    }
}
