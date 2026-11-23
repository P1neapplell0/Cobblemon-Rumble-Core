package com.p1nero.cobblemon.rumblecore.prologue.client;

import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TrialPortraits;
import com.p1nero.cobblemon.rumblecore.gym.CinemaTrials;
import com.p1nero.cobblemon.rumblecore.prologue.MegaSimulator;
import com.p1nero.misc.custom_instances.client.screen.LobbyScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** A catalog opens the native room lobby; cast choices belong to an existing room. */
public final class CinemaCatalogScreen extends Screen {
    private record Card(Component name, Component detail, List<CinemaTrials.Cast> cast,
                        boolean model, boolean enabled, boolean selected, Button.OnPress press) {}
    private final TrialPortraits portraits = new TrialPortraits();
    private final Screen parent;
    private final List<Card> cards;
    private final boolean roles;
    private CompoundTag partySnapshot;
    private int page, pageSize = 1, left, top, panelWidth, panelHeight;

    private CinemaCatalogScreen(Screen parent, Component title, List<Card> cards, boolean roles) {
        super(title);
        this.parent = parent;
        this.cards = cards;
        this.roles = roles;
    }

    public static void open() {
        var cards = new ArrayList<Card>();
        CinemaTrials.all().stream().filter(CinemaTrials.Trial::supported).forEach(trial -> {
            var featured = trial.sides().stream().map(List::getFirst).toList();
            cards.add(new Card(Component.translatable(trial.titleKey()), null, featured, false, true, false,
                    clicked -> PacketDistributor.sendToServer(new MegaSimulator.CinemaSelect(trial.id()))));
        });
        cards.add(new Card(text("coming_soon"), null, List.of(), false, false, false, clicked -> {}));
        Minecraft.getInstance().setScreen(new CinemaCatalogScreen(null, text("title"), List.copyOf(cards), false));
    }

    public static void chooseRole(Screen parent, CinemaTrials.Trial trial, CompoundTag party) {
        var roles = party.getCompound("rumbleCinema");
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        String viewer = player.getUUID().toString();
        var selections = roles.getCompound("selections");
        var current = selections.getCompound(viewer);
        int currentSide = current.contains("side") ? current.getInt("side") : -1;
        int currentSlot = current.getInt("slot");
        var cards = new ArrayList<Card>();
        for (int side = 0; side < trial.sides().size(); side++) {
            int faction = side;
            var lineup = trial.sides().get(side);
            for (int slot = 0; slot < lineup.size(); slot++) {
                int index = slot;
                var cast = lineup.get(slot);
                var occupants = selections.getAllKeys().stream().filter(id -> !id.equals(viewer))
                        .map(selections::getCompound).filter(role -> {
                            var other = trial.castAt(role.getInt("side"), role.getInt("slot"));
                            return other != null && other.species().equals(cast.species());
                        }).map(role -> role.getString("name")).toList();
                var detail = occupants.isEmpty() ? text("faction", side + 1, slot + 1)
                        : text("role_taken", side + 1, String.join(", ", occupants)).withStyle(ChatFormatting.RED);
                cards.add(new Card(cast.name(), detail, List.of(cast), true, true, side == currentSide && slot == currentSlot,
                        clicked -> PacketDistributor.sendToServer(new MegaSimulator.CinemaRole(party.getUUID("session"), faction, index))));
            }
        }
        var screen = new CinemaCatalogScreen(parent, text("choose_role"), List.copyOf(cards), true);
        screen.partySnapshot = party.copy();
        Minecraft.getInstance().setScreen(screen);
    }

    public boolean roomUpdate(String kind, CompoundTag data) {
        if (!(parent instanceof LobbyScreen lobby) || partySnapshot == null) return false;
        if (kind.equals("close")) {
            onCloseTo(null);
            return true;
        }
        if (!kind.equals("rooms")) return false;
        lobby.updateRooms(data);
        var party = data.getCompound("party");
        if (!party.getString("state").equals("WAITING") || !party.contains("rumbleCinema")
                || !partySnapshot.getUUID("session").equals(party.getUUID("session"))) {
            onClose();
        } else if (!partySnapshot.equals(party)) {
            var trial = CinemaTrials.Trial.fromTemplate(data.getCompound("definition").getString("id"));
            if (trial != null) chooseRole(parent, trial, party);
        }
        return true;
    }

