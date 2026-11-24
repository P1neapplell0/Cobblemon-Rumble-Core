package com.p1nero.cobblemon.rumblecore.client.ui.ore;

import com.p1nero.cobblemon.rumblecore.gym.LegendaryTrials;
import com.p1nero.cobblemon.rumblecore.gym.LegendaryTrials.Trial;
import com.p1nero.cobblemon.rumblecore.prologue.MerchantPanels;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Region-filtered encounter grid with the selected commission and its full roster alongside it. */
public final class LegendaryDirectoryScreen extends Screen {
    private static final String KEY = "rumble.legendary.";
    private static final int REGION_COUNT = 10, OPTION_HEIGHT = 15;
    private final List<Entry> entries;
    private final TrialPortraits portraits = new TrialPortraits();
    private final List<Card> cards = new ArrayList<>();
    private List<Entry> filtered;
    private Entry selected;
    private Button regionButton, previous, next, challenge;
    private EditBox search;
    private String query = "";
    private int region, page, x, y, w, h, details, columns, pageSize;
    private List<Component> detailHover;
    private boolean regionOpen;
    private int regionCursor;

    private record Entry(Trial trial, byte state) {
        Component title() { return Component.translatable(trial.titleKey()); }
    }

    private LegendaryDirectoryScreen(List<Entry> entries) {
        super(text("title"));
        this.entries = entries;
        this.filtered = entries;
        if (!entries.isEmpty()) selected = entries.getFirst();
    }

