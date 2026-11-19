package com.p1nero.cobblemon.rumblecore.client.ui.ore;

import com.p1nero.cobblemon.rumblecore.shop.ProfessorCatalogue;
import com.p1nero.cobblemon.rumblecore.shop.RecyclingCatalogue;
import com.p1nero.cobblemon.rumblecore.shop.RecyclingPackets;
import fr.harmex.cobbledollars.common.client.gui.screen.BankScreen;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Price list of the live bank. Bulk actions are available only at the professor's counter. */
public final class RecyclingScreen extends Screen {
    private static final String KEY = "rumble.bank_prices.";
    private static final int ROW = 21;
    private final BankScreen bank;
    private final boolean bulk;
    private final List<Group> groups;
    private List<Entry> visible;
    private int category;
    private int offset;
    private int x, y, w, h, listY, rows;
    private Button groupButton, recycle, selectGroup;
    private final Set<ResourceLocation> selected = new HashSet<>();
    private RecyclingPackets.Result result;
    private boolean waiting;

    public RecyclingScreen(BankScreen bank, boolean bulk) {
        super(Component.translatable(KEY + "title"));
        this.bank = bank;
        this.bulk = bulk;
        this.groups = groups(bank, bulk);
        this.visible = groups.getFirst().entries();
    }

    @Override protected void init() {
        w = Math.min(390, width - 12);
        h = Math.min(310, height - 12);
        x = (width - w) / 2;
        y = (height - h) / 2;
        listY = y + (bulk ? 65 : 46);
        rows = Math.max(1, (h - (listY - y) - (bulk ? 56 : 30)) / ROW);
        int footer = y + h - 23;
        groupButton = addRenderableWidget(OreTheme.action(x + 29, y + 22, w - 58, 18, groups.get(category).title(), false, b -> next(1)));
        addRenderableWidget(OreTheme.action(x + 7, y + 22, 18, 18, Component.literal("<"), false, b -> next(-1)));
        addRenderableWidget(OreTheme.action(x + w - 25, y + 22, 18, 18, Component.literal(">"), false, b -> next(1)));
        addRenderableWidget(OreTheme.action(x + 7, footer, 64, 17, Component.translatable(KEY + "back"), false, b -> onClose()));
        if (bulk) {
            selectGroup = addRenderableWidget(OreTheme.action(x + 7, y + 43, w - 14, 17, Component.empty(), false, b -> toggleGroup()));
            recycle = addRenderableWidget(OreTheme.action(x + 75, footer, w - 82, 17, Component.empty(), true, b -> request()));
            updateButton();
        }
        offset = Mth.clamp(offset, 0, maxOffset());
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float tick) {
        // The catalogue draws its own scrim before its contents.
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        g.fill(0, 0, width, height, 0xB0161618);
        OreTheme.panel(g, x, y, w, h, 18);
        g.drawCenteredString(font, title, width / 2, y + 5, OreTheme.TEXT);
        ItemStack hovered = null;
        if (visible.isEmpty()) g.drawCenteredString(font, Component.translatable(KEY + "empty"), width / 2, listY + 6, OreTheme.MUTED);
        for (int i = 0; i < rows && offset + i < visible.size(); i++) {
            Entry entry = visible.get(offset + i);
            int top = listY + i * ROW;
            boolean hover = mx >= x + 7 && mx < x + w - 14 && my >= top && my < top + ROW - 1;
            boolean checked = selected.contains(entry.id());
            OreTheme.row(g, x + 7, top, w - 22, ROW - 1, checked, hover);
            int inset = bulk ? 17 : 0;
            if (bulk) checkbox(g, x + 11, top + 4, checked);
            g.renderItem(entry.stack(), x + 10 + inset, top + 2);
            String price = money(entry.price().toString());
            int priceX = x + w - 20 - font.width(price);
            g.drawString(font, font.substrByWidth(entry.stack().getHoverName(), Math.max(0, priceX - x - 35 - inset)).getString(), x + 30 + inset, top + 6, OreTheme.TEXT, false);
            g.drawString(font, price, priceX, top + 6, OreTheme.COINS, false);
            if (hover) hovered = entry.stack();
        }
        if (maxOffset() > 0) {
            int track = rows * ROW, thumb = Math.max(9, track * rows / visible.size());
            int top = listY + (track - thumb) * offset / maxOffset();
            OreTheme.well(g, x + w - 12, listY, 5, track);
            g.fill(x + w - 11, top, x + w - 8, top + thumb, OreTheme.MUTED);
        }
        if (bulk && result != null) renderQuote(g);
        super.render(g, mx, my, partialTick);
        if (hovered != null) g.renderTooltip(font, hovered, mx, my);
    }

