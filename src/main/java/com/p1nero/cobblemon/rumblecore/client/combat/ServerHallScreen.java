package com.p1nero.cobblemon.rumblecore.client.combat;

import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import com.p1nero.cobblemon.rumblecore.combat.arena.ServerHall;
import com.p1nero.misc.custom_instances.client.widget.PlayerPortraits;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ServerHallScreen extends Screen {
    private static final int HEADER = 64, ROW = 16, FOOTER = 32;
    private final Screen parent;
    private final PlayerPortraits portraits = new PlayerPortraits();
    private List<ServerHall.Row> entries = List.of();
    private Button previous, next, championsTab, pvpTab;
    private Button[] rosterButtons;
    private boolean pvp;
    private UUID request;
    private int x, y, panelWidth, panelHeight, pageSize, page, total, waitTicks;
    private boolean failed;

    public ServerHallScreen(Screen parent) {
        super(Component.translatable("rumble.menu.server_hall"));
        this.parent = parent;
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ServerHallScreen(null));
    }

    @Override protected void init() {
        int previousPageSize = pageSize;
        int offset = page * pageSize;
        panelWidth = Math.min(320, width - 16);
        pageSize = Math.clamp((height - HEADER - FOOTER - 16) / ROW, 1, ServerHall.MAX_ROWS);
        panelHeight = HEADER + pageSize * ROW + FOOTER;
        x = (width - panelWidth) / 2;
        y = (height - panelHeight) / 2;
        int bottom = y + panelHeight - 24;
        var back = Component.translatable("gui.back");
        int backWidth = Math.min(panelWidth / 3, Math.max(42, font.width(back) + 10));
        addRenderableWidget(OreTheme.action(x + 8, bottom, backWidth, 16, back, false, b -> onClose()));
        previous = addRenderableWidget(arrow(x + panelWidth - 72, bottom, "<", "previous", -1));
        next = addRenderableWidget(arrow(x + panelWidth - 36, bottom, ">", "next", 1));
        int tabWidth = (panelWidth - 20) / 2;
        championsTab = addRenderableWidget(OreTheme.action(x + 8, y + 25, tabWidth, 17,
                text("champions"), false, b -> select(false)));
        pvpTab = addRenderableWidget(OreTheme.action(x + 12 + tabWidth, y + 25, tabWidth, 17,
                text("pvp"), false, b -> select(true)));
        rosterButtons = new Button[pageSize];
        var rosterLabel = text("view_roster");
        int actionWidth = Math.max(52, font.width(rosterLabel) + 8);
        for (int i = 0; i < pageSize; i++) {
            int index = i;
            rosterButtons[i] = addRenderableWidget(OreTheme.action(x + panelWidth - 14 - actionWidth,
                    y + HEADER + i * ROW + 1, actionWidth, ROW - 2, rosterLabel, false, b -> openRoster(index)));
        }
        if (request != null || failed || previousPageSize != pageSize) requestPage(offset / pageSize);
        else updateButtons();
    }

    private Button arrow(int left, int top, String glyph, String key, int direction) {
        var button = OreTheme.action(left, top, 28, 16, Component.literal(glyph), false, b -> requestPage(page + direction));
        button.setTooltip(Tooltip.create(text(key)));
        return button;
    }

    private void select(boolean showPvp) {
        pvp = showPvp;
        page = 0;
        requestPage(0);
    }

    private void requestPage(int target) {
        request = UUID.randomUUID();
        waitTicks = 200;
        failed = false;
        entries = List.of();
        updateButtons();
        PacketDistributor.sendToServer(new ServerHall.Request(request, target, pageSize, pvp));
    }

    public static void receive(ServerHall.Page packet) {
        if (Minecraft.getInstance().screen instanceof ServerHallScreen screen && packet.token().equals(screen.request)) {
            screen.request = null;
            screen.failed = packet.total() < 0;
            screen.page = packet.start() / screen.pageSize;
            screen.total = packet.total();
            screen.entries = packet.rows();
            screen.updateButtons();
        }
    }

    private void updateButtons() {
        championsTab.active = pvp;
        pvpTab.active = !pvp;
        previous.active = request == null && !failed && page > 0;
        next.active = request == null && !failed && (long) (page + 1) * pageSize < total;
        for (int i = 0; i < rosterButtons.length; i++) {
            var button = rosterButtons[i];
            button.visible = !pvp && i < entries.size();
            button.active = button.visible && !entries.get(i).roster().isEmpty();
            button.setTooltip(button.visible && !button.active ? Tooltip.create(text("no_roster")) : null);
        }
    }

    private void openRoster(int index) {
        var entry = entries.get(index);
        minecraft.setScreen(HallOfFameScreen.details(this, entry.roster()));
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0 || pvp || mouseX < x + 9 || mouseX >= x + panelWidth - 9 || mouseY < y + HEADER) return false;
        int index = (int) (mouseY - y - HEADER) / ROW;
        if (index >= entries.size() || !rosterButtons[index].active) return false;
        rosterButtons[index].playDownSound(minecraft.getSoundManager());
        openRoster(index);
        return true;
    }

    @Override public void tick() {
        if (request != null && --waitTicks <= 0) {
            request = null;
            failed = true;
            updateButtons();
        }
    }

    // Unpause the integrated server so it can answer the request.
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
        super.renderBackground(g, mx, my, delta);
        OreTheme.panel(g, x, y, panelWidth, panelHeight, 24);
        g.drawCenteredString(font, title, width / 2, y + 8, OreTheme.COINS);
        OreTheme.well(g, x + 8, y + 47, panelWidth - 16, HEADER - 43 + pageSize * ROW);
        int timeRight = pvp ? x + panelWidth - 14 : rosterButtons[0].getX() - 8;
        g.drawString(font, text(pvp ? "pvp_player" : "player"), x + 14, y + 53, OreTheme.MUTED);
        var best = text(pvp ? "losses" : "best_time");
        int winsRight = timeRight - Math.max(41, font.width(best) + 10);
        if (pvp) {
            var wins = text("wins");
            g.drawString(font, wins, winsRight - font.width(wins), y + 53, OreTheme.MUTED);
        }
        g.drawString(font, best, timeRight - font.width(best), y + 53, OreTheme.MUTED);

        for (int i = 0; i < entries.size(); i++) {
            int top = y + HEADER + i * ROW;
            var entry = entries.get(i);
            int rank = page * pageSize + i + 1;
            OreTheme.row(g, x + 9, top, panelWidth - 18, ROW, false,
                    mx >= x + 9 && mx < x + panelWidth - 9 && my >= top && my < top + ROW);
            g.drawString(font, Integer.toString(rank), x + 14, top + 4, rank <= 3 ? OreTheme.COINS : OreTheme.MUTED);
            int portraitX = x + 21 + font.width(Integer.toString(total));
            var member = new CompoundTag();
            member.putUUID("id", entry.id());
            member.putString("name", entry.name());
            portraits.render(g, member, portraitX, top + 4);
            String time = pvp ? Long.toString(entry.losses()) : formatTime(entry.ticks());
            int timeX = timeRight - font.width(time);
            int statsX = timeX;
            if (pvp) {
                String wins = Long.toString(entry.wins());
                statsX = winsRight - font.width(wins);
                g.drawString(font, wins, statsX, top + 4, OreTheme.GREEN);
            }
            g.drawString(font, font.plainSubstrByWidth(entry.name(), Math.max(0, statsX - portraitX - 18)),
                    portraitX + 12, top + 4, OreTheme.TEXT);
            g.drawString(font, time, timeX, top + 4, OreTheme.TEXT);
        }
        if (entries.isEmpty()) {
            var message = text(request != null ? "loading" : failed ? "unavailable" : pvp ? "pvp_empty" : "empty");
            g.drawCenteredString(font, font.substrByWidth(message, panelWidth - 24).getString(), width / 2,
                    y + HEADER + (pageSize * ROW - 8) / 2, OreTheme.MUTED);
        } else {
            g.drawCenteredString(font, text("page", page + 1, (total - 1) / pageSize + 1),
                    (x + 8 + previous.getX()) / 2 + 20, y + panelHeight - 20, OreTheme.MUTED);
        }
    }

    private static String formatTime(long ticks) {
        long seconds = ticks / 20;
        return String.format(Locale.ROOT, "%d:%02d.%d", seconds / 60, seconds % 60, ticks % 20 / 2);
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("rumble.server_hall." + key, args);
    }
}
