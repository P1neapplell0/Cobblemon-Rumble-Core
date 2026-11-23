package com.p1nero.cobblemon.rumblecore.client.combat;

import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TrialPortraits;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TutorialPulse;
import com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierRooms;
import com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierRoster;
import fr.harmex.cobbledollars.common.utils.extensions.BigIntegerExtensionsKt;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.arcadia.arc_quest.client.hud.style.OreUiRenderer.Action;
import org.arcadia.arc_quest.client.hud.style.OreUiRenderer;
import org.lwjgl.glfw.GLFW;

import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.CYAN;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.GOLD;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.MUTED;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.NEUTRAL;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.PINK;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.TEXT;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.card;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.center;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.item;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.moveStrip;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.moveTool;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.partyFace;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.placeholder;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.player;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.shell;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.text;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.typeIcon;

/** Client views of the authoritative room. Closing the editor never implicitly forfeits a match. */
public final class FrontierScreen extends Screen {
    private static final String KEY = "rumble.frontier.";
    private enum Page { LOBBY, CREATE, ROOM, PICKER, WAITING, BRACKET, INVITE }
    private enum Kind { SPECIES, HELD }
    private enum ItemsFilter { ALL, HELD, MEGA }
    private CompoundTag state;
    private Page page;
    private Kind kind;
    private int slot, move = -1, moveOffset, generation, offset, x, y, w, h;
    private ItemsFilter itemFilter = ItemsFilter.ALL;
    private String filter = "", stake = "0";
    private List<Option> options = List.of();
    private EditBox field;
    private boolean pending, megaLimitOpen;
    private int refreshTicks;
    private final TrialPortraits portraits = new TrialPortraits();
    private int moveAnchorX, moveAnchorY;
    private String moveQuery = "";
    private boolean generationsOpen;
    private enum CreateMenu { MODE, ROSTER }
    private CreateMenu createMenu;
    private boolean createMulti, ownParty, bracketPinned;
    private String capacity = "";
    private int roomFilter, bracketColumn;
    private String inviteQuery = "";

    private record Option(Component name, String id, Runnable apply) { }

    private FrontierScreen(CompoundTag state) {
        super(text("title"));
        this.state = state;
        page = destination(state);
        bracketColumn = roundColumn();
    }

    private static Page destination(CompoundTag data) {
        if (!data.hasUUID("room")) return Page.LOBBY;
        if (data.getInt("round") > 0) return data.contains("own") ? Page.ROOM : Page.BRACKET;
        return multi(data) ? Page.WAITING : Page.ROOM;
    }

    private static boolean multi(CompoundTag data) {
        return data.contains("multi") ? data.getBoolean("multi") : data.getInt("capacity") > 2;
    }