    @Override protected void init() {
        panelWidth = Math.min(414, width - 12);
        panelHeight = Math.min(248, height - 10);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int gap = 5, cellHeight = roles ? 76 : 54;
        int columns = Math.max(1, Math.min(4, (panelWidth - 16) / (roles ? 88 : 97)));
        int cellWidth = (panelWidth - 20 - (columns - 1) * gap) / columns;
        pageSize = columns * Math.max(1, (panelHeight - 62) / (cellHeight + gap));
        page = Math.min(page, pages() - 1);
        for (int i = 0; i < pageSize && page * pageSize + i < cards.size(); i++) {
            Card card = cards.get(page * pageSize + i);
            addCard(card, left + 10 + i % columns * (cellWidth + gap), top + 31 + i / columns * (cellHeight + gap), cellWidth, cellHeight);
        }
        int footer = top + panelHeight - 25;
        addRenderableWidget(OreTheme.action(left + 10, footer, 29, 19, Component.literal("‹"), false, clicked -> flip(-1))).active = page > 0;
        addRenderableWidget(OreTheme.action(left + panelWidth - 39, footer, 29, 19, Component.literal("›"), false, clicked -> flip(1))).active = page + 1 < pages();
        addRenderableWidget(OreTheme.action(width / 2 - 42, footer, 84, 19,
                Component.translatable(parent != null ? "gui.back" : "gui.done"), false, clicked -> onClose()));
    }

    private void addCard(Card card, int x, int y, int w, int h) {
        var button = new Button(x, y, w, h, card.name(), card.press(), narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
                OreTheme.button(g, this, null, card.selected(), active);
                int imageHeight = h - (card.model() ? 30 : 20);
                OreTheme.well(g, x + 4, y + 3, w - 8, imageHeight);
                int count = card.cast().size();
                if (count == 0) {
                    g.pose().pushPose();
                    g.pose().translate(x + w / 2F, y + 3 + imageHeight / 2F, 0);
                    g.pose().scale(2.5F, 2.5F, 1);
                    g.drawCenteredString(font, "?", 0, -font.lineHeight / 2, OreTheme.MUTED);
                    g.pose().popPose();
                } else {
                    int size = Math.min(imageHeight - 4, (w - 10) / count);
                    for (int i = 0; i < count; i++) {
                        var cast = card.cast().get(i);
                        int px = x + (w - count * size) / 2 + i * size, py = y + 3 + (imageHeight - size) / 2;
                        if (card.model() || !portraits.drawFace(g, cast.speciesId(), cast.aspects(), px, py, size, partial, i > 0))
                            portraits.render(g, cast.speciesId(), cast.aspects(), px, py, size, partial);
                    }
                }
                centered(g, card.name(), x + w / 2, y + imageHeight + 7, w - 8, active ? OreTheme.TEXT : OreTheme.MUTED);
                if (card.detail() != null) centered(g, card.detail(), x + w / 2, y + h - 13, w - 8, card.selected() ? OreTheme.GREEN : OreTheme.MUTED);
            }
        };
        button.active = card.enabled();
        button.setTooltip(Tooltip.create(card.detail() == null ? card.name() : card.name().copy().append("\n").append(card.detail())));
        addRenderableWidget(button);
    }

    private void centered(GuiGraphics g, Component text, int center, int y, int max, int color) {
        g.drawCenteredString(font, Language.getInstance().getVisualOrder(font.substrByWidth(text, Math.max(1, max))), center, y, color);
    }

    private int pages() { return Math.max(1, (cards.size() + pageSize - 1) / pageSize); }
    private void flip(int delta) {
        int next = Math.clamp(page + delta, 0, pages() - 1);
        if (next != page) { page = next; rebuildWidgets(); }
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double delta) {
        if (delta == 0) return false;
        flip(delta > 0 ? -1 : 1);
        return true;
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        super.renderBackground(g, mx, my, partial);
        OreTheme.panel(g, left, top, panelWidth, panelHeight, 25);
        centered(g, title, width / 2, top + 9, panelWidth - 14, OreTheme.TEXT);
        if (cards.isEmpty()) centered(g, text("unavailable"), width / 2, top + 45, panelWidth - 16, OreTheme.MUTED);
        super.render(g, mx, my, partial);
    }
    @Override public void onClose() { onCloseTo(parent); }
    private static void onCloseTo(Screen screen) { Minecraft.getInstance().setScreen(screen); }
    @Override public boolean isPauseScreen() { return false; }
    private static MutableComponent text(String key, Object... args) { return Component.translatable("rumble.cinema." + key, args); }
}
