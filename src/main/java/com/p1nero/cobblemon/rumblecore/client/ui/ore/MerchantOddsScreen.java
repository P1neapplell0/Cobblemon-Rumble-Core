package com.p1nero.cobblemon.rumblecore.client.ui.ore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class MerchantOddsScreen extends Screen {
    private static final String KEY = "rumble.merchant.odds.", SEARCH = "rumble.legendary.search";
    private final List<Entry> entries;
    private final NumberFormat coins = NumberFormat.getIntegerInstance();
    private final NumberFormat chance = NumberFormat.getPercentInstance();
    private List<Entry> shown;
    private Button up, down;
    private String query = "";
    private int x, y, w, h, rows, offset;
    private static final int ROW = 20;
    private record Entry(ItemStack item, int price, double odds) {}

    private MerchantOddsScreen(List<Entry> entries) {
        super(Component.translatable(KEY + "title"));
        this.entries = entries;
        this.shown = entries;
        chance.setMaximumFractionDigits(2);
    }

    public static void open(CompoundTag data) {
        var entries = new ArrayList<Entry>();
        var rows = data.getList("rows", Tag.TAG_COMPOUND);
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.getCompound(i);
            var id = ResourceLocation.tryParse(row.getString("item"));
            if (id != null) BuiltInRegistries.ITEM.getOptional(id).ifPresent(item ->
                    entries.add(new Entry(item.getDefaultInstance(), row.getInt("price"), row.getDouble("fraction"))));
        }
        Minecraft.getInstance().setScreen(new MerchantOddsScreen(List.copyOf(entries)));
    }

    @Override protected void init() {
        w = Math.min(396, width - 12);
        h = Math.min(310, height - 12);
        x = (width - w) / 2;
        y = (height - h) / 2;
        rows = Math.max(1, (h - 85) / ROW);
        var field = new EditBox(font, x + 8, y + 23, w - 16, 17, Component.translatable(SEARCH));
        field.setHint(Component.translatable(SEARCH));
        field.setMaxLength(40);
        field.setValue(query);
        field.setResponder(value -> {
            query = value;
            var needle = value.strip().toLowerCase(Locale.ROOT);
            shown = entries.stream().filter(e -> e.item().getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle)).toList();
            scroll(-offset);
        });
        addRenderableWidget(field);
        int footer = y + h - 23;
        addRenderableWidget(OreTheme.action(x + 8, footer, 72, 16, Component.translatable("gui.back"), false, b -> onClose()));
        up = addRenderableWidget(OreTheme.action(x + w - 58, footer, 22, 16, Component.literal("↑"), false, b -> scroll(-rows)));
        down = addRenderableWidget(OreTheme.action(x + w - 32, footer, 22, 16, Component.literal("↓"), false, b -> scroll(rows)));
        scroll(0);
    }

    private void scroll(int delta) {
        int max = Math.max(0, shown.size() - rows);
        offset = Mth.clamp(offset + delta, 0, max);
        up.active = offset > 0;
        down.active = offset < max;
    }

    @Override public void render(GuiGraphics g, int mx, int my, float tick) {
        g.fill(0, 0, width, height, 0xB0161618);
        OreTheme.panel(g, x, y, w, h, 19);
        g.drawCenteredString(font, title, width / 2, y + 5, OreTheme.TEXT);
        int itemX = x + 30, priceX = x + w - 105, chanceX = x + w - 49;
        g.drawString(font, Component.translatable(KEY + "item"), itemX, y + 45, OreTheme.MUTED, false);
        g.drawCenteredString(font, Component.translatable(KEY + "price"), priceX, y + 45, OreTheme.MUTED);
        g.drawCenteredString(font, Component.translatable(KEY + "chance"), chanceX, y + 45, OreTheme.MUTED);
        ItemStack hover = null;
        for (int i = 0; i < rows && offset + i < shown.size(); i++) {
            var entry = shown.get(offset + i);
            int rowY = y + 56 + i * ROW;
            boolean over = mx >= x + 8 && mx < x + w - 8 && my >= rowY && my < rowY + ROW;
            OreTheme.row(g, x + 8, rowY, w - 16, ROW - 1, false, over);
            g.renderItem(entry.item(), x + 10, rowY + 1);
            g.drawString(font, font.substrByWidth(entry.item().getHoverName(), Math.max(6, priceX - 29 - itemX)).getString(), itemX, rowY + 6, OreTheme.TEXT, false);
            g.drawCenteredString(font, coins.format(entry.price()), priceX, rowY + 6, OreTheme.COINS);
            g.drawCenteredString(font, chance.format(entry.odds()), chanceX, rowY + 6, entry.odds() == 1 ? OreTheme.GREEN : OreTheme.TEXT);
            if (over) hover = entry.item();
        }
        if (shown.isEmpty()) g.drawCenteredString(font, Component.translatable("rumble.legendary.empty"), width / 2, y + 60, OreTheme.MUTED);
        super.render(g, mx, my, tick);
        if (hover != null) g.renderTooltip(font, hover, mx, my);
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (mx >= x && mx < x + w && my >= y + 55 && my < y + h - 25 && vertical != 0) { scroll(vertical > 0 ? -1 : 1); return true; }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float tick) { }
    @Override public boolean isPauseScreen() { return false; }
}