    public static void open(CompoundTag data) {
        List<Entry> entries = new ArrayList<>();
        var rows = data.getList("rows", Tag.TAG_COMPOUND);
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.getCompound(i);
            var trial = LegendaryTrials.get(row.getString("id"));
            if (trial != null) entries.add(new Entry(trial, row.getByte("state")));
        }
        Minecraft.getInstance().setScreen(new LegendaryDirectoryScreen(List.copyOf(entries)));
    }

    @Override protected void init() {
        w = Math.min(466, width - 12);
        h = Math.min(276, height - 12);
        x = (width - w) / 2;
        y = (height - h) / 2;
        int content = x + 8, cellTop = y + 51, cellWidth;
        details = x + w * 59 / 100;
        columns = w >= 420 ? 3 : 2;
        cellWidth = (details - content - 7) / columns;
        int rows = Math.max(1, (h - 82) / 46);
        int cellHeight = Math.max(32, (h - 82) / rows);
        pageSize = columns * rows;
        regionOpen = false;
        regionButton = addRenderableWidget(OreTheme.action(details, y + 25, x + w - details - 8, 19,
                regionLabel(), false, b -> { regionOpen = true; regionCursor = region; }));
        search = new EditBox(font, content, y + 25, details - content - 10, 19, text("search"));
        search.setHint(text("search"));
        search.setMaxLength(40);
        search.setValue(query);
        search.setResponder(value -> { query = value; filter(); });
        addRenderableWidget(search);
        cards.clear();
        for (int i = 0; i < pageSize; i++) {
            var tile = new Card(content + (i % columns) * cellWidth, cellTop + (i / columns) * cellHeight, cellWidth - 3, cellHeight - 3);
            cards.add(addRenderableWidget(tile));
        }
        int bottom = y + h - 23;
        previous = addRenderableWidget(OreTheme.action(content, bottom, 24, 17, Component.literal("‹"), false, b -> page(-1)));
        next = addRenderableWidget(OreTheme.action(details - 34, bottom, 24, 17, Component.literal("›"), false, b -> page(1)));
        challenge = addRenderableWidget(OreTheme.action(details, bottom, x + w - details - 8, 17, text("take"), true, b -> {
            if (selected != null && selected.state() >= 0) PacketDistributor.sendToServer(new MerchantPanels.Select(selected.trial().id()));
        }));
        populate();
    }

    private void filter() {
        String value = query.strip().toLowerCase(Locale.ROOT);
        filtered = entries.stream().filter(e -> region == 0 || e.trial().generation() == region)
                .filter(e -> value.isEmpty() || e.title().getString().toLowerCase(Locale.ROOT).contains(value)
                        || e.trial().foe().species().contains(value)).toList();
        page = 0;
        if (!filtered.contains(selected)) selected = filtered.isEmpty() ? null : filtered.getFirst();
        populate();
    }

    private int pages() { return Math.max(1, (filtered.size() + pageSize - 1) / pageSize); }
    private void page(int change) {
        int nextPage = Mth.clamp(page + change, 0, pages() - 1);
        if (nextPage == page) return;
        page = nextPage;
        selected = filtered.get(page * pageSize);
        populate();
    }

    private void populate() {
        page = Mth.clamp(page, 0, pages() - 1);
        for (int i = 0; i < cards.size(); i++) {
            int slot = page * pageSize + i;
            cards.get(i).bind(slot < filtered.size() ? filtered.get(slot) : null);
        }
        previous.active = page > 0;
        next.active = page + 1 < pages();
        refreshSelection();
    }

    private void refreshSelection() {
        challenge.active = selected != null && selected.state() >= 0;
        challenge.setMessage(text(selected == null ? "take" : switch (selected.state()) {
            case -1 -> "unavailable";
            case 1 -> "resume";
            case 2 -> "replay";
            default -> "take";
        }));
    }

    @Override public void render(GuiGraphics g, int mx, int my, float tick) {
        g.fill(0, 0, width, height, 0xB0161618);
        OreTheme.panel(g, x, y, w, h, 20);
        g.drawCenteredString(font, title, width / 2, y + 6, OreTheme.TEXT);
        g.drawCenteredString(font, Component.literal((page + 1) + " / " + pages()), (x + details) / 2, y + h - 19, OreTheme.MUTED);
        int detailW = x + w - details - 8, detailH = h - 80;
        OreTheme.well(g, details, y + 51, detailW, detailH);
        detailHover = null;
        if (selected == null) g.drawCenteredString(font, text("empty"), (x + details) / 2, y + 65, OreTheme.MUTED);
        else renderDetails(g, detailW, detailH, mx, my, tick);
        super.render(g, mx, my, tick);
        if (regionOpen) renderRegions(g, mx, my);
        else if (detailHover != null) g.renderComponentTooltip(font, detailHover, mx, my);
    }

    private Component regionLabel() { return text("region." + region).copy().append("  ▾"); }

    private int popupTop() {
        return Math.min(regionButton.getY() + regionButton.getHeight(), height - REGION_COUNT * OPTION_HEIGHT - 4);
    }

    private void selectRegion(int index) {
        regionOpen = false;
        if (region == index) return;
        region = index;
        regionButton.setMessage(regionLabel());
        filter();
    }

    private void renderRegions(GuiGraphics g, int mx, int my) {
        int left = regionButton.getX(), top = popupTop(), span = regionButton.getWidth();
        g.pose().pushPose();
        try {
            g.pose().translate(0, 0, 400);
            for (int i = 0; i < REGION_COUNT; i++) {
                int row = top + i * OPTION_HEIGHT;
                boolean hover = i == regionCursor || (mx >= left && mx < left + span && my >= row && my < row + OPTION_HEIGHT);
                OreTheme.row(g, left, row, span, OPTION_HEIGHT, i == region, hover);
                g.drawString(font, font.substrByWidth(text("region." + i), span - 12).getString(),
                        left + 6, row + 4, OreTheme.TEXT, false);
            }
        } finally { g.pose().popPose(); }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (!regionOpen) return super.mouseClicked(mx, my, button);
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        int top = popupTop();
        if (mx >= regionButton.getX() && mx < regionButton.getX() + regionButton.getWidth()
                && my >= top && my < top + REGION_COUNT * OPTION_HEIGHT) {
            selectRegion((int) ((my - top) / OPTION_HEIGHT));
        } else regionOpen = false;
        return true;
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!regionOpen) return super.keyPressed(key, scanCode, modifiers);
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_TAB -> regionOpen = false;
            case GLFW.GLFW_KEY_UP -> regionCursor = Math.floorMod(regionCursor - 1, REGION_COUNT);
            case GLFW.GLFW_KEY_DOWN -> regionCursor = (regionCursor + 1) % REGION_COUNT;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> selectRegion(regionCursor);
        }
        return true;
    }

    @Override public boolean charTyped(char character, int modifiers) {
        return regionOpen || super.charTyped(character, modifiers);
    }

    private void renderDetails(GuiGraphics g, int detailW, int detailH, int mx, int my, float tick) {
        int dx = details + 6, dy = y + 57;
        var title = selected.title();
        g.drawString(font, font.substrByWidth(title, detailW - 12).getString(), dx, dy, OreTheme.TEXT, false);
        var foe = selected.trial().foe();
        g.drawString(font, text("boss_stats"), dx, dy + 12, OreTheme.COINS, false);
        int size = Math.min(54, Math.min(detailW - 17, detailH - 41));
        int px = dx + (detailW - 12 - size) / 2, py = dy + 27;
        if (size >= 12) {
            portraits.render(g, foe.speciesId(), foe.aspects(), px, py, size, tick);
            g.drawCenteredString(font, text("level", foe.level()), dx + (detailW - 12) / 2, py + size, OreTheme.MUTED);
            if (mx >= px && mx < px + size && my >= py && my < py + size + 10) {
                var tooltip = new ArrayList<Component>();
                tooltip.add(selected.title().copy().append(" · ").append(text("level", foe.level())));
                foe.moves().forEach(move -> tooltip.add(Component.translatable("cobblemon.move." + move)));
                detailHover = tooltip;
            }
        }
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (regionOpen) return true;
        if (mx >= x && mx < details && my >= y + 46 && my < y + h && vertical != 0) {
            page(vertical > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float tick) { }

    private static Component text(String key, Object... args) { return Component.translatable(KEY + key, args); }

    private final class Card extends Button {
        private Entry entry;
        Card(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), button -> {
                selected = ((Card) button).entry;
                refreshSelection();
            }, DEFAULT_NARRATION);
        }
        void bind(Entry value) {
            entry = value;
            visible = active = value != null;
            if (value == null) return;
            setMessage(value.title());
            setTooltip(Tooltip.create(value.title()));
        }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float tick) {
            if (entry == null) return;
            OreTheme.row(g, getX(), getY(), width, height, entry.equals(selected), isHoveredOrFocused());
            var lead = entry.trial().foe();
            int size = Math.min(height - 11, 37);
            portraits.render(g, lead.speciesId(), lead.aspects(), getX() + (width - size) / 2, getY() + 1, size, tick);
            g.drawCenteredString(font, font.substrByWidth(entry.title(), width - 6).getString(), getX() + width / 2,
                    getY() + height - 10, entry.state() < 0 ? OreTheme.MUTED : OreTheme.TEXT);
            if (entry.state() > 0) g.fill(getX() + width - 9, getY() + 4, getX() + width - 5, getY() + 8,
                    entry.state() == 2 ? OreTheme.GREEN : OreTheme.COINS);
        }
    }
}
