package com.p1nero.cobblemon.rumblecore.client.ui.ore;

import com.p1nero.cobblemon.rumblecore.prologue.MerchantHoopa;
import com.p1nero.cobblemon.rumblecore.prologue.MerchantPanels;
import com.p1nero.misc.custom_instances.client.style.UiTheme;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public final class HoopaRingScreen extends Screen {
    private static final String KEY = "rumble.hoopa.";
    private static final int CARD_HEIGHT = 58, GAP = 8, PADDING = 12;
    private record Destination(String id, Component name, ItemStack icon) {}

    private final List<Destination> destinations;
    private int x, y, w, h, columns, rows, offset;

    private HoopaRingScreen(List<Destination> destinations) {
        super(Component.translatable(KEY + "title"));
        this.destinations = destinations;
    }

    public static void open(CompoundTag data) {
        var destinations = new ArrayList<Destination>();
        var rows = data.getList("rows", Tag.TAG_COMPOUND);
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.getCompound(i);
            var id = row.getString("id");
            var localName = KEY + "destination." + id;
            var nativeName = row.getString("name");
            destinations.add(new Destination(id, Component.translatableWithFallback(
                    I18n.exists(localName) ? localName : nativeName, id), destinationIcon(id)));
        }
        Minecraft.getInstance().setScreen(new HoopaRingScreen(List.copyOf(destinations)));
    }

    private static String price() { return NumberFormat.getIntegerInstance().format(MerchantHoopa.PRICE); }

    private static ItemStack destinationIcon(String id) {
        String item = switch (id) {
            case "dungeon_distortion" -> "cobblesafari:ingredient_dungeon_distortion";
            case "dungeon_jump" -> "cobblesafari:belltower_bell";
            case "dungeon_megalopolis" -> "mega_showdown:ultranecrozium_z";
            case "dungeon_paris" -> "cobblesafari:ingredient_dungeon_paris";
            case "dungeon_underground" -> "cobblesafari:fossil_random";
            default -> "cobblesafari:hoopa_ring_portal";
        };
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(item))
                .orElse(Items.ENDER_EYE).getDefaultInstance();
    }

    @Override protected void init() {
        w = Math.min(446, width - 12);
        columns = Math.clamp((w - 2 * PADDING + GAP) / 82, 1, Math.min(5, Math.max(1, destinations.size())));
        rows = Math.clamp((height - 71) / (CARD_HEIGHT + GAP), 1, Math.max(1, totalRows()));
        h = 59 + rows * (CARD_HEIGHT + GAP);
        x = (width - w) / 2;
        y = (height - h) / 2;
        offset = Math.clamp(offset, 0, maxOffset());
        int cardWidth = (w - 2 * PADDING - (columns - 1) * GAP) / columns;
        for (int row = 0; row < rows; row++) {
            int first = (offset + row) * columns;
            int count = Math.min(columns, destinations.size() - first);
            int left = (width - count * cardWidth - (count - 1) * GAP) / 2;
            for (int i = 0; i < count; i++) {
                var destination = destinations.get(first + i);
                addRenderableWidget(new DestinationChoice(left + i * (cardWidth + GAP),
                        y + 36 + row * (CARD_HEIGHT + GAP), cardWidth, destination));
            }
        }
        int bottom = y + h - 23;
        addRenderableWidget(OreTheme.action(x + 12, bottom, 65, 17, Component.translatable("gui.back"), false, b -> onClose()));
        if (maxOffset() > 0) {
            addRenderableWidget(OreTheme.action(x + w - 60, bottom, 22, 17, Component.literal("↑"), false, b -> scroll(-1))).active = offset > 0;
            addRenderableWidget(OreTheme.action(x + w - 34, bottom, 22, 17, Component.literal("↓"), false, b -> scroll(1))).active = offset < maxOffset();
        }
    }

    private int totalRows() { return (destinations.size() + columns - 1) / columns; }
    private int maxOffset() { return Math.max(0, totalRows() - rows); }

    private void scroll(int delta) {
        int next = Math.clamp(offset + delta, 0, maxOffset());
        if (next != offset) { offset = next; rebuildWidgets(); }
    }

    private final class DestinationChoice extends Button {
        private final Destination destination;

        private DestinationChoice(int x, int y, int width, Destination destination) {
            super(x, y, width, CARD_HEIGHT, destination.name(),
                    b -> minecraft.setScreen(new Confirmation(destination)), DEFAULT_NARRATION);
            this.destination = destination;
            setTooltip(Tooltip.create(destination.name()));
        }

        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float tick) {
            boolean highlighted = isHoveredOrFocused();
            UiTheme.button(g, getX(), getY(), width, height, true, highlighted, false, false);
            int center = getX() + width / 2;
            drawIcon(g, destination.icon(), center, getY() + 20);
            int labelWidth = font.width(getMessage());
            float scale = Math.min(1F, (width - 10F) / Math.max(1, labelWidth));
            g.pose().pushPose();
            g.pose().translate(center, getY() + 43 - font.lineHeight * scale / 2, 0);
            g.pose().scale(scale, scale, 1);
            g.drawString(font, getMessage(), -labelWidth / 2, 0, UiTheme.buttonText(highlighted, OreTheme.TEXT), false);
            g.pose().popPose();
        }
    }

    private static void drawIcon(GuiGraphics g, ItemStack icon, int centerX, int centerY) {
        g.pose().pushPose();
        g.pose().translate(centerX, centerY, 0);
        g.pose().scale(1.625F, 1.625F, 1);
        g.renderItem(icon, -8, -8);
        g.pose().popPose();
    }

    private final class Confirmation extends Screen {
        private final Destination destination;
        private int left, top, panelWidth, panelHeight;
        private List<net.minecraft.util.FormattedCharSequence> question;

        private Confirmation(Destination destination) {
            super(HoopaRingScreen.this.title);
            this.destination = destination;
        }

        @Override protected void init() {
            panelWidth = Math.min(302, width - 12);
            question = font.split(Component.translatable(KEY + "confirm", destination.name(), price()), panelWidth - 24);
            panelHeight = 88 + question.size() * font.lineHeight;
            left = (width - panelWidth) / 2;
            top = (height - panelHeight) / 2;
            int buttonWidth = (panelWidth - 32) / 2;
            int bottom = top + panelHeight - 27;
            addRenderableWidget(OreTheme.action(left + 12, bottom, buttonWidth, 18,
                    Component.translatable("gui.cancel"), false, b -> onClose()));
            addRenderableWidget(OreTheme.action(left + panelWidth - 12 - buttonWidth, bottom, buttonWidth, 18,
                    Component.translatable("gui.yes"), true, b -> {
                        PacketDistributor.sendToServer(new MerchantPanels.HoopaEntry(destination.id()));
                        minecraft.setScreen(null);
                    }));
        }

        @Override public void render(GuiGraphics g, int mx, int my, float tick) {
            g.fill(0, 0, width, height, 0xb0161618);
            OreTheme.panel(g, left, top, panelWidth, panelHeight, 19);
            g.drawCenteredString(font, title, width / 2, top + 5, OreTheme.TEXT);
            drawIcon(g, destination.icon(), width / 2, top + 38);
            for (int i = 0; i < question.size(); i++)
                g.drawCenteredString(font, question.get(i), width / 2, top + 56 + i * font.lineHeight, OreTheme.COINS);
            super.render(g, mx, my, tick);
        }

        @Override public void onClose() { minecraft.setScreen(HoopaRingScreen.this); }
        @Override public void renderBackground(GuiGraphics g, int mx, int my, float tick) {}
        @Override public boolean isPauseScreen() { return false; }
    }

    @Override public void render(GuiGraphics g, int mx, int my, float tick) {
        g.fill(0, 0, width, height, 0xb0161618);
        OreTheme.panel(g, x, y, w, h, 19);
        g.drawCenteredString(font, title, width / 2, y + 5, OreTheme.TEXT);
        g.drawCenteredString(font, Component.translatable(KEY + "price", price()), width / 2, y + 23, OreTheme.COINS);
        if (destinations.isEmpty()) g.drawCenteredString(font, Component.translatable(KEY + "empty"), width / 2, y + 53, OreTheme.MUTED);
        super.render(g, mx, my, tick);
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (vertical != 0 && mx >= x && mx < x + w && my >= y + 36 && my < y + h - 25) {
            scroll(vertical > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float tick) {}
    @Override public boolean isPauseScreen() { return false; }
}