    public static void receive(CompoundTag data) {
        var mc = Minecraft.getInstance();
        FrontierTournamentHud.sync(data);
        if (mc.screen instanceof FrontierScreen screen) {
            if (data.getBoolean("close")) { mc.setScreen(null); return; }
            boolean hadRoom = screen.state.hasUUID("room");
            Page before = destination(screen.state), after = destination(data);
            boolean advance = data.getInt("round") != screen.state.getInt("round");
            screen.state = data;
            screen.pending = false;
            if (data.hasUUID("room") != hadRoom || advance || before != after && !(screen.bracketPinned && after == Page.ROOM)
                    || screen.page == Page.INVITE && !data.getBoolean("canInvite")) {
                screen.page = after;
                screen.bracketPinned = false;
                screen.offset = 0;
                screen.bracketColumn = screen.roundColumn();
                screen.bracketScroll = 0;
                screen.createMenu = null;
                screen.move = -1;
                screen.generationsOpen = false;
                screen.moveOffset = 0;
                screen.megaLimitOpen = false;
            }
            if (screen.move >= 0 && (screen.own().getBoolean("ready") ||
                    FrontierRoster.NONE.equals(screen.get(screen.own(), screen.slot).species()))) screen.move = -1;
            if (data.hasUUID("room") && "mega_limit".equals(data.getString("problem"))) {
                screen.page = Page.ROOM;
                screen.move = -1;
                screen.megaLimitOpen = true;
            }
            if (screen.page == Page.INVITE && screen.field != null && screen.field.isFocused()) {
                screen.rebuildWidgets();
                screen.setFocused(screen.field);
                screen.field.moveCursorToEnd(false);
            } else if (screen.page != Page.PICKER || !data.getString("problem").isEmpty()) screen.rebuildWidgets();
        } else if (data.getBoolean("open") && mc.level != null
                && !(data.getBoolean("automatic") && FrontierTournamentHud.isPinned())) {
            mc.setScreen(new FrontierScreen(data));
        }
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override protected void init() {
        int preferredWidth = switch (page) {
            case CREATE, INVITE -> 320;
            case BRACKET -> state.getBoolean("finished") ? 330 : 620;
            default -> 620;
        };
        int preferredHeight = switch (page) {
            case ROOM -> 330;
            case CREATE -> createMulti ? 180 : 153;
            case BRACKET -> state.getBoolean("finished") ? 195 : 300;
            default -> 300;
        };
        w = Math.min(preferredWidth, width - 14);
        h = Math.min(preferredHeight, height - 12);
        x = (width - w) / 2;
        y = (height - h) / 2;
        field = null;
        switch (page) {
            case LOBBY -> {
                btn(x + w - 100, y + 7, 91, text("create_plus"), true, Action.PRIMARY, () -> page(Page.CREATE));
                int start = x + 12;
                for (int i = 0; i < 3; i++) {
                    int value = i;
                    btn(start + 61 * i, y + 34, 57, text(i == 0 ? "filter_all" : i == 1 ? "mode_solo" : "mode_multi"),
                            true, roomFilter == i ? Action.PRIMARY : Action.SECONDARY, () -> { roomFilter = value; offset = 0; rebuildWidgets(); });
                }
                rooms();
            }
            case CREATE -> createForm();
            case ROOM -> lineups();
            case PICKER -> picker();
            case WAITING -> waiting();
            case BRACKET -> bracketButtons();
            case INVITE -> invitation();
        }
        if (page == Page.CREATE || page == Page.PICKER || page == Page.INVITE)
            btn(x + 9, y + 7, 62, text("back"), () -> page(page == Page.CREATE ? Page.LOBBY : destination(state)));
        if (state.hasUUID("room") && (page == Page.WAITING || page == Page.ROOM && !tournament()))
            btn(x + 91, y + 7, 81, text("pin_room"), this::pinRoom);
        if (megaLimitOpen) megaLimitDialog();
    }

    @Override public void tick() {
        if ((page == Page.LOBBY || page == Page.INVITE) && ++refreshTicks >= 40) {
            refreshTicks = 0;
            if (!pending) request("list", tag -> {});
        }
    }

    private void page(Page next) {
        generationsOpen = false;
        createMenu = null;
        move = -1;
        page = next;
        if (next == Page.BRACKET) { bracketColumn = roundColumn(); bracketScroll = 0; }
        offset = 0;
        rebuildWidgets();
    }

    private void request(String action, Consumer<CompoundTag> payload) {
        if (pending && !"leave".equals(action)) return;
        var data = new CompoundTag();
        data.putString("action", action);
        if (state.hasUUID("room")) data.putUUID("room", state.getUUID("room"));
        data.putInt("round", state.getInt("round"));
        payload.accept(data);
        pending = true;
        PacketDistributor.sendToServer(new FrontierRooms.Command(data));
    }

    private int createRow(int row) { return y + 34 + row * Math.min(26, (h - 62) / (createMulti ? 4 : 3)); }
    private int createInputX() { return x + Math.min(112, w * 2 / 5); }
    private int createInputWidth() { return x + w - 15 - createInputX(); }

    private void createForm() {
        btn(createInputX(), createRow(0), createInputWidth(), createOption(CreateMenu.MODE, createMulti).copy().append("  ▾"),
                () -> createMenu = createMenu == CreateMenu.MODE ? null : CreateMenu.MODE);
        btn(createInputX(), createRow(1), createInputWidth(), createOption(CreateMenu.ROSTER, ownParty).copy().append("  ▾"),
                () -> createMenu = createMenu == CreateMenu.ROSTER ? null : CreateMenu.ROSTER);
        var stakeBox = edit(createInputX(), createRow(2), createInputWidth(), stake, value -> stake = value);
        stakeBox.setMaxLength(18);
        stakeBox.setFilter(value -> value.matches("[0-9]{0,18}"));
        if (createMulti) {
            var limitBox = edit(createInputX(), createRow(3), createInputWidth(), capacity, value -> capacity = value);
            limitBox.setMaxLength(10);
            limitBox.setFilter(value -> value.matches("[0-9]{0,10}"));
            limitBox.setHint(text("capacity_unlimited"));
        }
        btn(x + w / 2 - 55, y + h - 27, 110, text("create"), true, Action.PRIMARY, () -> {
            int limit;
            try { limit = createMulti && !capacity.isBlank() ? Integer.parseInt(capacity) : 0; }
            catch (NumberFormatException e) { limit = -1; }
            int requestedLimit = limit;
            request("create", tag -> {
                tag.putString("stake", stake.isBlank() ? "0" : stake);
                tag.putBoolean("multi", createMulti);
                tag.putBoolean("ownParty", ownParty);
                tag.putInt("capacity", requestedLimit);
            });
        });
    }

    private void drawCreateForm(GuiGraphics g) {
        String[] labels = {"create_mode", "create_roster", "stake_label", "capacity"};
        for (int i = 0; i < (createMulti ? 4 : 3); i++) {
            FrontierConsole.text(g, font, text(labels[i]), x + 15, createRow(i) + 6, createInputX() - x - 20, i == 2 ? GOLD : TEXT);
        }
    }

    private static Component createOption(CreateMenu menu, boolean second) {
        return text(menu == CreateMenu.MODE ? second ? "mode_multi" : "mode_solo" : second ? "own_party" : "any_party");
    }

    private void selectCreateOption(int index) {
        if (createMenu == CreateMenu.MODE) createMulti = index == 1;
        else ownParty = index == 1;
        createMenu = null;
        rebuildWidgets();
    }

    private void drawCreateMenu(GuiGraphics g, int mx, int my) {
        int top = createRow(createMenu == CreateMenu.MODE ? 0 : 1) + 20;
        int left = createInputX(), width = createInputWidth();
        g.pose().pushPose();
        try {
            g.pose().translate(0, 0, 400);
            for (int i = 0; i < 2; i++) {
                int row = top + i * 20;
                boolean selected = (i == 1) == (createMenu == CreateMenu.MODE ? createMulti : ownParty);
                OreUiRenderer.labeledButton(g, font, createOption(createMenu, i == 1), left, row, width, 20,
                        selected ? Action.PRIMARY : Action.SECONDARY, inside(mx, my, left, row, width, 20), true, 1, NEUTRAL);
            }
        } finally { g.pose().popPose(); }
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (page == Page.CREATE && createMenu != null) {
            if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
                selectCreateOption(key == GLFW.GLFW_KEY_UP ? 0 : 1);
                return true;
            }
            if (key != GLFW.GLFW_KEY_ESCAPE) return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private static final int ROOM_STRIDE = 83;
    private int roomRows() { return Math.max(1, (h - 89) / ROOM_STRIDE); }
    private int roomColumns() { return Math.clamp((w - 24) / 131, 1, 4); }
    private List<CompoundTag> listings() {
        return state.getList("rooms", Tag.TAG_COMPOUND).stream().map(CompoundTag.class::cast)
                .filter(row -> roomFilter == 0 || multi(row) == (roomFilter == 2)).toList();
    }

    private void rooms() {
        var rooms = listings();
        int rows = roomRows(), columns = roomColumns(), span = (w - 24) / columns;
        offset = Math.clamp(offset, 0, Math.max(0, Math.ceilDiv(rooms.size(), columns) - rows));
        for (int i = 0; i < rows * columns && offset * columns + i < rooms.size(); i++) {
            var room = rooms.get(offset * columns + i);
            var host = room.getCompound("host");
            var guest = room.getCompound("guest");
            boolean multi = multi(room);
            boolean ownRoster = room.getBoolean("ownParty");
            boolean open = multi ? (room.getInt("capacity") == 0 || room.getInt("count") < room.getInt("capacity")) : guest.isEmpty();
            int cx = x + 12 + i % columns * span, cy = y + 58 + i / columns * ROOM_STRIDE;
            var label = open ? text("join_room", host.getString("name")) : text("full");
            control(cx, cy, span - 4, ROOM_STRIDE - 4, label, open,
                    () -> request("join", tag -> tag.putUUID("room", room.getUUID("room"))),
                    (g, b, dt) -> {
                        int middle = cx + b.getWidth() / 2, wing = Math.max(23, b.getWidth() / 4);
                        card(g, cx, cy, b.getWidth(), b.getHeight(), open ? CYAN : MUTED, b.isHoveredOrFocused() && open);
                        if (multi) {
                            player(g, host, cx + 10, cy + 11, 23, GOLD);
                            FrontierConsole.text(g, font, Component.literal(host.getString("name")), cx + 39, cy + 13, b.getWidth() - 47, TEXT);
                            FrontierConsole.text(g, font, text("mode_multi"), cx + 39, cy + 26, b.getWidth() - 47, GOLD);
                            center(g, font, text(ownRoster ? "lobby_own_party" : "lobby_any_party"),
                                    middle, cy + 39, b.getWidth() - 16, ownRoster ? PINK : CYAN);
                            center(g, font, money(room), middle, cy + 52, b.getWidth() - 16, GOLD);
                            center(g, font, text("seats", room.getInt("count"), capacityLabel(room)), middle, cy + 65, b.getWidth() - 8, open ? CYAN : MUTED);
                            return;
                        }
                        roomSeat(g, host, cx + wing, cy, wing * 2 - 5, CYAN, dt, false);
                        roomSeat(g, guest, cx + b.getWidth() - wing, cy, wing * 2 - 5, PINK, dt, true);
                        center(g, font, Component.literal("VS"), middle, cy + 12, 28, TEXT);
                        center(g, font, money(room), middle, cy + 26, Math.max(38, b.getWidth() - 52), GOLD);
                        center(g, font, text(open ? "open" : "full"), middle, cy + 65, b.getWidth() - 8, open ? CYAN : MUTED);
                    });
        }
    }

    private void roomSeat(GuiGraphics g, CompoundTag seat, int center, int y, int width, int accent, float dt, boolean mirror) {
        player(g, seat, center - 9, y + 6, 18, accent);
        if (seat.isEmpty()) placeholder(g, font, center - 9, y + 6, 18);
        center(g, font, seat.isEmpty() ? text("vacant") : Component.literal(seat.getString("name")), center, y + 31, width, TEXT);
        var ids = seat.getList("species", Tag.TAG_STRING);
        int size = Math.clamp((width - 4) / 3, 10, 18), begin = center - (size * 3 + 4) / 2;
        for (int i = 0; i < 3; i++) {
            var id = i < ids.size() ? ResourceLocation.tryParse(ids.getString(i)) : null;
            int px = begin + (mirror ? 2 - i : i) * (size + 2);
            if (id == null || FrontierRoster.NONE.equals(id) || !partyFace(g, portraits, id, Set.of(), px, y + 43, size, dt, mirror, accent))
                placeholder(g, font, px, y + 43, size);
        }
    }

    private int centerRail() { return Math.clamp(w / 9, 38, 72); }
    private int teamWidth() { return (w - 24 - centerRail()) / 2; }
    private int teamX(boolean left) { return left ? x + 10 : x + w - 10 - teamWidth(); }

    private boolean tournament() { return state.getInt("round") > 0; }
    private CompoundTag own() { return state.getCompound(tournament() ? "own" : state.getBoolean("owner") ? "host" : "guest"); }
    private CompoundTag rival() { return state.getCompound(tournament() ? "rival" : state.getBoolean("owner") ? "guest" : "host"); }

    private void inviteButton() {
        btn(x + w - 105, y + 7, 95, text("invite"), state.getBoolean("canInvite"), () -> {
            inviteQuery = "";
            page(Page.INVITE);
            request("list", tag -> {});
        });
    }

    private void waiting() {
        btn(x + 9, y + 7, 77, text("leave"), () -> request("leave", tag -> {}));
        inviteButton();
        int middle = x + w / 2, footer = y + h - 27;
        boolean owner = state.getBoolean("owner");
        btn(owner ? middle - 95 : middle - 47, footer, 94, text(state.getBoolean("selfReady") ? "unready" : "ready"),
                () -> request("ready", tag -> {}));
        if (owner) btn(middle + 3, footer, 94, text("start_tournament"), state.getBoolean("canStart"), Action.PRIMARY,
                () -> request("start", tag -> {}));
    }

    private List<CompoundTag> inviteRows() {
        String term = inviteQuery.strip().toLowerCase(Locale.ROOT);
        return state.getList("candidates", Tag.TAG_COMPOUND).stream().map(CompoundTag.class::cast)
                .filter(row -> row.getString("name").toLowerCase(Locale.ROOT).contains(term)).toList();
    }

    private int inviteVisible() { return Math.max(1, (h - 107) / 26); }

    private void invitation() {
        field = edit(x + 16, y + 40, w - 32, inviteQuery, value -> {
            inviteQuery = value;
            offset = 0;
            int cursor = field.getCursorPosition();
            rebuildWidgets();
            setFocused(field);
            field.moveCursorTo(cursor, false);
        });
        field.setHint(text("search"));
        var rows = inviteRows();
        offset = Math.clamp(offset, 0, Math.max(0, rows.size() - inviteVisible()));
        for (int i = 0; i < inviteVisible() && offset + i < rows.size(); i++) {
            var row = rows.get(offset + i);
            int top = y + 67 + i * 26;
            boolean sent = row.getBoolean("sent"), cooldown = row.getBoolean("cooldown");
            btn(x + w - 105, top + 2, 85, text(sent ? "invited" : cooldown ? "invite_cooldown" : "send_invite"),
                    !sent && !cooldown, () -> request("invite", tag -> tag.putUUID("target", row.getUUID("player"))));
        }
    }

    private void drawInvites(GuiGraphics g) {
        var rows = inviteRows();
        if (rows.isEmpty()) center(g, font, text("no_candidates"), x + w / 2, y + 91, w - 40, MUTED);
        for (int i = 0; i < inviteVisible() && offset + i < rows.size(); i++) {
            var row = rows.get(offset + i);
            int top = y + 67 + i * 26;
            card(g, x + 16, top, w - 32, 24, CYAN, false);
            player(g, row, x + 21, top + 3, 17, CYAN);
            FrontierConsole.text(g, font, Component.literal(row.getString("name")), x + 44, top + 8, w - 155, TEXT);
        }
    }

    private static Component capacityLabel(CompoundTag room) {
        int limit = room.getInt("capacity");
        return limit == 0 ? text("capacity_unlimited") : Component.literal(Integer.toString(limit));
    }

    private int waitingRows() { return Math.max(1, (h - 99) / 32); }

    private void drawWaiting(GuiGraphics g) {
        var seats = state.getList("seats", Tag.TAG_COMPOUND);
        int visible = waitingRows();
        int gap = 16, span = (w - 34 - gap) / 2;
        int stride = 32;
        offset = Math.min(offset, Math.max(0, Math.ceilDiv(seats.size(), 2) - visible));
        center(g, font, text("waiting_line", seats.size(), capacityLabel(state), money(state)), x + w / 2, y + 39, w - 36, GOLD);
        int end = Math.min(seats.size() + (seats.size() % 2), (offset + visible) * 2);
        for (int i = offset * 2; i < end; ++i) {
            var seat = seats.getCompound(i);
            int left = x + 17 + i % 2 * (span + gap), top = y + 55 + (i / 2 - offset) * stride;
            int tint = i % 2 == 0 ? CYAN : PINK;
            card(g, left, top, span, stride - 4, tint, false);
            int face = Math.min(23, stride - 11);
            if (seat.isEmpty()) placeholder(g, font, left + 7, top + 4, face);
            else player(g, seat, left + 7, top + 4, face, tint);
            int readyWidth = Math.min(67, span / 3);
            FrontierConsole.text(g, font, seat.isEmpty() ? text("vacant") : Component.literal(seat.getString("name")),
                    left + face + 13, top + (stride - 12) / 2, span - face - readyWidth - 17, TEXT);
            if (!seat.isEmpty()) center(g, font, text(seat.getBoolean("ready") ? "is_ready" : i == 0 ? "host" : "not_ready"),
                    left + span - readyWidth / 2 - 4, top + (stride - 12) / 2, readyWidth, seat.getBoolean("ready") ? tint : MUTED);
        }
    }

    private int bracketDepth() { return Math.clamp(state.getInt("roundTotal"), 1, 30); }
    private int bracketColumns() { return Math.clamp((w - 28) / 132, 1, Math.min(3, bracketDepth())); }
    private int roundColumn() { return Math.max(0, state.getInt("round") - 1); }
    private int bracketMaxColumn() { return bracketDepth() - bracketColumns(); }
    private int bracketTop() { return y + 80; }
    private int bracketBottom() { return y + h - 48; }
    private int bracketFirst() { return 1 << (bracketDepth() - 1); }
    private int bracketStride() {
        return Math.max(39, Math.max(1, bracketBottom() - bracketTop() - 5) / Math.min(4, bracketFirst()));
    }
    private long bracketExtent() { return (long) bracketFirst() * bracketStride() + 45; }
    private long bracketMaxScroll() { return Math.max(0, bracketExtent() - (bracketBottom() - bracketTop())); }
    private long bracketScroll;

    private void bracketButtons() {
        btn(x + 9, y + 7, 78, text("close_world"), this::pinRoom);
        if (state.contains("own")) btn(x + w - 99, y + 7, 89, text("lineup"), true, Action.PRIMARY, () -> { bracketPinned = false; page(Page.ROOM); });
        if (state.getBoolean("finished")) {
            btn(x + w / 2 - 47, y + h - 27, 94, text("done"), () -> request("leave", tag -> {}));
            return;
        }
        bracketColumn = Math.clamp(bracketColumn, 0, bracketMaxColumn());
        if (bracketMaxColumn() > 0) {
            btn(x + 13, y + h - 29, 30, Component.literal("‹"), bracketColumn > 0, () -> shiftBracket(-1));
            btn(x + 47, y + h - 29, 30, Component.literal("›"), bracketColumn < bracketMaxColumn(), () -> shiftBracket(1));
        }
        if (bracketMaxScroll() > 0) {
            btn(x + w - 77, y + h - 29, 30, Component.literal("↑"), () -> scrollBracket(-Math.max(39, bracketBottom() - bracketTop() - 39)));
            btn(x + w - 43, y + h - 29, 30, Component.literal("↓"), () -> scrollBracket(Math.max(39, bracketBottom() - bracketTop() - 39)));
        }
    }

    private void shiftBracket(int delta) {
        bracketColumn = Math.clamp(bracketColumn + delta, 0, bracketMaxColumn());
        rebuildWidgets();
    }

    private void scrollBracket(long pixels) { bracketScroll = Math.clamp(bracketScroll + pixels, 0, bracketMaxScroll()); }

    private void drawBracket(GuiGraphics g) {
        if (state.getBoolean("finished")) {
            drawAwards(g);
            return;
        }
        var opponent = state.getCompound("currentOpponent");
        var heading = opponent.isEmpty()
                ? text(state.getBoolean("bye") ? "bye" : "no_round_opponent", state.getInt("round"))
                : text("round_opponent", state.getInt("round"), opponent.getString("name"));
        card(g, x + 12, y + 31, w - 24, 22, GOLD, false);
        center(g, font, heading, x + w / 2, y + 38, w - 44, GOLD);

        var rounds = state.getList("rounds", Tag.TAG_COMPOUND);
        int depth = bracketDepth(), columns = bracketColumns();
        int span = (w - 29) / columns, cardW = span - 13;
        int stride = bracketStride(), rowH = Math.min(stride - 5, 43);
        scrollBracket(0);
        for (int col = bracketColumn; col < bracketColumn + columns; col++) {
            int left = x + 14 + (col - bracketColumn) * span;
            center(g, font, text(col + 1 == depth ? "final_round" : "round", col + 1), left + cardW / 2, y + 64,
                    cardW, col + 1 == state.getInt("round") ? GOLD : MUTED);
        }
        g.enableScissor(x + 10, bracketTop(), x + w - 12, bracketBottom());
        try {
            int viewport = bracketBottom() - bracketTop();
            for (int col = bracketColumn; col < bracketColumn + columns; col++) {
                int left = x + 14 + (col - bracketColumn) * span;
                var bouts = rounds.getCompound(col).getList("bouts", Tag.TAG_COMPOUND);
                long step = (long) stride << col;
                int count = bracketFirst() >> col;
                // Visit the visible cards and their neighboring connectors, regardless of bracket size.
                int start = (int) Math.max(0, bracketScroll / step - 2);
                int end = (int) Math.min(count, (bracketScroll + viewport) / step + 3);
                for (int row = start; row < end; row++) {
                    long middle = step * row + step / 2 - bracketScroll;
                    int mid = bracketTop() + (int) Math.clamp(middle, -1_000_000, 1_000_000);
                    if (col + 1 < bracketColumn + columns) {
                        long join = step * (2L * (row / 2) + 1) - bracketScroll;
                        int to = bracketTop() + (int) Math.clamp(join, -1_000_000, 1_000_000);
                        g.fill(left + cardW, mid, left + cardW + 7, mid + 1, NEUTRAL);
                        g.fill(left + cardW + 6, Math.min(mid, to), left + cardW + 7, Math.max(mid, to) + 1, NEUTRAL);
                        g.fill(left + cardW + 7, to, left + span, to + 1, NEUTRAL);
                    }
                    if (middle > -rowH && middle < viewport + rowH)
                        drawBout(g, bouts.getCompound(row), left, mid - rowH / 2, cardW, rowH);
                }
                if (col == depth - 1 && state.getInt("count") >= 4) {
                    long bronze = (long) stride * bracketFirst() - bracketScroll + 18;
                    if (bronze > -rowH && bronze < viewport + rowH) {
                        int middle = bracketTop() + (int) bronze;
                        center(g, font, text("bronze"), left + cardW / 2, middle - rowH / 2 - 11, cardW, MUTED);
                        drawBout(g, bouts.getCompound(1), left, middle - rowH / 2, cardW, rowH);
                    }
                }
            }
        } finally { g.disableScissor(); }
        if (bracketMaxScroll() > 0) {
            int viewport = bracketBottom() - bracketTop();
            int thumb = Math.max(9, (int) ((long) viewport * viewport / bracketExtent()));
            int top = bracketTop() + (int) ((viewport - thumb) * (double) bracketScroll / bracketMaxScroll());
            g.fill(x + w - 10, bracketTop(), x + w - 7, bracketBottom(), 0xff242922);
            g.fill(x + w - 10, top, x + w - 7, top + thumb, GOLD);
        }
        center(g, font, text(state.getBoolean("roundOpened") ? "round_progress" : "round_next"), x + w / 2, y + h - 24, w - 163, GOLD);
    }

    private void drawAwards(GuiGraphics g) {
        var awards = state.getList("awards", Tag.TAG_COMPOUND);
        if (awards.isEmpty()) center(g, font, text("refunded"), x + w / 2, y + h / 2, w - 32, GOLD);
        int left = x + 13, span = w - 26;
        int stride = Math.min(29, (h - 67) / Math.max(1, awards.size()));
        for (int i = 0; i < awards.size(); i++) {
            var award = awards.getCompound(i);
            int top = y + 35 + i * stride;
            int face = Math.min(19, stride - 7);
            card(g, left, top, span, stride - 3, i == 0 ? GOLD : CYAN, false);
            FrontierConsole.text(g, font, Component.literal("#" + (i + 1)), left + 7, top + (stride - 11) / 2, 21, GOLD);
            player(g, award, left + 29, top + 3, face, CYAN);
            FrontierConsole.text(g, font, Component.literal(award.getString("name")), left + 34 + face, top + (stride - 11) / 2, span / 2 - 35 - face, TEXT);
            center(g, font, BigIntegerExtensionsKt.formatFull(new BigInteger(award.getString("prize")), true),
                    left + span * 3 / 4, top + (stride - 11) / 2, span / 2 - 8, GOLD);
        }
    }

    private void drawBout(GuiGraphics g, CompoundTag bout, int left, int top, int width, int height) {
        int accent = bout.getBoolean("running") ? CYAN : bout.getBoolean("done") ? GOLD : NEUTRAL;
        card(g, left, top, width, height, accent, false);
        for (int i = 0; i < 2; i++) {
            var seat = bout.getCompound(i == 0 ? "left" : "right");
            int y = top + 3 + i * (height - 6) / 2, face = Math.min(14, (height - 7) / 2);
            boolean winner = seat.hasUUID("player") && bout.hasUUID("winner") && bout.getUUID("winner").equals(seat.getUUID("player"));
            if (!seat.isEmpty()) player(g, seat, left + 5, y, face, winner ? GOLD : accent);
            FrontierConsole.text(g, font, seat.isEmpty() ? text(bout.getBoolean("done") ? "bye" : "tbd") : Component.literal(seat.getString("name")),
                    left + face + 10, y + (face - 8) / 2, width - face - 15, winner ? GOLD : seat.isEmpty() ? MUTED : TEXT);
        }
    }

    private FrontierRoster.Pick get(CompoundTag seat, int i) {
        var picks = seat.getList("picks", Tag.TAG_COMPOUND);
        return i >= picks.size() ? FrontierRoster.EMPTY : Objects.requireNonNullElse(FrontierRooms.decode(picks.getCompound(i)), FrontierRoster.EMPTY);
    }

    private int lineupHeight() { return (h - 95) / 3; }

    private static final int[] SLOT_COLORS = {0xffffcc76, 0xff80dfff, 0xffc8a4ff};

    private void lineups() {
        var own = own();
        var rival = rival();
        boolean editable = !state.getBoolean("ownParty") && !own.getBoolean("ready");
        for (boolean left : new boolean[]{true, false}) {
            var seat = left ? own : rival;
            int accent = left ? CYAN : PINK;
            int cx = teamX(left), cw = teamWidth(), ch = lineupHeight();
            for (int i = 0; i < FrontierRoster.TEAM_SIZE; i++) {
                lineupCard(cx, y + 66 + i * ch, cw, ch - 3, i, get(seat, i), left, accent, left && editable);
            }
        }

        if (tournament()) {
            btn(x + 9, y + 7, 77, text("close_world"), this::pinRoom);
            btn(x + w - 97, y + 7, 87, text("bracket"), () -> { bracketPinned = true; page(Page.BRACKET); });
        } else {
            btn(x + 9, y + 7, 77, text("leave"), () -> request("leave", tag -> {}));
            inviteButton();
        }
        boolean selected = IntStream.range(0, FrontierRoster.TEAM_SIZE)
                .anyMatch(i -> !FrontierRoster.NONE.equals(get(own, i).species()));
        boolean owner = !tournament() && state.getBoolean("owner"), ready = own.getBoolean("ready");
        int middle = x + w / 2, footer = y + h - 27;
        btn(owner ? middle - 95 : middle - 47, footer, 94, text(ready ? "unready" : "ready"), selected || ready, () -> request("ready", tag -> {}));
        if (owner) btn(middle + 3, footer, 94, text("start"), ready && rival.getBoolean("ready"), Action.PRIMARY, () -> request("start", tag -> {}));
        if (move >= 0) moveChooser(get(own, slot));
    }

    private void lineupCard(int cx, int cy, int cw, int ch, int index, FrontierRoster.Pick pick,
                            boolean left, int accent, boolean edit) {
        var species = FrontierRoster.species(pick.species());
        int innerY = cy + 4;
        int innerH = ch - 8;
        int itemSize = Math.min(Math.clamp(cw / 8, 12, 28), Math.max(12, edit ? innerH - 21 : innerH));
        int toolSize = Math.min(itemSize, 17);
        int itemColumnH = itemSize + (edit ? toolSize + 4 : 0);
        int portraitW = Math.max(0, Math.min(68, Math.min(cw / 4 + 3, innerH - 2)));
        int badgeW = cw >= 155 ? 24 : 20;
        int emptyBadgeSpace = species == null ? badgeW + 4 : 0;
        int contentX = cx + 5 + (left ? emptyBadgeSpace : 0);
        int contentW = Math.max(1, cw - emptyBadgeSpace - 10);
        int faceX = left ? contentX : contentX + contentW - portraitW;
        int faceY = innerY + (innerH - portraitW) / 2;
        int itemX = left ? faceX + portraitW + 3 : faceX - itemSize - 3;
        int detailsX = left ? itemX + itemSize + 4 : contentX;
        int detailsW = Math.max(1, contentW - portraitW - itemSize - 7);
        int itemColumnY = innerY + Math.max(0, (innerH - itemColumnH) / 2);
        int itemY = itemColumnY + itemColumnH - itemSize;
        var held = itemStack(pick.held());
        addRenderableOnly((g, mx, my, dt) -> {
            card(g, cx, cy, cw, ch, accent, false);
            if (species == null) {
                drawSlotBadge(g, cx + (left ? 5 : cw - badgeW - 5), innerY, badgeW, index);
                if (!edit) center(g, font, text("empty"), contentX + contentW / 2, cy + (ch - 8) / 2, contentW - 6, MUTED);
                return;
            }
            if (portraitW > 4) partyFace(g, portraits, pick.species(), Set.of(), faceX, faceY,
                    portraitW, dt, !left, accent);
            drawSlotBadge(g, faceX + (left ? 1 : Math.max(0, portraitW - badgeW - 1)), faceY + 1, badgeW, index);
        });
        if (species == null) {
            if (edit) control(contentX, innerY, contentW, innerH, text("choose_species"), true,
                    () -> choose(Kind.SPECIES, index), (g, b, dt) -> {
                        card(g, contentX, innerY, contentW, innerH, CYAN, b.isHoveredOrFocused());
                        center(g, font, b.getMessage(), contentX + contentW / 2, innerY + (innerH - 8) / 2, contentW - 6, TEXT);
                        pulse(g, contentX, innerY, contentW, innerH);
                    });
            return;
        }
        if (edit) control(itemX + (itemSize - toolSize) / 2, itemColumnY, toolSize, toolSize, text("switch_species"), true, true,
                () -> choose(Kind.SPECIES, index), (g, b, dt) -> OreUiRenderer.labeledButton(g, font, Component.literal("⇄"),
                        b.getX(), b.getY(), toolSize, toolSize, Action.SECONDARY, b.isHoveredOrFocused(), true, 1, accent));

        var heldHint = held.isEmpty() ? text(edit ? "choose_held" : "no_held_item") : held.getHoverName();
        var heldTip = !held.isEmpty() && edit ? heldHint.copy().append("\n").append(text("switch_held")) : heldHint;
        control(itemX, itemY, itemSize, itemSize, heldTip, edit, true, () -> choose(Kind.HELD, index), (g, b, dt) -> {
            OreUiRenderer.field(g, itemX, itemY, itemSize, itemSize, edit && b.isHoveredOrFocused(), 1, accent);
            if (held.isEmpty()) {
                center(g, font, Component.literal(edit ? "+" : "–"), itemX + itemSize / 2, itemY + (itemSize - 8) / 2, itemSize - 4, MUTED);
                if (edit) pulse(g, itemX, itemY, itemSize, itemSize);
            } else {
                int size = Math.min(itemSize - 6, 24);
                FrontierConsole.item(g, held, itemX + (itemSize - size) / 2, itemY + (itemSize - size) / 2, size);
            }
        });
        lineupMoves(detailsX, innerY, detailsW, innerH, index, pick, accent, edit);
    }

    private void lineupMoves(int x, int y, int width, int height, int teamSlot, FrontierRoster.Pick pick,
                             int accent, boolean edit) {
        int count = FrontierRoster.MOVE_SLOTS;
        int stride = Math.max(1, height / count);
        int rowH = Math.max(1, stride - 2);
        int tool = edit ? Math.clamp(width / 7, 7, 12) : 0;
        int inset = edit ? tool + 2 : 0;
        int nameW = Math.max(1, width - inset * 2);
        for (int i = 0; i < count; i++) {
            int index = i, rowY = y + i * stride;
            String id = pick.moves().get(i);
            var template = id.isEmpty() ? null : Moves.getByName(id);
            Component name = id.isEmpty() ? Component.literal("–") : moveName(id);
            boolean selected = edit && slot == teamSlot && move == index;
            addRenderableOnly((g, mx, my, dt) -> {
                int nameX = x + inset;
                card(g, nameX, rowY, nameW, rowH, accent, selected);
                int iconSize = template == null ? 0 : Math.max(0, Math.min(10, Math.min(rowH - 5, nameW - 10)));
                int nameInset = 3;
                if (iconSize > 0) {
                    typeIcon(g, template.getElementalType(), nameX + 3, rowY + (rowH - 2 - iconSize) / 2, iconSize);
                    nameInset += iconSize + 3;
                }
                int textHeight = Math.max(1, rowH - 4);
                float scale = Math.min(1, textHeight / 8F);
                g.pose().pushPose();
                try {
                    g.pose().translate(nameX + nameInset, rowY + 1 + (textHeight - 8 * scale) / 2, 0);
                    g.pose().scale(scale, scale, 1);
                    FrontierConsole.text(g, font, name, 0, 0, (int) ((nameW - nameInset - 3) / scale), id.isEmpty() ? MUTED : TEXT);
                } finally { g.pose().popPose(); }
                if (!id.isEmpty() && move < 0 && !megaLimitOpen && inside(mx, my, nameX, rowY, nameW, rowH)) {
                    setTooltipForNextRenderPass(List.of(name.getVisualOrderText()));
                }
            });
            if (!edit) continue;
            int half = Math.max(1, rowH / 2), swapX = x + width - tool;
            moveToolButton(x, rowY, tool, half, -1, text("move_up"), i > 0 && !id.isEmpty(),
                    () -> reorder(teamSlot, index, index - 1, pick));
            moveToolButton(x, rowY + half, tool, Math.max(1, rowH - half), 1, text("move_down"),
                    i < count - 1 && !id.isEmpty(), () -> reorder(teamSlot, index, index + 1, pick));
            moveToolButton(swapX, rowY, tool, rowH, 0, text("move_swap"), true, () -> {
                if (pending) return;
                slot = teamSlot;
                move = index;
                moveAnchorX = swapX + tool + 4;
                moveAnchorY = rowY;
                moveOffset = 0;
                moveQuery = "";
                rebuildWidgets();
            });
        }
    }

    private void drawSlotBadge(GuiGraphics g, int x, int y, int width, int slot) {
        int color = SLOT_COLORS[slot];
        g.pose().pushPose();
        try {
            g.pose().translate(0, 0, 100);
            card(g, x, y, width, 18, color, true);
            g.pose().translate(x + width / 2F, y + 4, 1);
            g.pose().scale(1.35F, 1.35F, 1);
            g.drawCenteredString(font, Component.literal((slot + 1) + "P"), 0, 0, color);
        } finally { g.pose().popPose(); }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void pulse(GuiGraphics g, int x, int y, int width, int height) {
        TutorialPulse.render(g, x, y, width, height);
    }

    private int moveStride() { return 20; }
    private int moveListW() { return Math.min(201, width - 12); }
    private int moveListX() { return Math.clamp(moveAnchorX, 6, width - moveListW() - 6); }
    private int moveListY() { return Math.clamp(moveAnchorY, 6, Math.max(6, height - moveListHeight() - 6)); }
    private int moveListRows() { return Math.clamp((height - 61) / moveStride(), 1, 7); }
    private int moveListHeight() { return 49 + moveListRows() * moveStride(); }

    private List<String> candidates(FrontierRoster.Pick pick) {
        String query = moveQuery.toLowerCase(Locale.ROOT).strip();
        return FrontierRoster.legalMoves(FrontierRoster.species(pick.species())).stream().map(t -> t.getName())
                .filter(id -> !id.equals("struggle") && !pick.moves().contains(id))
                .filter(id -> query.isEmpty() || id.contains(query) || moveName(id).getString().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(id -> moveName(id).getString())).toList();
    }

    private void reorder(int teamSlot, int index, int destination, FrontierRoster.Pick pick) {
        if (pending || move >= 0) return;
        var moves = new ArrayList<>(pick.moves());
        Collections.swap(moves, index, destination);
        submitPick(teamSlot, new FrontierRoster.Pick(pick.species(), pick.held(), moves));
    }

    private void moveChooser(FrontierRoster.Pick pick) {
        int bx = moveListX(), by = moveListY(), bw = moveListW(), rows = moveListRows();
        int listY = by + 42;
        addRenderableOnly((g, mx, my, dt) -> {
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            try {
                var list = candidates(pick);
                int total = list.size(), track = rows * moveStride();
                moveOffset = Math.clamp(moveOffset, 0, Math.max(0, total - rows));
                OreUiRenderer.panel(g, bx, by, bw, moveListHeight(), 1, NEUTRAL);
                FrontierConsole.text(g, font, text("replace_move", move + 1), bx + 7, by + 6, bw - 14, TEXT);
                if (total == 0) FrontierConsole.text(g, font, text("no_replacements"), bx + 6, listY + 4, bw - 12, MUTED);
                for (int i = 0; i < rows && moveOffset + i < total; i++) {
                    var option = list.get(moveOffset + i);
                    int ry = listY + i * moveStride();
                    moveStrip(g, font, option, moveName(option), bx + 5, ry, bw - 17, moveStride() - 2,
                            inside(mx, my, bx + 5, ry, bw - 17, moveStride() - 2));
                }
                if (total > rows) {
                    int thumb = Math.max(8, track * rows / total);
                    OreUiRenderer.scrollbar(g, bx + bw - 9, listY, 6, track, (track - thumb) * moveOffset / (total - rows), thumb, false, false, 1, NEUTRAL);
                }
            } finally { g.pose().popPose(); }
        });
        field = edit(bx + 5, by + 19, bw - 10, moveQuery, value -> { moveQuery = value; moveOffset = 0; });
        field.setHint(text("search_moves"));
        setFocused(field);
    }

    private void moveToolButton(int x, int y, int w, int h, int direction, Component label, boolean enabled, Runnable click) {
        control(x, y, w, h, label, enabled, true, click,
                (g, b, dt) -> moveTool(g, x, y, w, h, direction, enabled, b.isHoveredOrFocused()));
    }

    private void choose(Kind kind, int slot) {
        this.kind = kind;
        this.slot = slot;
        itemFilter = ItemsFilter.ALL;
        generation = 0;
        generationsOpen = false;
        filter = "";
        buildOptions();
        page(Page.PICKER);
    }

    private void buildOptions() {
        var old = get(own(), slot);
        var list = new ArrayList<Option>();
        switch (kind) {
            case SPECIES -> {
                PokemonSpecies.getImplemented().stream().filter(species -> generation == 0 || FrontierRoster.generation(species) == generation)
                        .sorted(Comparator.comparingInt(species -> species.getNationalPokedexNumber())).forEach(species -> {
                            var id = species.getResourceIdentifier();
                            list.add(new Option(species.getTranslatedName(), id.toString(),
                                    () -> editPick(id.equals(old.species()) ? old : FrontierRoster.initialPick(species))));
                        });
            }
            case HELD -> {
                for (var id : FrontierRoster.heldItems().stream()
                        .sorted(Comparator.comparingInt(FrontierScreen::heldGroup).thenComparing(ResourceLocation::getPath)
                                .thenComparing(ResourceLocation::getNamespace)).toList()) {
                    var item = itemStack(id);
                    if (itemFilter == ItemsFilter.MEGA && !FrontierRoster.isMega(item) || itemFilter == ItemsFilter.HELD && FrontierRoster.isMega(item)) continue;
                    var label = item.getHoverName();
                    list.add(new Option(label, id.toString(), () -> editPick(new FrontierRoster.Pick(old.species(), id, old.moves()))));
                }
            }
        }
        options = List.copyOf(list);
    }

    private void submitPick(int index, FrontierRoster.Pick pick) {
        request("edit", tag -> { tag.putInt("slot", index); tag.put("pick", FrontierRooms.encode(pick)); });
    }

    private void editPick(FrontierRoster.Pick pick) {
        submitPick(slot, pick);
        page(Page.ROOM);
    }

    private List<Option> filtered() {
        String query = filter.toLowerCase(Locale.ROOT).strip();
        return query.isEmpty() ? options : options.stream().filter(opt -> opt.id.contains(query)
                || opt.name.getString().toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private int pickerX() { return x + 12; }
    private int pickerWidth() { return x + w - 12 - pickerX(); }
    private int pickerColumns() { return kind == Kind.SPECIES ? Math.clamp(pickerWidth() / 54, 2, 9) : Math.max(1, pickerWidth() / 22); }
    private int pickerStride() { return kind == Kind.SPECIES ? 49 : 22; }
    private int pickerTop() { return y + (kind == Kind.HELD ? 87 : 63); }
    private int pickerRows() { return Math.max(1, (y + h - pickerTop() - 33) / pickerStride()); }
    private int pickerCellWidth() { return kind == Kind.HELD ? 22 : pickerWidth() / pickerColumns(); }

    private void picker() {
        var pick = get(own(), slot);
        boolean species = kind == Kind.SPECIES;
        boolean remove = !(species ? FrontierRoster.NONE.equals(pick.species()) : FrontierRoster.NONE.equals(pick.held()));
        int generationWidth = species ? 85 : 0;
        int removeW = species && remove ? 94 : 0;
        field = edit(x + 12, y + 37, w - 24 - generationWidth - removeW, filter, value -> { filter = value; offset = 0; });
        field.setHint(text("search"));
        if (remove) {
            btn(species ? x + w - 12 - generationWidth - removeW + 3 : x + w - 118,
                    species ? y + 36 : y + h - 27, species ? removeW - 6 : 110,
                    text(species ? "remove_species" : "remove_held"), true, Action.DANGER,
                    () -> editPick(species ? FrontierRoster.EMPTY : new FrontierRoster.Pick(pick.species(), FrontierRoster.NONE, pick.moves())));
        }
        if (kind == Kind.HELD) {
            int tw = (w - 24) / 3;
            for (var category : ItemsFilter.values()) {
                var label = text("items." + category.name().toLowerCase(Locale.ROOT));
                btn(x + 12 + category.ordinal() * tw, y + 61, tw - 3, label, itemFilter != category, () -> {
                    itemFilter = category;
                    buildOptions();
                    offset = 0;
                    rebuildWidgets();
                });
            }
        }
        if (species) {
            var label = generation == 0 ? text("all_generations") : text("generation", generation);
            btn(generationX(), y + 36, 82, label.copy().append(" ▾"), () -> {
                generationsOpen = !generationsOpen;
                rebuildWidgets();
            });
        }
    }

    private int generationX() { return x + w - 94; }
    private int generationRow() { return Math.clamp((height - y - 59) / 10, 10, 19); }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (megaLimitOpen) { super.mouseClicked(mx, my, button); return true; }
        if (page == Page.CREATE && createMenu != null && button == 0) {
            int menuTop = createRow(createMenu == CreateMenu.MODE ? 0 : 1) + 20;
            if (inside(mx, my, createInputX(), menuTop, createInputWidth(), 40)) {
                selectCreateOption((int) (my - menuTop) / 20);
            } else createMenu = null;
            return true;
        }
        if (page == Page.PICKER && generationsOpen && button == 0 && !inside(mx, my, generationX(), y + 36, 82, 20)) {
            if (inside(mx, my, generationX(), y + 57, 82, generationRow() * 10)) {
                generation = (int) ((my - y - 57) / generationRow());
                buildOptions();
                offset = 0;
            }
            generationsOpen = false;
            rebuildWidgets();
            return true;
        }
        if (page == Page.ROOM && move >= 0) {
            if (button != 0) return true;
            if (field.mouseClicked(mx, my, button)) { setFocused(field); return true; }
            int listY = moveListY() + 42;
            if (inside(mx, my, moveListX() + 5, listY, moveListW() - 17, moveListRows() * moveStride())) {
                var pick = get(own(), slot);
                var list = candidates(pick);
                int i = moveOffset + (int) (my - listY) / moveStride();
                if (!pending && i < list.size() && (int) (my - listY) % moveStride() < moveStride() - 2) {
                    var moves = new ArrayList<>(pick.moves());
                    moves.set(move, list.get(i));
                    move = -1;
                    submitPick(slot, new FrontierRoster.Pick(pick.species(), pick.held(), moves));
                    rebuildWidgets();
                }
            } else if (!inside(mx, my, moveListX(), moveListY(), moveListW(), moveListHeight())) {
                move = -1;
                rebuildWidgets();
            }
            return true;
        }
        int cell = page == Page.PICKER ? pickerCellWidth() : 1;
        if (page == Page.PICKER && button == 0 && mx >= pickerX() && mx < pickerX() + cell * pickerColumns() && my >= pickerTop() && my < pickerTop() + pickerRows() * pickerStride()) {
            var list = filtered();
            int i = (offset + (int) ((my - pickerTop()) / pickerStride())) * pickerColumns() + (int) ((mx - pickerX()) / cell);
            if (i < list.size() && !pending && !own().getBoolean("ready")) { list.get(i).apply.run(); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (megaLimitOpen) return true;
        if (page == Page.ROOM && move >= 0 && dy != 0 && inside(mx, my, moveListX(), moveListY(), moveListW(), moveListHeight())) {
            moveOffset = Math.clamp(moveOffset + (dy < 0 ? 1 : -1), 0, Math.max(0, candidates(get(own(), slot)).size() - moveListRows()));
            return true;
        }
        if (generationsOpen) return true;
        if (page == Page.BRACKET && !state.getBoolean("finished")) {
            if (dx != 0) shiftBracket(dx > 0 ? 1 : -1);
            if (dy != 0) scrollBracket(dy < 0 ? bracketStride() : -bracketStride());
            return true;
        }
        if (page == Page.WAITING && dy != 0) {
            int count = state.getList("seats", Tag.TAG_COMPOUND).size();
            offset = Math.clamp(offset + (dy < 0 ? 1 : -1), 0, Math.max(0, Math.ceilDiv(count, 2) - waitingRows()));
            return true;
        }
        if (page == Page.INVITE && dy != 0) {
            offset = Math.clamp(offset + (dy < 0 ? 1 : -1), 0, Math.max(0, inviteRows().size() - inviteVisible()));
            rebuildWidgets();
            return true;
        }
        if (page != Page.LOBBY && page != Page.PICKER || dy == 0) return super.mouseScrolled(mx, my, dx, dy);
        int rows = page == Page.LOBBY ? roomRows() : pickerRows();
        int count = page == Page.LOBBY ? Math.ceilDiv(listings().size(), roomColumns()) : Math.ceilDiv(filtered().size(), pickerColumns());
        offset = Math.clamp(offset + (dy < 0 ? 1 : -1), 0, Math.max(0, count - rows));
        if (page == Page.LOBBY) rebuildWidgets();
        return true;
    }

    private interface Paint { void render(GuiGraphics g, Button b, float tick); }

    private Button control(int x, int y, int w, int h, Component label, boolean enabled, Runnable click, Paint paint) {
        return control(x, y, w, h, label, enabled, false, click, paint);
    }

    private Button control(int x, int y, int w, int h, Component label, boolean enabled, boolean tooltip, Runnable click, Paint paint) {
        var button = new Button(x, y, Math.max(1, w), h, label, b -> click.run(), narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float tick) { paint.render(g, this, tick); }
        };
        if (megaLimitOpen) return addRenderableOnly(button);
        if (tooltip && move < 0) button.setTooltip(Tooltip.create(label));
        button.active = enabled;
        return addRenderableWidget(button);
    }

    private void btn(int x, int y, int w, Component label, Runnable click) {
        btn(x, y, w, label, true, click);
    }

    private void btn(int x, int y, int w, Component label, boolean enabled, Runnable click) {
        btn(x, y, w, label, enabled, Action.SECONDARY, click);
    }

    private void btn(int x, int y, int w, Component label, boolean enabled, Action style, Runnable click) {
        control(x, y, w, 20, label, enabled, click, (g, b, tick) ->
                OreUiRenderer.labeledButton(g, font, label, x, y, w, 20, style, b.isHoveredOrFocused(), enabled, 1, style == Action.PRIMARY ? 0xff4b8b30 : NEUTRAL));
    }

    private static final TagKey<Item> EVOLUTION_STONES =
            TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("cobblemon", "evolution_stones"));

    private static int heldGroup(ResourceLocation id) {
        var item = itemStack(id);
        return FrontierRoster.isMega(item) ? 2 : item.is(EVOLUTION_STONES) ? 1 : 0;
    }

    private static ItemStack itemStack(ResourceLocation id) {
        return id == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR).getDefaultInstance();
    }

    private EditBox edit(int x, int y, int width, String value, Consumer<String> responder) {
        var field = new EditBox(font, x + 5, y, Math.max(1, width - 10), 18, text("search")) {
            @Override public void renderWidget(GuiGraphics g, int mx, int my, float dt) {
                g.pose().pushPose();
                try {
                    if (page == Page.ROOM && move >= 0) g.pose().translate(0, 0, 301);
                    OreUiRenderer.field(g, x, y, width, 18, isFocused(), 1, NEUTRAL);
                    super.renderWidget(g, mx, my, dt);
                } finally { g.pose().popPose(); }
            }
        };
        field.setBordered(false);
        // A borderless EditBox draws text at its origin rather than vertically centering it.
        field.setY(y + 5);
        field.setMaxLength(96);
        field.setValue(value);
        field.setResponder(responder);
        addRenderableWidget(field);
        return field;
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
        // Draw the console's own backdrop; vanilla's menu blur also clears depth.
    }

    @Override public void render(GuiGraphics g, int mx, int my, float delta) {
        shell(g, x, y, w, h);
        if (page == Page.LOBBY) FrontierConsole.text(g, font, title, x + 13, y + 12, w / 3, CYAN);
        center(g, font, text(page == Page.PICKER ? kind == Kind.SPECIES ? "picker_species" : "picker_held"
                : page == Page.BRACKET && state.getBoolean("finished") ? "panel.awards" : "panel." + page.name().toLowerCase(Locale.ROOT)),
                x + w / 2, y + 12, w / 3, TEXT);
        var balance = text("balance", state.getString("balance"));
        int balanceW = Math.min(w / 3 - 7, font.width(balance));
        if (page == Page.CREATE || page == Page.PICKER) FrontierConsole.text(g, font, balance, x + w - 13 - balanceW, y + 12, balanceW, GOLD);
        switch (page) {
            case CREATE -> drawCreateForm(g);
            case LOBBY -> drawLobby(g);
            case ROOM -> drawSeats(g);
            case PICKER -> drawPicker(g, mx, my, delta);
            case WAITING -> drawWaiting(g);
            case BRACKET -> drawBracket(g);
            case INVITE -> drawInvites(g);
        }
        super.render(g, mx, my, delta);
        if (page == Page.PICKER && generationsOpen) drawGenerations(g, mx, my);
        if (page == Page.CREATE && createMenu != null) drawCreateMenu(g, mx, my);
        var error = state.getString("problem");
        if (!error.isEmpty() && !"mega_limit".equals(error) && !megaLimitOpen) {
            var message = text("error." + error);
            center(g, font, message, x + w / 2, y + h - 43, w - 24, 0xffffaa94);
        }
    }

    private void drawLobby(GuiGraphics g) {
        var rooms = listings();
        var balance = text("balance", state.getString("balance"));
        int bw = Math.min(w / 2 - 14, font.width(balance));
        FrontierConsole.text(g, font, balance, x + w - 12 - bw, y + 33, bw, GOLD);
        if (rooms.isEmpty()) {
            card(g, x + 12, y + 58, w - 24, 45, CYAN, false);
            center(g, font, text("no_rooms"), x + w / 2, y + 76, w - 32, MUTED);
        }
    }

    private void dismissMegaLimit() {
        megaLimitOpen = false;
        rebuildWidgets();
    }

    private void megaLimitDialog() {
        int dw = Math.min(230, width - 16);
        int dx = (width - dw) / 2;
        var lines = font.split(text("error.mega_limit"), dw - 28);
        int dh = 66 + lines.size() * 10;
        int dy = (height - dh) / 2;
        var ok = Button.builder(CommonComponents.GUI_OK, button -> dismissMegaLimit())
                .bounds(width / 2 - 42, dy + dh - 28, 84, 20).build();
        addWidget(ok);
        setInitialFocus(ok);
        addRenderableOnly((g, mx, my, delta) -> {
            g.pose().pushPose();
            try {
                g.pose().translate(0, 0, 400);
                g.fill(0, 0, width, height, 0xa0000000);
                OreUiRenderer.panel(g, dx, dy, dw, dh, 1, NEUTRAL);
                center(g, font, text("mega_limit_title"), width / 2, dy + 12, dw - 24, GOLD);
                for (int i = 0; i < lines.size(); i++) {
                    g.drawCenteredString(font, lines.get(i), width / 2, dy + 30 + i * 10, TEXT);
                }
                OreUiRenderer.labeledButton(g, font, ok.getMessage(), ok.getX(), ok.getY(), ok.getWidth(), ok.getHeight(),
                        Action.PRIMARY, ok.isFocused() || inside(mx, my, ok.getX(), ok.getY(), ok.getWidth(), ok.getHeight()), true, 1, NEUTRAL);
            } finally { g.pose().popPose(); }
        });
    }

    private static Component money(CompoundTag data) {
        var amount = data.getString("stake");
        return BigIntegerExtensionsKt.formatFull(amount.isEmpty() ? BigInteger.ZERO : new BigInteger(amount), true);
    }

    private void drawSeats(GuiGraphics g) {
        for (boolean left : new boolean[]{true, false}) {
            int sx = teamX(left), ww = teamWidth(), accent = left ? CYAN : PINK;
            var seat = left ? own() : rival();
            card(g, sx, y + 36, ww, 26, accent, false);
            int avatarX = left ? sx + 5 : sx + ww - 21;
            player(g, seat, avatarX, y + 41, 16, accent);
            var name = seat.isEmpty() ? text("waiting") : text(seat.getBoolean("ready") ? "seat_ready" : "seat_edit", seat.getString("name"));
            center(g, font, name, sx + ww / 2, y + 45, ww - 46, seat.getBoolean("ready") ? accent : TEXT);
        }
        int middle = x + w / 2, rail = centerRail(), top = y + 33;
        int railH = h - 66, midY = top + railH / 2;
        card(g, middle - rail / 2, top, rail, railH, GOLD, false);
        var vs = Component.literal("VS").withStyle(ChatFormatting.BOLD);
        float scale = Math.min(4F, (rail - 10) / (float) font.width(vs));
        g.pose().pushPose();
        try {
            g.pose().translate(middle, midY - 24, 0);
            g.pose().scale(scale, scale, 1);
            g.drawCenteredString(font, vs, 0, 0, GOLD);
        } finally { g.pose().popPose(); }
        center(g, font, text("stake_label"), middle, midY + 12, rail - 6, MUTED);
        center(g, font, money(state), middle, midY + 25, rail - 4, GOLD);
        if (state.getBoolean("ownParty")) center(g, font, text("own_party_short"), middle, midY + 42, rail - 5, TEXT);
    }

    private void drawPicker(GuiGraphics g, int mx, int my, float dt) {
        var list = filtered();
        int columns = pickerColumns(), rows = pickerRows();
        int stride = pickerStride(), cell = pickerCellWidth();
        offset = Math.clamp(offset, 0, Math.max(0, Math.ceilDiv(list.size(), columns) - rows));
        for (int i = 0; i < rows * columns && offset * columns + i < list.size(); i++) {
            int at = pickerTop() + i / columns * stride, bx = pickerX() + i % columns * cell;
            var choice = list.get(offset * columns + i);
            boolean hover = !generationsOpen && inside(mx, my, bx, at, cell - 1, stride - 1);
            if (kind == Kind.SPECIES) {
                card(g, bx, at, cell - 2, stride - 2, CYAN, hover);
                int size = Math.min(29, cell - 6);
                var species = ResourceLocation.tryParse(choice.id);
                if (species != null) portraits.draw(g, species, Set.of(), bx + (cell - 2 - size) / 2, at + 2, size, dt);
                center(g, font, choice.name, bx + (cell - 2) / 2, at + stride - 15, cell - 8, TEXT);
            } else {
                var stack = itemStack(ResourceLocation.tryParse(choice.id));
                OreUiRenderer.field(g, bx, at, 21, 21, hover, 1, NEUTRAL);
                FrontierConsole.item(g, stack, bx + 2, at + 2, 16);
                if (hover) setTooltipForNextRenderPass(List.of(choice.name.getVisualOrderText()));
            }
        }
    }

    private void drawGenerations(GuiGraphics g, int mx, int my) {
        int gx = generationX(), gy = y + 57, row = generationRow();
        g.pose().pushPose();
        try {
            g.pose().translate(0, 0, 280);
            OreUiRenderer.panel(g, gx, gy, 82, row * 10, 1, NEUTRAL);
            for (int i = 0; i <= 9; i++) {
                int ry = gy + i * row;
                if (i == generation || inside(mx, my, gx, ry, 82, row)) OreUiRenderer.card(g, gx + 1, ry, 80, row, true, i == generation, 1, CYAN);
                FrontierConsole.text(g, font, i == 0 ? text("all_generations") : text("generation", i), gx + 5, ry + (row - 8) / 2, 73, TEXT);
            }
        } finally { g.pose().popPose(); }
    }

    private void pinRoom() {
        FrontierTournamentHud.pin(state);
        minecraft.setScreen(null);
    }

    @Override public void onClose() {
        if (megaLimitOpen) { dismissMegaLimit(); return; }
        if (createMenu != null) { createMenu = null; return; }
        if (generationsOpen) { generationsOpen = false; rebuildWidgets(); return; }
        if (move >= 0) { move = -1; rebuildWidgets(); return; }
        if (page == Page.PICKER && state.hasUUID("room")) { pinRoom(); return; }
        if (page == Page.CREATE) { page(Page.LOBBY); return; }
        if (page == Page.INVITE && state.hasUUID("room")) { pinRoom(); return; }
        if (state.hasUUID("room")) { pinRoom(); return; }
        super.onClose();
    }

    private static Component moveName(String id) { return Component.translatable("cobblemon.move." + id); }
    private static Component text(String key, Object... args) { return Component.translatable(KEY + key, args); }
}
