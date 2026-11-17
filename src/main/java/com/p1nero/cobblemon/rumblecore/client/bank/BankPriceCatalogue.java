package com.p1nero.cobblemon.rumblecore.client.bank;

import fr.harmex.cobbledollars.common.api.bank.Bank;
import fr.harmex.cobbledollars.common.api.shop.Offer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Scrollable catalogue of every item accepted by the currently open bank. */
public final class BankPriceCatalogue extends Screen {

    private static final int ROW_HEIGHT = 24;
    private static final int HEADER_HEIGHT = 34;
    private static final int FOOTER_HEIGHT = 30;
    private static final int MAX_PANEL_WIDTH = 520;
    private final Screen parent;
    private final List<PriceEntry> entries;
    private double scroll;
    private boolean draggingScrollbar;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listTop;
    private int listBottom;
    private int scrollbarX;
    private Button backButton;

    public BankPriceCatalogue(Screen parent, Bank bank) {
        super(Component.translatable("rumble.bank_prices.title"));
        this.parent = parent;
        this.entries = createEntries(bank);
    }

    @Override
    protected void init() {
        layout();
        backButton = addRenderableWidget(Button.builder(
                        Component.translatable("rumble.bank_prices.back"),
                        button -> onClose())
                .bounds(width / 2 - 52, panelY + panelHeight - 23, 104, 18)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE60A0F12);
        layout();
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF0181E22);
        graphics.fill(panelX, panelY, panelX + 3, panelY + panelHeight, 0xFF6EA66B);
        graphics.fill(panelX + 3, panelY, panelX + panelWidth, panelY + 1, 0xFF65757D);
        graphics.drawCenteredString(font, title, width / 2, panelY + 10, 0xFFF4F7F8);

        int contentLeft = panelX + 12;
        int contentRight = panelX + panelWidth - 19;
        graphics.enableScissor(contentLeft, listTop, contentRight, listBottom);
        int first = Math.max(0, (int) (scroll / ROW_HEIGHT));
        int y = listTop - (int) scroll + first * ROW_HEIGHT;
        PriceEntry hovered = null;
        for (int index = first; index < entries.size() && y < listBottom; index++) {
            PriceEntry entry = entries.get(index);
            boolean isHovered = mouseX >= contentLeft && mouseX < contentRight
                    && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
            graphics.fill(contentLeft, y, contentRight, y + ROW_HEIGHT - 2,
                    isHovered ? 0xCC344249 : (index % 2 == 0 ? 0x8830393E : 0x88272F33));
            graphics.renderItem(entry.stack(), contentLeft + 4, y + 3);
            graphics.drawString(font, font.plainSubstrByWidth(
                            entry.stack().getHoverName().getString(), panelWidth - 155),
                    contentLeft + 25, y + 7, 0xFFF0F3F4, false);
            String price = formatPrice(entry.price());
            graphics.drawString(font, price, contentRight - font.width(price) - 6,
                    y + 7, 0xFFFFD166, false);
            if (isHovered) hovered = entry;
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();
        renderScrollbar(graphics, mouseX, mouseY);
        backButton.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null) graphics.renderTooltip(font, hovered.stack(), mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= panelX && mouseX <= panelX + panelWidth
                && mouseY >= listTop && mouseY <= listBottom) {
            scroll = Mth.clamp(scroll - scrollY * ROW_HEIGHT * 2.0D, 0.0D, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScroll() > 0 && mouseX >= scrollbarX - 2
                && mouseX <= scrollbarX + 8 && mouseY >= listTop && mouseY <= listBottom) {
            draggingScrollbar = true;
            setScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            setScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void layout() {
        panelWidth = Math.min(MAX_PANEL_WIDTH, Math.max(270, width - 32));
        panelHeight = Math.max(150, height - 28);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        listTop = panelY + HEADER_HEIGHT;
        listBottom = panelY + panelHeight - FOOTER_HEIGHT;
        scrollbarX = panelX + panelWidth - 13;
        scroll = Mth.clamp(scroll, 0.0D, maxScroll());
    }

    private void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        int trackHeight = listBottom - listTop;
        graphics.fill(scrollbarX, listTop, scrollbarX + 6, listBottom, 0xFF0D1113);
        if (maxScroll() <= 0) return;
        int thumbHeight = Math.max(18, trackHeight * trackHeight
                / Math.max(trackHeight, entries.size() * ROW_HEIGHT));
        int travel = trackHeight - thumbHeight;
        int thumbY = listTop + (int) Math.round(scroll / maxScroll() * travel);
        boolean hovered = mouseX >= scrollbarX - 2 && mouseX <= scrollbarX + 8
                && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
        graphics.fill(scrollbarX + 1, thumbY, scrollbarX + 5, thumbY + thumbHeight,
                hovered || draggingScrollbar ? 0xFFE6C85C : 0xFF82949C);
    }

    private void setScrollFromMouse(double mouseY) {
        int trackHeight = listBottom - listTop;
        int thumbHeight = Math.max(18, trackHeight * trackHeight
                / Math.max(trackHeight, entries.size() * ROW_HEIGHT));
        double ratio = (mouseY - listTop - thumbHeight / 2.0D)
                / Math.max(1.0D, trackHeight - thumbHeight);
        scroll = Mth.clamp(ratio, 0.0D, 1.0D) * maxScroll();
    }

    private double maxScroll() {
        return Math.max(0, entries.size() * ROW_HEIGHT - (listBottom - listTop));
    }

    private static List<PriceEntry> createEntries(Bank bank) {
        List<PriceEntry> result = new ArrayList<>();
        for (Offer offer : bank.getOffers().keySet()) {
            for (ItemStack stack : offer.getItems()) {
                if (!stack.isEmpty()) result.add(new PriceEntry(stack, offer.getPrice()));
            }
        }
        result.sort(Comparator
                .comparing(PriceEntry::price)
                .thenComparing(entry -> entry.stack().getHoverName().getString(),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(entry -> entry.stack().getItem().toString()));
        return List.copyOf(result);
    }

    private static String formatPrice(BigInteger price) {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.ROOT);
        return format.format(price);
    }

    private record PriceEntry(ItemStack stack, BigInteger price) {
    }
}