    private static void checkbox(GuiGraphics g, int x, int y, boolean checked) {
        OreTheme.well(g, x, y, 12, 12);
        g.renderOutline(x, y, 12, 12, checked ? OreTheme.GREEN : OreTheme.MUTED);
        if (checked) {
            g.fill(x + 3, y + 6, x + 5, y + 8, OreTheme.GREEN);
            g.fill(x + 5, y + 5, x + 7, y + 7, OreTheme.GREEN);
            g.fill(x + 7, y + 3, x + 9, y + 5, OreTheme.GREEN);
        }
    }

    private void renderQuote(GuiGraphics g) {
        int top = y + h - 52;
        if (result.status().equals("unavailable")) {
            g.drawString(font, Component.translatable(KEY + "unavailable"), x + 8, top + 9, OreTheme.MUTED, false);
            return;
        }
        g.drawString(font, Component.translatable(KEY + "status." + result.status()), x + 8, top, OreTheme.GREEN, false);
        g.drawString(font, Component.translatable(KEY + "inventory", money(result.inventory())), x + 8, top + 10, OreTheme.TEXT, false);
        g.drawString(font, Component.translatable(KEY + "storage", money(result.storage())), x + 8, top + 20, OreTheme.TEXT, false);
    }

    @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (mx >= x && mx < x + w && my >= listY && my < listY + rows * ROW) {
            offset = Mth.clamp(offset - (int) Math.signum(dy) * 2, 0, maxOffset());
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && maxOffset() > 0 && mx >= x + w - 15 && mx < x + w - 5 && my >= listY && my < listY + rows * ROW) {
            offset = Mth.clamp((int) Math.round((my - listY) / (rows * ROW) * maxOffset()), 0, maxOffset());
            return true;
        }
        if (bulk && !waiting && button == 0 && mx >= x + 7 && mx < x + w - 15 && my >= listY && my < listY + rows * ROW) {
            int index = offset + ((int) my - listY) / ROW;
            if (index < visible.size()) {
                var id = visible.get(index).id();
                if (!selected.add(id)) selected.remove(id);
                result = null;
                updateButton();
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (key == 266 || key == 267) {
            offset = Mth.clamp(offset + (key == 266 ? -rows : rows), 0, maxOffset());
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override public void onClose() {
        minecraft.setScreen(minecraft.player != null && minecraft.player.containerMenu == bank.getMenu() ? bank : null);
    }

    @Override public void tick() {
        if (minecraft.player == null || minecraft.player.containerMenu != bank.getMenu()) minecraft.setScreen(null);
    }

    private int maxOffset() { return Math.max(0, visible.size() - rows); }

    private void next(int direction) {
        category = Math.floorMod(category + direction, groups.size());
        visible = groups.get(category).entries();
        groupButton.setMessage(groups.get(category).title());
        offset = 0;
        if (bulk) updateButton();
    }

    private void request() {
        waiting = true;
        result = null;
        PacketDistributor.sendToServer(new RecyclingPackets.Request(bank.getMenu().containerId, 0, List.copyOf(selected)));
        updateButton();
    }

    private void toggleGroup() {
        boolean all = visible.stream().allMatch(entry -> selected.contains(entry.id()));
        for (var entry : visible) {
            if (all) selected.remove(entry.id());
            else selected.add(entry.id());
        }
        result = null;
        updateButton();
    }

    private void updateButton() {
        recycle.active = !waiting && !selected.isEmpty() && (result == null || !result.status().equals("unavailable"));
        recycle.setMessage(Component.translatable(KEY + (waiting ? "quoting" : "bulk"), selected.size()));
        recycle.setTooltip(Tooltip.create(Component.translatable(KEY + "bulk_tip")));
        long checked = visible.stream().filter(entry -> selected.contains(entry.id())).count();
        selectGroup.active = !waiting && !visible.isEmpty();
        selectGroup.setMessage(Component.translatable(KEY + (checked == visible.size() && checked > 0 ? "clear_group" : "select_group"), checked, visible.size()));
    }

    public static void receive(RecyclingPackets.Result result) {
        if (Minecraft.getInstance().screen instanceof RecyclingScreen screen && screen.bank.getMenu().containerId == result.menuId()) {
            screen.waiting = false;
            if (result.quote() != 0) {
                screen.minecraft.setScreen(screen.new Confirmation(result));
                return;
            }
            screen.result = result;
            screen.updateButton();
        }
    }

    private final class Confirmation extends Screen {
        private final RecyclingPackets.Result quote;
        private int left, top, panelWidth;

        private Confirmation(RecyclingPackets.Result quote) {
            super(Component.translatable(KEY + "confirm_title"));
            this.quote = quote;
        }

        @Override protected void init() {
            panelWidth = Math.min(290, width - 12);
            left = (width - panelWidth) / 2;
            top = (height - 106) / 2;
            int buttonWidth = (panelWidth - 24) / 2;
            addRenderableWidget(OreTheme.action(left + 8, top + 78, buttonWidth, 19, Component.translatable(KEY + "cancel"), false, b -> onClose()));
            addRenderableWidget(OreTheme.action(left + panelWidth - buttonWidth - 8, top + 78, buttonWidth, 19,
                    Component.translatable(KEY + "confirm"), true, b -> {
                        waiting = true;
                        minecraft.setScreen(RecyclingScreen.this);
                        PacketDistributor.sendToServer(new RecyclingPackets.Request(quote.menuId(), quote.quote(), List.of()));
                    }));
        }

        @Override public void renderBackground(GuiGraphics g, int mx, int my, float tick) {}
        @Override public boolean isPauseScreen() { return false; }
        @Override public void onClose() { minecraft.setScreen(RecyclingScreen.this); }
        @Override public void tick() {
            if (minecraft.player == null || minecraft.player.containerMenu != bank.getMenu()) minecraft.setScreen(null);
        }

        @Override public void render(GuiGraphics g, int mx, int my, float tick) {
            g.fill(0, 0, width, height, 0xC0161618);
            OreTheme.panel(g, left, top, panelWidth, 106, 21);
            g.drawCenteredString(font, title, width / 2, top + 6, OreTheme.TEXT);
            g.drawString(font, Component.translatable(KEY + (quote.status().equals("changed") ? "status.changed" : "selection_count"), selected.size()), left + 9, top + 27, OreTheme.MUTED, false);
            g.drawString(font, Component.translatable(KEY + "inventory", money(quote.inventory())), left + 9, top + 42, OreTheme.TEXT, false);
            g.drawString(font, Component.translatable(KEY + "storage", money(quote.storage())), left + 9, top + 55, OreTheme.TEXT, false);
            super.render(g, mx, my, tick);
        }
    }

    private static List<Group> groups(BankScreen screen, boolean bulk) {
        Map<String, List<Entry>> grouped = new LinkedHashMap<>();
        Map<Item, String> retail = new java.util.HashMap<>();
        if (bulk) for (var category : ProfessorCatalogue.create().getCategories()) {
            grouped.put(category.getName(), new ArrayList<>());
            for (var offer : category.getOffers()) for (var stack : offer.getItems()) retail.put(stack.getItem(), category.getName());
        }
        for (String group : List.of("materials", "produce", "valuables", "other")) grouped.put(KEY + "category." + group, new ArrayList<>());
        List<Entry> all = new ArrayList<>();
        for (var offer : screen.getMenu().getBankRef().getBank().getOffers().keySet()) for (var stack : offer.getItems()) {
            Entry entry = new Entry(stack, offer.getPrice());
            all.add(entry);
            grouped.get(retail.getOrDefault(stack.getItem(), KEY + "category." + RecyclingCatalogue.legacyCategory(stack))).add(entry);
        }
        Comparator<Entry> nameOrder = Comparator.comparing(entry -> entry.stack().getHoverName().getString());
        List<Group> groups = new ArrayList<>();
        groups.add(new Group(Component.translatable(KEY + "category.all"), all.stream().sorted(nameOrder).toList()));
        grouped.forEach((key, entries) -> {
            if (!entries.isEmpty()) groups.add(new Group(Component.translatable(key), entries.stream().sorted(nameOrder).toList()));
        });
        return groups;
    }

    private static String money(String raw) { return NumberFormat.getIntegerInstance().format(new BigInteger(raw)); }
    private record Entry(ItemStack stack, BigInteger price) {
        ResourceLocation id() { return BuiltInRegistries.ITEM.getKey(stack.getItem()); }
    }
    private record Group(Component title, List<Entry> entries) {}
}
