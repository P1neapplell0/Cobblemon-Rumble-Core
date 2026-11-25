package com.p1nero.cobblemon.rumblecore.client.combat;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TrialPortraits;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TutorialPulse;
import com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierRooms;
import com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierRoster;
import com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierRules;
import fr.harmex.cobbledollars.common.utils.extensions.BigIntegerExtensionsKt;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
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
    private enum Page { LOBBY, CREATE, ROOM, PRESETS, PICKER, WAITING, INVITE }
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
    private boolean pending;
    private String dialogProblem = "";
    private int spectatorOffset;
    private static final int READY_GREEN = 0xff98eb9b;
    private int refreshTicks;
    private final TrialPortraits portraits = new TrialPortraits();
    private int moveAnchorX, moveAnchorY;
    private String moveQuery = "";
    private MoveTemplate inspectedMove;
    private ResourceLocation pendingSpeciesPick;
    private boolean generationsOpen;
    private enum CreateMenu { MODE, RULE, TIME }
    private CreateMenu createMenu;
    private boolean createMulti;
    private boolean createTimed = true;
    private int createMinutes = FrontierRules.DEFAULT_TIMED_MINUTES, menuSelected;
    private int roomFilter;
    private int presetScroll, pickerScroll, teamScroll, draftScroll;
    private int lastPromptSlot = -1;
    private boolean listContent, draggingScrollbar;
    private static final int PRESET_STRIDE = 64, TEAM_STRIDE = 34;
    private boolean editingPreset, managePresets;
    private final Map<ResourceLocation, List<String>> syncedLearnsets = new HashMap<>();
    private final List<FrontierRoster.Pick> presets = FrontierPresetStore.load();
    private String inviteQuery = "";
    private final List<Button> inviteButtons = new ArrayList<>();

    private record Option(Component name, String id, Runnable apply) { }

    private FrontierScreen(CompoundTag state) {
        super(text("title"));
        this.state = state;
        page = destination(state);
        updateDialog(state);
    }

    private static Page destination(CompoundTag data) {
        if (!data.hasUUID("room")) return Page.LOBBY;
        return multi(data) ? Page.WAITING : Page.ROOM;
    }

    private static boolean multi(CompoundTag data) {
        return data.contains("multi") ? data.getBoolean("multi") : data.getInt("capacity") > 2;
    }

    public static void receive(CompoundTag data) {
        var mc = Minecraft.getInstance();
        if (data.contains("learnset", Tag.TAG_STRING)) {
            if (mc.screen instanceof FrontierScreen screen) {
                var id = ResourceLocation.tryParse(data.getString("learnset"));
                if (id != null) {
                    screen.syncedLearnsets.put(id, data.getList("moves", Tag.TAG_STRING).stream()
                            .map(Tag::getAsString).toList());
                    if (id.equals(screen.pendingSpeciesPick) && screen.page == Page.PICKER && screen.kind == Kind.SPECIES) {
                        screen.pendingSpeciesPick = null;
                        var pick = FrontierRooms.decode(data.getCompound("initialPick"));
                        if (pick != null) screen.editPick(pick);
                    }
                }
            }
            return;
        }
        FrontierTournamentHud.sync(data);
        if (mc.screen instanceof FrontierScreen screen) {
            if (data.getBoolean("close")) { mc.setScreen(null); return; }
            boolean hadRoom = screen.state.hasUUID("room");
            Page before = destination(screen.state), after = destination(data);
            screen.state = data;
            screen.pending = false;
            if (data.hasUUID("room") != hadRoom || before != after
                    || screen.page == Page.INVITE && !data.getBoolean("canInvite")) {
                screen.page = after;
                screen.offset = 0;
                screen.createMenu = null;
                screen.move = -1;
                screen.generationsOpen = false;
                screen.moveOffset = 0;
                screen.dialogProblem = "";
                screen.spectatorOffset = 0;
                screen.teamScroll = screen.draftScroll = 0;
                screen.lastPromptSlot = -1;
            }
            if (screen.move >= 0 && (screen.own().getBoolean("ready") ||
                    FrontierRoster.NONE.equals(screen.editingPick(screen.slot).species()))) screen.move = -1;
            screen.updateDialog(data);
            if (screen.page == Page.INVITE && screen.field != null) {
                screen.refreshInviteButtons();
            } else if (screen.page != Page.PICKER || !data.getString("problem").isEmpty()) screen.rebuildWidgets();
        } else if (data.getBoolean("open") && mc.level != null
                && !(data.getBoolean("automatic") && FrontierTournamentHud.isPinned())) {
            mc.setScreen(new FrontierScreen(data));
        }
    }

    private void updateDialog(CompoundTag data) {
        String issue = data.getString("problem");
        if (ruleDialog(issue)) {
            dialogProblem = issue;
            move = -1;
        }
    }

    private static boolean ruleDialog(String issue) {
        return "mega_limit".equals(issue) || "repeat_species".equals(issue);
    }

    private boolean dialogOpen() { return !dialogProblem.isEmpty(); }
    private boolean watching() { return state.getBoolean("watching"); }

    @Override public boolean isPauseScreen() { return false; }

    @Override protected void init() {
        int preferredWidth = switch (page) {
            case CREATE, INVITE -> 320;
            case PRESETS -> 336;
            default -> 620;
        };
        int preferredHeight = switch (page) {
            case ROOM -> 334;
            case WAITING -> 350;
            case CREATE -> createMulti ? 195 : 153;
            default -> 300;
        };
        w = Math.min(preferredWidth, width - 14);
        h = Math.min(preferredHeight, height - 12);
        x = (width - w) / 2;
        y = (height - h) / 2;
        field = null;
        listContent = false;
        inviteButtons.clear();
        switch (page) {
            case LOBBY -> {
                var editPresets = btn(x + w - 194, y + 7, 88, text("edit_presets"), true, Action.SECONDARY, () -> openPresets(true));
                addRenderableOnly((g, mx, my, dt) -> {
                    if (presets.isEmpty() && !dialogOpen()) {
                        TutorialPulse.render(g, editPresets.getX(), editPresets.getY(), editPresets.getWidth(), editPresets.getHeight());
                    }
                });
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
            case PRESETS -> presetPanel();
            case WAITING -> waiting();
            case INVITE -> invitation();
        }
        if (page == Page.CREATE || page == Page.PICKER || page == Page.INVITE || page == Page.PRESETS)
            btn(x + 9, y + 7, 62, text("back"), () -> page(page == Page.CREATE ? Page.LOBBY : page == Page.PICKER && editingPreset ? Page.PRESETS : destination(state)));
        if (state.hasUUID("room") && (page == Page.WAITING || page == Page.ROOM))
            btn(x + 91, y + 7, 81, text("pin_room"), this::pinRoom);
        if (page == Page.ROOM || page == Page.WAITING) spectatorControls();
        if (dialogOpen()) drawDialog();
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
        draggingScrollbar = false;
        if (next == Page.PICKER) pickerScroll = 0;
        if (next == Page.WAITING && page != Page.PRESETS) teamScroll = 0;
        move = -1;
        page = next;
        if (next != Page.PICKER && next != Page.PRESETS) {
            editingPreset = false;
            managePresets = false;
        }
        offset = 0;
        rebuildWidgets();
    }

    private void request(String action, Consumer<CompoundTag> payload) {
        if (pending && !"leave".equals(action)) return;
        var data = new CompoundTag();
        data.putString("action", action);
        if (state.hasUUID("room")) data.putUUID("room", state.getUUID("room"));
        payload.accept(data);
        pending = true;
        PacketDistributor.sendToServer(new FrontierRooms.Command(data));
    }

    private int createRow(int row) {
        int last = createMulti && createTimed ? 3 : createMulti ? 2 : 1;
        return y + 34 + row * Math.clamp((h - 64) / Math.max(1, last), 21, 27);
    }
    private int createInputX() { return x + Math.min(108, w * 2 / 5); }
    private int createInputWidth() { return x + w - 13 - createInputX(); }
    private int createMenuAnchor() { return createRow(createMenu == CreateMenu.MODE ? 0 : createMenu == CreateMenu.RULE ? 2 : 3); }
    private int menuTop() {
        int anchor = createMenuAnchor(), menuHeight = 19 * optionCount();
        return anchor + 20 + menuHeight < height - 4 ? anchor + 20 : Math.max(4, anchor - menuHeight);
    }
    private int optionCount() { return createMenu == CreateMenu.TIME ? FrontierRules.TIMED_MINUTES.size() : 2; }

    private void dropdown(CreateMenu menu, int row, int selected) {
        btn(createInputX(), createRow(row), createInputWidth(), createOption(menu, selected).copy().append("  ▾"), () -> {
            createMenu = menu;
            menuSelected = selected;
        });
    }

    private void createForm() {
        dropdown(CreateMenu.MODE, 0, createMulti ? 1 : 0);
        var stakeBox = edit(createInputX(), createRow(1) + 1, createInputWidth(), stake, value -> stake = value);
        stakeBox.setMaxLength(18);
        stakeBox.setFilter(value -> value.matches("[0-9]{0,18}"));
        if (createMulti) {
            dropdown(CreateMenu.RULE, 2, createTimed ? 0 : 1);
            if (createTimed) dropdown(CreateMenu.TIME, 3, FrontierRules.TIMED_MINUTES.indexOf(createMinutes));
        }
        btn(createInputX(), y + h - 27, createInputWidth(), text("create"), true, Action.PRIMARY, () -> request("create", tag -> {
            tag.putString("stake", stake.isBlank() ? "0" : stake);
            tag.putBoolean("multi", createMulti);
            tag.putInt("minutes", createMulti && createTimed ? createMinutes : 0);
        }));
    }

    private void drawCreateForm(GuiGraphics g) {
        String[] labels = {"create_mode", "stake_label", "create_rule", "create_time"};
        for (int i = 0; i < (createMulti ? createTimed ? 4 : 3 : 2); i++) {
            FrontierConsole.text(g, font, text(labels[i]), x + 15, createRow(i) + 6, createInputX() - x - 20, i == 1 ? GOLD : TEXT);
        }
    }

    private static Component createOption(CreateMenu menu, int index) {
        return switch (menu) {
            case MODE -> text(index == 0 ? "mode_solo" : "mode_multi");
            case RULE -> text(index == 0 ? "timed" : "elimination");
            case TIME -> text("minutes_option", FrontierRules.TIMED_MINUTES.get(index));
        };
    }

    private void selectCreateOption(int index) {
        switch (createMenu) {
            case MODE -> createMulti = index == 1;
            case RULE -> createTimed = index == 0;
            case TIME -> createMinutes = FrontierRules.TIMED_MINUTES.get(index);
        }
        createMenu = null;
        rebuildWidgets();
    }

    private void drawCreateMenu(GuiGraphics g, int mx, int my) {
        int top = menuTop(), left = createInputX(), rows = optionCount(), menuWidth = createInputWidth();
        g.pose().pushPose();
        try {
            g.pose().translate(0, 0, 400);
            g.fill(left, top, left + menuWidth, top + 19 * rows, 0xff171918);
            for (int index = 0; index < rows; index++) {
                int row = top + index * 19;
                OreUiRenderer.labeledButton(g, font, createOption(createMenu, index), left, row, menuWidth, 19,
                        index == menuSelected ? Action.PRIMARY : Action.SECONDARY, inside(mx, my, left, row, menuWidth, 19), true, 1, NEUTRAL);
            }
        } finally { g.pose().popPose(); }
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (dialogOpen() && key == GLFW.GLFW_KEY_ESCAPE) { dismissDialog(); return true; }
        if (page == Page.CREATE && createMenu != null) {
            if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
                menuSelected = Math.clamp(menuSelected + (key == GLFW.GLFW_KEY_UP ? -1 : 1), 0, optionCount() - 1);
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) selectCreateOption(menuSelected);
            else if (key == GLFW.GLFW_KEY_ESCAPE) createMenu = null;
            return true;
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
            boolean open = multi ? (room.getInt("capacity") == 0 || room.getInt("count") < room.getInt("capacity")) : guest.isEmpty();
            int cx = x + 12 + i % columns * span, cy = y + 58 + i / columns * ROOM_STRIDE;
            var label = open ? text("join_room", host.getString("name")) : text("join_watch");
            control(cx, cy, span - 4, ROOM_STRIDE - 4, label, true,
                    () -> request("join", tag -> tag.putUUID("room", room.getUUID("room"))),
                    (g, b, dt) -> {
                        int middle = cx + b.getWidth() / 2, wing = Math.max(23, b.getWidth() / 4);
                        card(g, cx, cy, b.getWidth(), b.getHeight(), open ? CYAN : GOLD, b.isHoveredOrFocused());
                        if (multi) {
                            player(g, host, cx + 10, cy + 11, 23, GOLD);
                            FrontierConsole.text(g, font, Component.literal(host.getString("name")), cx + 39, cy + 13, b.getWidth() - 47, TEXT);
                            FrontierConsole.text(g, font, text("mode_multi"), cx + 39, cy + 26, b.getWidth() - 47, GOLD);
                            center(g, font, text(room.getInt("minutes") > 0 ? "timed_minutes" : "elimination", room.getInt("minutes")),
                                    middle, cy + 39, b.getWidth() - 16, CYAN);
                            center(g, font, money(room), middle, cy + 52, b.getWidth() - 16, GOLD);
                            center(g, font, text("seats", room.getInt("count"), capacityLabel(room)), middle, cy + 65, b.getWidth() - 8, open ? CYAN : MUTED);
                            return;
                        }
                        roomSeat(g, host, cx + wing, cy, wing * 2 - 5, CYAN, dt, false);
                        roomSeat(g, guest, cx + b.getWidth() - wing, cy, wing * 2 - 5, PINK, dt, true);
                        center(g, font, Component.literal("VS"), middle, cy + 12, 28, TEXT);
                        center(g, font, money(room), middle, cy + 26, Math.max(38, b.getWidth() - 52), GOLD);
                        center(g, font, text(open ? "open" : "join_watch"), middle, cy + 65, b.getWidth() - 8, open ? CYAN : GOLD);
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
    private int wingWidth(int middleGap) { return (w - 36 - middleGap) / 2; }
    private int wingX(boolean left, int middleGap) {
        return left ? x + 18 : x + w - 18 - wingWidth(middleGap);
    }
    private int teamWidth() { return wingWidth(centerRail() + 12); }
    private int teamX(boolean left) { return wingX(left, centerRail() + 12); }

    private CompoundTag own() {
        if (watching()) return state.getCompound("host");
        return state.getCompound(state.contains("self", Tag.TAG_COMPOUND) ? "self" : state.getBoolean("owner") ? "host" : "guest");
    }
    private CompoundTag rival() { return state.getCompound(watching() || state.getBoolean("owner") ? "guest" : "host"); }

    private Component seatLabel(CompoundTag seat) {
        return text(seat.getBoolean("away") ? "seat_away" : seat.getBoolean("ready") ? "seat_ready" : "seat_edit", seat.getString("name"));
    }

    private int seatColor(CompoundTag seat) {
        return seat.getBoolean("away") ? MUTED : seat.getBoolean("ready") ? READY_GREEN : GOLD;
    }

    private void inviteButton() {
        btn(x + w - 105, y + 7, 95, text("invite"), state.getBoolean("canInvite"), () -> {
            inviteQuery = "";
            page(Page.INVITE);
            request("list", tag -> {});
        });
    }

    private void waiting() {
        var seats = state.getList("seats", Tag.TAG_COMPOUND);
        int total = wingWidth(10);
        int[] row = new int[2];
        boolean ready = state.getBoolean("selfReady");
        var local = minecraft.player.getUUID();
        var host = state.getCompound("host");
        teamScroll = Math.clamp(teamScroll, 0, scrollLimit());
        listContent = true;
        for (int i = 0; i < seats.size(); i++) {
            var seat = seats.getCompound(i);
            int side = seat.getInt("side"), accent = side == 0 ? CYAN : PINK;
            if (side < 0 || side > 1 || !seat.hasUUID("player")) continue;
            int left = wingX(side == 0, 10), top = scrollTop() + row[side]++ * TEAM_STRIDE - teamScroll;
            if (top >= scrollBottom() || top + TEAM_STRIDE <= scrollTop()) continue;
            int centerY = top + (TEAM_STRIDE - 5) / 2;
            boolean self = seat.getUUID("player").equals(local);
            boolean owner = host.hasUUID("player") && host.getUUID("player").equals(seat.getUUID("player"));
            int portraitX = left + total - 27, avatarX = left + 6;
            int nameEnd = self ? portraitX - 24 : portraitX;
            addContent((g, mx, my, dt) -> {
                card(g, left, top, total, TEAM_STRIDE - 2, accent, false);
                g.fill(left + 2, top + 4, left + 4, top + TEAM_STRIDE - 7, seatColor(seat));
                if (owner) g.renderOutline(avatarX - 2, centerY - 10, 20, 20, GOLD);
                player(g, seat, avatarX, centerY - 8, 16, owner ? GOLD : accent);
                FrontierConsole.text(g, font, seatLabel(seat), left + 28, centerY - 4, nameEnd - left - 32, seatColor(seat));
                if (owner && inside(mx, my, avatarX - 2, centerY - 10, 20, 20)) setTooltipForNextRenderPass(text("room_owner"));
                var species = ResourceLocation.tryParse(seat.getList("species", Tag.TAG_STRING).getString(0));
                if (species != null && !FrontierRoster.NONE.equals(species)) {
                    partyFace(g, portraits, species, Set.of(), portraitX, centerY - 11, 22, dt, side == 1, accent);
                } else {
                    OreUiRenderer.field(g, portraitX, centerY - 11, 22, 22, false, 1, accent);
                    center(g, font, Component.literal("?"), portraitX + 11, centerY - 4, 16, MUTED);
                }
            });
            if (self) control(portraitX - 23, centerY - 9, 20, 19, text("choose_preset"), !ready, true,
                    this::openPresets, (g, b, dt) -> OreUiRenderer.labeledButton(g, font, Component.literal("⇄"),
                            b.getX(), b.getY(), b.getWidth(), b.getHeight(), Action.SECONDARY, b.isHoveredOrFocused(), b.active, 1, accent));
        }
        listContent = false;
        btn(x + 9, y + 7, 77, text("leave"), () -> request("leave", tag -> {}));
        inviteButton();
        for (int side = 0; side < 2; side++) {
            int team = side;
            btn(wingX(side == 0, 10), y + 47, total,
                    text(side == 0 ? "join_blue" : "join_red"), state.getInt("selfSide") != side,
                    () -> request("side", tag -> tag.putInt("side", team)));
        }
        int footer = y + h - 26;
        if (!watching()) btn(x + w - (state.getBoolean("owner") ? 180 : 94), footer, 82, text(ready ? "unready" : "ready"),
                !get(own(), 0).equals(FrontierRoster.EMPTY), () -> request("ready", tag -> {}));
        if (state.getBoolean("owner")) {
            btn(x + 10, footer, 94, text("shuffle_teams"), seats.size() >= 2, () -> request("shuffle", tag -> {}));
            btn(x + w - 94, footer, 84, text("start"), state.getBoolean("canStart"),
                    Action.PRIMARY, () -> request("start", tag -> {}));
        }
    }

    private List<CompoundTag> inviteRows() {
        String term = inviteQuery.strip().toLowerCase(Locale.ROOT);
        return state.getList("candidates", Tag.TAG_COMPOUND).stream().map(CompoundTag.class::cast)
                .filter(row -> row.getString("name").toLowerCase(Locale.ROOT).contains(term)).toList();
    }

    private int inviteVisible() { return Math.max(1, (h - 107) / 26); }

    private void invitation() {
        field = edit(x + 16, y + 40, w - 32, inviteQuery, value -> {
            // EditBox also calls the responder when only its cursor moves.
            if (inviteQuery.equals(value)) return;
            inviteQuery = value;
            offset = 0;
            refreshInviteButtons();
        });
        field.setHint(text("search"));
        refreshInviteButtons();
    }

    private void refreshInviteButtons() {
        // The search box must survive list refreshes, empty results, and scrollbar movement.
        for (var button : inviteButtons) {
            if (getFocused() == button) setFocused(null);
            removeWidget(button);
        }
        inviteButtons.clear();
        var rows = inviteRows();
        offset = Math.clamp(offset, 0, Math.max(0, rows.size() - inviteVisible()));
        for (int i = 0; i < inviteVisible() && offset + i < rows.size(); i++) {
            var row = rows.get(offset + i);
            int top = y + 67 + i * 26;
            boolean sent = row.getBoolean("sent"), cooldown = row.getBoolean("cooldown");
            inviteButtons.add(btn(x + w - 105, top + 2, 85, text(sent ? "invited" : cooldown ? "invite_cooldown" : "send_invite"),
                    !sent && !cooldown, Action.SECONDARY, () -> request("invite", tag -> tag.putUUID("target", row.getUUID("player")))));
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



    private void drawWaiting(GuiGraphics g) {
        int minutes = state.getInt("minutes");
        center(g, font, text(minutes > 0 ? "timed_minutes" : "elimination", minutes), x + w / 2, y + 34, w - 24, GOLD);
    }

    private FrontierRoster.Pick get(CompoundTag seat, int i) {
        var picks = seat.getList("picks", Tag.TAG_COMPOUND);
        return i >= picks.size() ? FrontierRoster.EMPTY : Objects.requireNonNullElse(FrontierRooms.decode(picks.getCompound(i)), FrontierRoster.EMPTY);
    }

    private int lineupHeight() { return Math.max(48, (scrollBottom() - scrollTop()) / FrontierRoster.TEAM_SIZE); }

    private static final int[] SLOT_COLORS = {0xffffcc76, 0xff80dfff, 0xffc8a4ff};

    private void lineups() {
        var own = own();
        var rival = rival();
        int locked = state.getInt("locked");
        boolean turn = state.getBoolean("draftTurn");
        var selected = FrontierRooms.decode(state.getCompound("pendingPick"));
        boolean canConfirm = turn && selected != null && !selected.equals(FrontierRoster.EMPTY);
        int rowHeight = lineupHeight();
        int viewport = scrollBottom() - scrollTop();
        if (turn && locked != lastPromptSlot) {
            if (locked * rowHeight < draftScroll) draftScroll = locked * rowHeight;
            else if ((locked + 1) * rowHeight > draftScroll + viewport) draftScroll = (locked + 1) * rowHeight - viewport;
        }
        lastPromptSlot = turn ? locked : -1;
        draftScroll = Math.clamp(draftScroll, 0, scrollLimit());
        listContent = true;
        for (boolean left : new boolean[]{true, false}) {
            var seat = left ? own : rival;
            int accent = left ? CYAN : PINK;
            int cx = teamX(left), cw = teamWidth(), ch = lineupHeight();
            for (int i = 0; i < FrontierRoster.TEAM_SIZE; i++) {
                var pick = left && turn && i == locked && selected != null ? selected : get(seat, i);
                int top = scrollTop() + i * ch - draftScroll;
                lineupCard(cx, top, cw, ch - 3, i, pick, left, accent, false);
                if (left && turn && i == locked) {
                    control(cx, top, cw, ch - 3, text("choose_preset"), true, true, this::openPresets, (g, b, dt) -> {
                        g.renderOutline(cx, top, cw, ch - 4, CYAN);
                        if (pick.equals(FrontierRoster.EMPTY)) turnBanner(g, text("choose_preset"), cx + cw / 2, top + ch / 2 - 11, cw - 18, CYAN);
                        if (!canConfirm || b.isHoveredOrFocused()) pulse(g, cx, top, cw, ch - 3);
                    });
                }
            }
        }
        listContent = false;
        btn(x + 9, y + 7, 77, text("leave"), () -> request("leave", tag -> {}));
        inviteButton();
        int middle = x + w / 2, footer = y + h - 26;
        if (!watching()) {
            int confirmX = middle - (state.getBoolean("owner") ? 95 : 45);
            btn(confirmX, footer, 90, text("confirm_pick"), canConfirm,
                    canConfirm ? Action.PRIMARY : Action.SECONDARY, () -> request("confirm", tag -> {}));
            if (canConfirm) addRenderableOnly((g, mx, my, dt) -> pulse(g, confirmX, footer, 90, 20));
        }
        if (state.getBoolean("owner")) btn(middle + 5, footer, 90, text("start"), state.getBoolean("canStart"),
                Action.PRIMARY, () -> request("start", tag -> {}));
        var active = state.getCompound(state.getBoolean("hostTurn") ? "host" : "guest");
        boolean away = own.getBoolean("away") || rival.getBoolean("away");
        boolean complete = own.getBoolean("ready") && rival.getBoolean("ready");
        var status = text(!state.contains("guest", Tag.TAG_COMPOUND) ? "waiting" : away ? "waiting_rejoin"
                : complete ? "draft_locked" : turn ? canConfirm ? "draft_confirm_hint" : "draft_slot" : "draft_opponent",
                turn ? locked + 1 : active.getString("name"));
        addRenderableOnly((g, mx, my, dt) -> turnBanner(g, status, middle, y + 64, w - 24, turn ? CYAN : GOLD));
    }

    private void turnBanner(GuiGraphics g, Component message, int center, int top, int available, int color) {
        var bold = message.copy().withStyle(ChatFormatting.BOLD);
        int size = Math.min(available, Math.max(60, font.width(bold) + 20));
        g.fill(center - size / 2, top, center - size / 2 + size, top + 19, 0xff151e22);
        g.renderOutline(center - size / 2, top, size, 19, color);
        center(g, font, bold, center, top + 6, size - 10, color);
    }

    private void openPresets() { openPresets(false); }

    private void openPresets(boolean manage) {
        editingPreset = true;
        managePresets = manage;
        presetScroll = 0;
        page(Page.PRESETS);
    }

    private FrontierRoster.Pick editingPick(int index) {
        return editingPreset ? index < presets.size() ? presets.get(index) : FrontierRoster.EMPTY : get(own(), index);
    }

    private void presetPanel() {
        int cardW = w - 91;
        presetScroll = Math.clamp(presetScroll, 0, scrollLimit());
        btn(x + w - 104, y + 7, 94, text("new_preset"), presets.size() < FrontierPresetStore.LIMIT, () -> choose(Kind.SPECIES, presets.size()));
        if (presets.isEmpty()) addRenderableOnly((g, mx, my, dt) -> center(g, font, text("preset_empty"), x + w / 2, y + h / 2, w - 26, CYAN));
        listContent = true;
        for (int index = presetScroll / PRESET_STRIDE; index < presets.size(); index++) {
            int top = scrollTop() + PRESET_STRIDE * index - presetScroll, which = index;
            if (top >= scrollBottom()) break;
            var pick = presets.get(index);
            lineupCard(x + 10, top, cardW, PRESET_STRIDE - 4, index, pick, true, CYAN, true);
            boolean canSelect = multi(state) ? !state.getBoolean("selfReady") : state.getBoolean("draftTurn");
            if (!managePresets) btn(x + w - 76, top + 8, 57, text("select_preset"), canSelect, Action.PRIMARY, () -> {
                request("preset", tag -> tag.put("pick", FrontierRooms.encode(pick)));
                page(destination(state));
            });
            btn(x + w - 76, top + (managePresets ? 20 : 32), 57, text("delete_preset"), () -> {
                presets.remove(which);
                FrontierPresetStore.save(presets);
                move = -1;
                rebuildWidgets();
            });
        }
        listContent = false;
        addRenderableOnly((g, mx, my, dt) -> center(g, font,
                text("preset_count", presets.size(), FrontierPresetStore.LIMIT), x + w / 2, y + h - 20, w - 26, GOLD));
        if (move >= 0 && slot < presets.size()) moveChooser(presets.get(slot));
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
        int badgeW = Math.clamp(Math.min(cw / 7, ch / 2), 17, 28);
        int badgeH = Math.clamp(ch / 3, 12, 19);
        int badgeSpace = badgeW + 3;
        int contentX = cx + 5 + (left ? badgeSpace : 0);
        int contentW = Math.max(1, cw - badgeSpace - 10);
        int faceX = left ? contentX : contentX + contentW - portraitW;
        int faceY = innerY + (innerH - portraitW) / 2;
        int itemX = left ? faceX + portraitW + 3 : faceX - itemSize - 3;
        int detailsX = left ? itemX + itemSize + 4 : contentX;
        int detailsW = Math.max(1, contentW - portraitW - itemSize - 7);
        int itemColumnY = innerY + Math.max(0, (innerH - itemColumnH) / 2);
        int itemY = itemColumnY + itemColumnH - itemSize;
        var held = itemStack(pick.held());
        addContent((g, mx, my, dt) -> {
            card(g, cx, cy, cw, ch, accent, false);
            drawSlotBadge(g, cx + (left ? 5 : cw - badgeW - 5), innerY + (innerH - badgeH) / 2, badgeW, badgeH, index);
            if (species == null) {
                if (!edit && !(left && state.getBoolean("draftTurn") && index == state.getInt("locked")))
                    center(g, font, text("empty"), contentX + contentW / 2, cy + (ch - 8) / 2, contentW - 6, MUTED);
                return;
            }
            if (portraitW > 4) partyFace(g, portraits, pick.species(), Set.of(), faceX, faceY,
                    portraitW, dt, !left, accent);
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
                if (b.isHoveredOrFocused() && move < 0) itemTooltip(held);
                int size = Math.min(itemSize - 6, 24);
                FrontierConsole.item(g, held, itemX + (itemSize - size) / 2, itemY + (itemSize - size) / 2, size);
            }
        });
        lineupMoves(detailsX, innerY, detailsW, innerH, index, pick, accent, edit);
    }

    private void lineupMoves(int x, int y, int width, int height, int teamSlot, FrontierRoster.Pick pick,
                             int accent, boolean edit) {
        int count = FrontierRoster.MOVE_SLOTS;
        // Keep the four moves stacked and center them inside the card.
        int stride = height / count;
        int rowH = stride - (stride >= 13 ? 2 : 1);
        int tool = edit ? Math.clamp(width / 7, 7, 12) : 0;
        int inset = edit ? tool + 2 : 0;
        int nameW = Math.max(1, width - inset * 2);
        for (int i = 0; i < count; i++) {
            int index = i, rowY = y + (height - count * stride) / 2 + i * stride, nameX = x + inset;
            String id = pick.moves().get(i);
            var template = id.isEmpty() ? null : Moves.getByName(id);
            Component name = template != null ? RealtimeMoveShapeUi.displayNameWithShape(template)
                    : id.isEmpty() ? Component.literal("–") : moveName(id);
            boolean selected = edit && slot == teamSlot && move == index;
            addContent((g, mx, my, dt) -> {
                FrontierConsole.shallowCard(g, nameX, rowY, nameW, rowH, accent, selected);
                int iconSize = template == null ? 0 : Math.max(0, Math.min(10, Math.min(rowH - 3, nameW - 10)));
                int nameInset = 3;
                if (iconSize > 0) {
                    typeIcon(g, template.getElementalType(), nameX + 3, rowY + (rowH - 2 - iconSize) / 2, iconSize);
                    nameInset += iconSize + 3;
                }
                FrontierConsole.moveLabel(g, font, name, nameX + nameInset, rowY + Math.max(0, (rowH - 9) / 2),
                        nameW - nameInset - 3, id.isEmpty() ? MUTED : TEXT);
                if (!id.isEmpty() && move < 0 && !dialogOpen() && inside(mx, my, nameX, rowY, nameW, rowH)) {
                    inspectedMove = template;
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
                syncedLearnsets.remove(pick.species());
                requestLearnset(pick.species());
                rebuildWidgets();
            });
        }
    }

    private void drawSlotBadge(GuiGraphics g, int x, int y, int width, int height, int slot) {
        int color = SLOT_COLORS[slot % SLOT_COLORS.length];
        var label = Component.literal(editingPreset ? "#" + (slot + 1) : (slot + 1) + "P").withStyle(ChatFormatting.BOLD);
        FrontierConsole.shallowCard(g, x, y, width, height, color, true);
        float scale = Math.min((width - 4F) / font.width(label), (height - 4F) / font.lineHeight);
        g.pose().pushPose();
        try {
            g.pose().translate(x + width / 2F, y + (height - 2 - font.lineHeight * scale) / 2F, 0);
            g.pose().scale(scale, scale, 1);
            g.drawCenteredString(font, label, 0, 0, color);
        } finally { g.pose().popPose(); }
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void pulse(GuiGraphics g, int x, int y, int width, int height) {
        TutorialPulse.render(g, x, y, width, height);
    }

    private int moveStride() { return 20; }
    private int moveListW() { return Math.min(168, width - 12); }
    private int moveListX() { return Math.clamp(moveAnchorX, 6, width - moveListW() - 6); }
    private int moveListY() { return Math.clamp(moveAnchorY, 6, Math.max(6, height - moveListHeight() - 6)); }
    private int moveListRows() { return Math.clamp((height - 61) / moveStride(), 1, 7); }
    private int moveListHeight() { return 49 + moveListRows() * moveStride(); }

    private List<String> candidates(FrontierRoster.Pick pick) {
        String query = moveQuery.toLowerCase(Locale.ROOT).strip();
        return syncedLearnsets.getOrDefault(pick.species(), List.of()).stream()
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
                if (total == 0) FrontierConsole.text(g, font,
                        text(syncedLearnsets.containsKey(pick.species()) ? "no_replacements" : "loading_moves"),
                        bx + 6, listY + 4, bw - 12, MUTED);
                for (int i = 0; i < rows && moveOffset + i < total; i++) {
                    var option = Moves.getByName(list.get(moveOffset + i));
                    if (option == null) continue;
                    int ry = listY + i * moveStride();
                    boolean over = inside(mx, my, bx + 5, ry, bw - 17, moveStride() - 2);
                    if (over && !dialogOpen()) inspectedMove = option;
                    FrontierConsole.summaryMove(g, font, option, bx + 5, ry, bw - 17, moveStride() - 2, over);
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

    private void requestLearnset(ResourceLocation species) {
        var data = new CompoundTag();
        data.putString("action", "learnset");
        data.putString("species", species.toString());
        PacketDistributor.sendToServer(new FrontierRooms.Command(data));
    }

    private void choose(Kind kind, int slot) {
        pendingSpeciesPick = null;
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
        var old = editingPick(slot);
        var list = new ArrayList<Option>();
        switch (kind) {
            case SPECIES -> {
                PokemonSpecies.getImplemented().stream().filter(species -> generation == 0 || FrontierRoster.generation(species) == generation)
                        .sorted(Comparator.comparingInt(species -> species.getNationalPokedexNumber())).forEach(species -> {
                            var id = species.getResourceIdentifier();
                            list.add(new Option(species.getTranslatedName(), id.toString(),
                                    () -> {
                                        if (id.equals(old.species())) editPick(old);
                                        else {
                                            pendingSpeciesPick = id;
                                            requestLearnset(id);
                                        }
                                    }));
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
        if (editingPreset) {
            if (pick.equals(FrontierRoster.EMPTY)) return;
            if (index < presets.size()) presets.set(index, pick);
            else if (index == presets.size() && presets.size() < FrontierPresetStore.LIMIT) {
                presets.add(pick);
                presetScroll = index * PRESET_STRIDE;
            }
            FrontierPresetStore.save(presets);
        } else request("edit", tag -> { tag.putInt("slot", index); tag.put("pick", FrontierRooms.encode(pick)); });
    }

    private void editPick(FrontierRoster.Pick pick) {
        pendingSpeciesPick = null;
        submitPick(slot, pick);
        page(editingPreset ? Page.PRESETS : Page.ROOM);
    }

    private List<Option> filtered() {
        String query = filter.toLowerCase(Locale.ROOT).strip();
        return query.isEmpty() ? options : options.stream().filter(opt -> opt.id.contains(query)
                || opt.name.getString().toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    private int pickerX() { return x + 12; }
    private int pickerWidth() { return w - 32; }
    private int pickerColumns() { return kind == Kind.SPECIES ? Math.clamp(pickerWidth() / 54, 2, 9) : Math.max(1, pickerWidth() / 22); }
    private int pickerStride() { return kind == Kind.SPECIES ? 49 : 22; }
    private int pickerTop() { return y + (kind == Kind.HELD ? 87 : 63); }
    private int pickerCellWidth() { return kind == Kind.HELD ? 22 : pickerWidth() / pickerColumns(); }

    private void picker() {
        var pick = editingPick(slot);
        boolean species = kind == Kind.SPECIES;
        boolean remove = !(species ? FrontierRoster.NONE.equals(pick.species()) : FrontierRoster.NONE.equals(pick.held()));
        int generationWidth = species ? 85 : 0;
        int removeW = species && remove ? 94 : 0;
        field = edit(x + 12, y + 37, w - 24 - generationWidth - removeW, filter, value -> { filter = value; pickerScroll = 0; });
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
                    pickerScroll = 0;
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
        if (dialogOpen()) { super.mouseClicked(mx, my, button); return true; }
        if (page == Page.CREATE && createMenu != null && button == 0) {
            int menuTop = menuTop();
            if (inside(mx, my, createInputX(), menuTop, createInputWidth(), optionCount() * 19)) {
                selectCreateOption((int) (my - menuTop) / 19);
            } else createMenu = null;
            return true;
        }
        if (page == Page.PICKER && generationsOpen && button == 0 && !inside(mx, my, generationX(), y + 36, 82, 20)) {
            if (inside(mx, my, generationX(), y + 57, 82, generationRow() * 10)) {
                generation = (int) ((my - y - 57) / generationRow());
                buildOptions();
                pickerScroll = 0;
            }
            generationsOpen = false;
            rebuildWidgets();
            return true;
        }
        if ((page == Page.ROOM || page == Page.PRESETS) && move >= 0) {
            if (button != 0) return true;
            if (field.mouseClicked(mx, my, button)) { setFocused(field); return true; }
            int listY = moveListY() + 42;
            if (inside(mx, my, moveListX() + 5, listY, moveListW() - 17, moveListRows() * moveStride())) {
                var pick = editingPick(slot);
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
        if (button == 0 && scrollPage() && scrollLimit() > 0 && inside(mx, my, scrollbarX() - 1, scrollTop(), 8, scrollBottom() - scrollTop())) {
            draggingScrollbar = true;
            jumpScroll(my);
            return true;
        }
        if (page == Page.PICKER && button == 0 && inside(mx, my, pickerX(), scrollTop(), pickerCellWidth() * pickerColumns(), scrollBottom() - scrollTop())) {
            var list = filtered();
            int localY = (int)(my - pickerTop()) + pickerScroll, localX = (int)(mx - pickerX());
            int i = localY / pickerStride() * pickerColumns() + localX / pickerCellWidth();
            if (i < list.size() && !pending && localY % pickerStride() < pickerStride() - 2 && localX % pickerCellWidth() < pickerCellWidth() - 2) {
                list.get(i).apply.run(); return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (dialogOpen()) return true;
        if ((page == Page.ROOM || page == Page.WAITING) && dy != 0
                && inside(mx, my, x + 10, spectatorTop(), w - 20, SPECTATOR_HEIGHT)) {
            spectatorOffset = Math.clamp(spectatorOffset + (dy < 0 ? 1 : -1), 0, spectatorMaxOffset());
            return true;
        }
        if (page == Page.CREATE && createMenu != null) return true;
        if ((page == Page.ROOM || page == Page.PRESETS) && move >= 0) {
            if (dy != 0 && inside(mx, my, moveListX(), moveListY(), moveListW(), moveListHeight()))
                moveOffset = Math.clamp(moveOffset + (dy < 0 ? 1 : -1), 0, Math.max(0, candidates(editingPick(slot)).size() - moveListRows()));
            return true;
        }
        if (page == Page.INVITE && dy != 0) {
            offset = Math.clamp(offset + (dy < 0 ? 1 : -1), 0, Math.max(0, inviteRows().size() - inviteVisible()));
            refreshInviteButtons();
            return true;
        }
        if (scrollPage() && dy != 0 && inside(mx, my, x + 8, scrollTop(), w - 16, scrollBottom() - scrollTop())) {
            setScroll(scroll() - (int) Math.round(dy * 24));
            return true;
        }
        if (page != Page.LOBBY || dy == 0) return super.mouseScrolled(mx, my, dx, dy);
        offset = Math.clamp(offset + (dy < 0 ? 1 : -1), 0, Math.max(0, Math.ceilDiv(listings().size(), roomColumns()) - roomRows()));
        rebuildWidgets();
        return true;
    }

    private boolean scrollPage() { return page == Page.ROOM || page == Page.PRESETS || page == Page.WAITING || page == Page.PICKER; }
    private int scrollTop() { return y + switch (page) {
        case PICKER -> pickerTop() - y;
        case WAITING -> 72;
        case ROOM -> 85;
        default -> 39;
    }; }
    private int scrollBottom() { return page == Page.ROOM || page == Page.WAITING ? spectatorTop() - 3 : y + h - 33; }
    private int scrollbarX() { return x + w - 16; }
    private int scroll() { return page == Page.ROOM ? draftScroll : page == Page.PRESETS ? presetScroll : page == Page.WAITING ? teamScroll : pickerScroll; }
    private int contentHeight() {
        if (page == Page.ROOM) return lineupHeight() * FrontierRoster.TEAM_SIZE;
        if (page == Page.PRESETS) return presets.size() * PRESET_STRIDE;
        if (page == Page.WAITING) {
            var seats = state.getList("seats", Tag.TAG_COMPOUND);
            int blue = 0;
            for (int i = 0; i < seats.size(); i++) if (seats.getCompound(i).getInt("side") == 0) blue++;
            return TEAM_STRIDE * Math.max(blue, seats.size() - blue);
        }
        return Math.ceilDiv(filtered().size(), pickerColumns()) * pickerStride();
    }
    private int scrollLimit() { return Math.max(0, contentHeight() - Math.max(1, scrollBottom() - scrollTop())); }
    private void setScroll(int pixels) {
        int value = Math.clamp(pixels, 0, scrollLimit());
        if (value == scroll()) return;
        if (page == Page.PICKER) pickerScroll = value;
        else {
            if (page == Page.PRESETS) presetScroll = value;
            else if (page == Page.ROOM) draftScroll = value;
            else teamScroll = value;
            rebuildWidgets();
        }
    }
    private void jumpScroll(double y) {
        int track = scrollBottom() - scrollTop(), thumb = scrollbarThumb(track, contentHeight());
        setScroll((int) Math.round((y - scrollTop() - thumb / 2.0) * scrollLimit() / Math.max(1, track - thumb)));
    }
    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && draggingScrollbar && scrollPage()) { jumpScroll(my); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }
    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && draggingScrollbar) { draggingScrollbar = false; return true; }
        return super.mouseReleased(mx, my, button);
    }
    private static int scrollbarThumb(int track, int content) { return Math.min(track, Math.max(12, (int) ((long) track * track / Math.max(1, content)))); }
    private static void drawScrollBar(GuiGraphics g, int x, int y, int track, int position, int limit, int content) {
        if (track <= 0 || limit <= 0) return;
        int thumb = scrollbarThumb(track, content);
        OreUiRenderer.scrollbar(g, x, y, 6, track, (track - thumb) * position / limit, thumb, false, false, 1, NEUTRAL);
    }
    private boolean inScroll(double mx, double my) { return inside(mx, my, x + 8, scrollTop(), w - 24, scrollBottom() - scrollTop()); }
    private void addContent(Renderable render) {
        if (!listContent) { addRenderableOnly(render); return; }
        addRenderableOnly((g, mx, my, dt) -> {
            g.enableScissor(x + 8, scrollTop(), scrollbarX(), scrollBottom());
            try { render.render(g, inScroll(mx, my) ? mx : -1, inScroll(mx, my) ? my : -1, dt); }
            finally { g.disableScissor(); }
        });
    }

    private interface Paint { void render(GuiGraphics g, Button b, float tick); }

    private Button control(int x, int y, int w, int h, Component label, boolean enabled, Runnable click, Paint paint) {
        return control(x, y, w, h, label, enabled, false, click, paint);
    }

    private Button control(int x, int y, int w, int h, Component label, boolean enabled, boolean tooltip, Runnable click, Paint paint) {
        boolean clip = listContent;
        var button = new Button(x, y, Math.max(1, w), h, label, b -> click.run(), narration -> narration.get()) {
            @Override public boolean mouseClicked(double mx, double my, int button) {
                return (!clip || inScroll(mx, my)) && super.mouseClicked(mx, my, button);
            }
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float tick) { paint.render(g, this, tick); }
        };
        button.active = enabled;
        if (tooltip && move < 0) button.setTooltip(Tooltip.create(label));
        if (clip) {
            addContent(button);
            if (!dialogOpen() && enabled && y < scrollBottom() && y + h > scrollTop()) addWidget(button);
            return button;
        }
        return dialogOpen() ? addRenderableOnly(button) : addRenderableWidget(button);
    }

    private void btn(int x, int y, int w, Component label, Runnable click) {
        btn(x, y, w, label, true, click);
    }

    private void btn(int x, int y, int w, Component label, boolean enabled, Runnable click) {
        btn(x, y, w, label, enabled, Action.SECONDARY, click);
    }

    private Button btn(int x, int y, int w, Component label, boolean enabled, Action style, Runnable click) {
        return control(x, y, w, 20, label, enabled, click, (g, b, tick) ->
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
        int outerWidth = width;
        var field = new EditBox(font, x + 5, y, Math.max(1, width - 10), 18, text("search")) {
            @Override public void renderWidget(GuiGraphics g, int mx, int my, float dt) {
                g.pose().pushPose();
                try {
                    if ((page == Page.ROOM || page == Page.PRESETS) && move >= 0) g.pose().translate(0, 0, 301);
                    OreUiRenderer.field(g, x, y, outerWidth, 18, isFocused(), 1, NEUTRAL);
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
        inspectedMove = null;
        shell(g, x, y, w, h);
        if (page == Page.LOBBY) FrontierConsole.text(g, font, title, x + 13, y + 12, w / 3, CYAN);
        if (page != Page.LOBBY) center(g, font, text(page == Page.PRESETS && managePresets ? "edit_presets"
                : page == Page.PICKER ? kind == Kind.SPECIES ? "picker_species" : "picker_held"
                : "panel." + page.name().toLowerCase(Locale.ROOT)),
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
            case INVITE -> drawInvites(g);
            case PRESETS -> {}
        }
        if (page == Page.ROOM || page == Page.WAITING) drawSpectators(g, mx, my);
        super.render(g, mx, my, delta);
        if (scrollPage() && move < 0) drawScrollBar(g, scrollbarX(), scrollTop(), scrollBottom() - scrollTop(), scroll(), scrollLimit(), contentHeight());
        if (page == Page.PICKER && generationsOpen) drawGenerations(g, mx, my);
        if (page == Page.CREATE && createMenu != null) drawCreateMenu(g, mx, my);
        if (inspectedMove != null && !dialogOpen()) {
            int panelWidth = Math.min(152, width - 8);
            int side = mx + 13 + panelWidth < width ? mx + 13 : mx - panelWidth - 6;
            g.pose().pushPose();
            try {
                g.pose().translate(0, 0, 390);
                FrontierConsole.summaryMoveDetails(g, font, inspectedMove, Math.clamp(side, 4, width - panelWidth - 4),
                        my + 8, panelWidth, height);
            } finally { g.pose().popPose(); }
        }
        var error = state.getString("problem");
        if (!error.isEmpty() && !ruleDialog(error) && !dialogOpen()) {
            var message = text("error." + error);
            int errorY = page == Page.ROOM || page == Page.WAITING ? y + h - 25 : y + h - 43;
            center(g, font, message, x + w / 2, errorY, w - 24, 0xffffaa94);
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

    private void dismissDialog() {
        dialogProblem = "";
        rebuildWidgets();
    }

    private void drawDialog() {
        int dw = Math.min(230, width - 16);
        int dx = (width - dw) / 2;
        var lines = font.split(text("error." + dialogProblem), dw - 28);
        int dh = 66 + lines.size() * 10;
        int dy = (height - dh) / 2;
        var ok = Button.builder(CommonComponents.GUI_OK, button -> dismissDialog())
                .bounds(width / 2 - 42, dy + dh - 28, 84, 20).build();
        addWidget(ok);
        setInitialFocus(ok);
        addRenderableOnly((g, mx, my, delta) -> {
            g.pose().pushPose();
            try {
                g.pose().translate(0, 0, 400);
                g.fill(0, 0, width, height, 0xa0000000);
                OreUiRenderer.panel(g, dx, dy, dw, dh, 1, NEUTRAL);
                center(g, font, text("mega_limit".equals(dialogProblem) ? "mega_limit_title" : "repeat_species_title"), width / 2, dy + 12, dw - 24, GOLD);
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
            var host = state.getCompound("host");
            boolean owner = seat.hasUUID("player") && host.hasUUID("player") && seat.getUUID("player").equals(host.getUUID("player"));
            if (owner) g.renderOutline(avatarX - 2, y + 39, 20, 20, GOLD);
            player(g, seat, avatarX, y + 41, 16, owner ? GOLD : accent);
            var name = seat.isEmpty() ? Component.literal("—") : seatLabel(seat);
            center(g, font, name, sx + ww / 2, y + 45, ww - 46, seatColor(seat));
        }
        int middle = x + w / 2, rail = centerRail(), top = scrollTop();
        int railH = scrollBottom() - top - 3;
        card(g, middle - rail / 2, top, rail, railH, GOLD, false);
        var vs = Component.literal("VS").withStyle(ChatFormatting.BOLD);
        float scale = Math.min(4F, (rail - 10) / (float) font.width(vs));
        int vsHeight = (int) Math.ceil(font.lineHeight * scale);
        int vsY = top + Math.max(3, (railH - vsHeight - 27) / 2);
        int stakeY = vsY + vsHeight + 5;
        g.pose().pushPose();
        try {
            g.pose().translate(middle, vsY, 0);
            g.pose().scale(scale, scale, 1);
            g.drawCenteredString(font, vs, 0, 0, GOLD);
        } finally { g.pose().popPose(); }
        center(g, font, text("stake_label"), middle, stakeY, rail - 6, MUTED);
        center(g, font, money(state), middle, stakeY + 13, rail - 4, GOLD);
    }

    private static final int SPECTATOR_HEIGHT = 45, SPECTATOR_CELL = 43;
    private int spectatorTop() { return y + h - 29 - SPECTATOR_HEIGHT; }
    private int spectatorColumns() { return Math.max(1, (w - (page == Page.WAITING ? 165 : 92)) / SPECTATOR_CELL); }
    private int spectatorMaxOffset() {
        return Math.max(0, state.getList("spectators", Tag.TAG_COMPOUND).size() - spectatorColumns());
    }

    private void spectatorControls() {
        if (page != Page.WAITING || watching()) return;
        int bx = x + w - 81, by = spectatorTop() + 5, bw = 65, bh = SPECTATOR_HEIGHT - 10;
        control(bx, by, bw, bh, text("join_watch"), true, true,
                () -> request("side", tag -> tag.putInt("side", -1)), (g, b, dt) -> {
                    boolean hover = b.isHoveredOrFocused();
                    OreUiRenderer.field(g, bx, by, bw, bh, hover, 1, GOLD);
                    center(g, font, Component.literal("+"), bx + bw / 2, by + 5, bw - 6, GOLD);
                    center(g, font, b.getMessage(), bx + bw / 2, by + 21, bw - 6, hover ? TEXT : MUTED);
                });
    }

    private void drawSpectators(GuiGraphics g, int mx, int my) {
        var seats = state.getList("spectators", Tag.TAG_COMPOUND);
        int left = x + 10, top = spectatorTop();
        card(g, left, top, w - 20, SPECTATOR_HEIGHT, GOLD, false);
        FrontierConsole.text(g, font, text("spectator_count", seats.size()), left + 6, top + 16, 62, GOLD);
        spectatorOffset = Math.clamp(spectatorOffset, 0, spectatorMaxOffset());
        var host = state.getCompound("host");
        for (int i = 0; i < spectatorColumns(); i++) {
            int slotX = left + 68 + i * SPECTATOR_CELL, slotY = top + 4;
            int slotW = SPECTATOR_CELL - 3, slotH = SPECTATOR_HEIGHT - 8;
            OreUiRenderer.field(g, slotX, slotY, slotW, slotH, false, 1, NEUTRAL);
            if (i + spectatorOffset >= seats.size()) continue;
            var seat = seats.getCompound(i + spectatorOffset);
            int px = slotX + (slotW - 20) / 2, py = slotY + 2;
            if (host.hasUUID("player") && seat.hasUUID("player") && host.getUUID("player").equals(seat.getUUID("player")))
                g.renderOutline(px - 1, py - 1, 22, 22, GOLD);
            player(g, seat, px, py, 20, GOLD);
            var name = Component.literal(seat.getString("name"));
            center(g, font, name, slotX + slotW / 2, slotY + 24, slotW - 4, TEXT);
            if (!dialogOpen() && inside(mx, my, slotX, slotY, slotW, slotH)) setTooltipForNextRenderPass(name);
        }
    }

    private void drawPicker(GuiGraphics g, int mx, int my, float dt) {
        var list = filtered();
        int columns = pickerColumns(), stride = pickerStride(), cell = pickerCellWidth();
        pickerScroll = Math.clamp(pickerScroll, 0, scrollLimit());
        int top = scrollTop(), bottom = scrollBottom();
        g.enableScissor(pickerX(), top, scrollbarX(), bottom);
        try {
            for (int i = pickerScroll / stride * columns; i < list.size(); i++) {
                int at = top + i / columns * stride - pickerScroll, bx = pickerX() + i % columns * cell;
                if (at >= bottom) break;
                var choice = list.get(i);
                boolean hover = !generationsOpen && inScroll(mx, my) && inside(mx, my, bx, at, cell - 2, stride - 2);
                if (kind == Kind.SPECIES) {
                    card(g, bx, at, cell - 2, stride - 2, CYAN, hover);
                    int size = Math.min(29, cell - 6);
                    var species = ResourceLocation.tryParse(choice.id);
                    if (species != null) portraits.render(g, species, Set.of(), bx + (cell - 2 - size) / 2, at + 2, size, dt);
                    center(g, font, choice.name, bx + (cell - 2) / 2, at + stride - 15, cell - 8, TEXT);
                } else {
                    var stack = itemStack(ResourceLocation.tryParse(choice.id));
                    OreUiRenderer.field(g, bx, at, 21, 21, hover, 1, NEUTRAL);
                    FrontierConsole.item(g, stack, bx + 2, at + 2, 16);
                    if (hover) itemTooltip(stack);
                }
            }
        } finally { g.disableScissor(); }
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
        if (dialogOpen()) { dismissDialog(); return; }
        if (createMenu != null) { createMenu = null; return; }
        if (generationsOpen) { generationsOpen = false; rebuildWidgets(); return; }
        if (move >= 0) { move = -1; rebuildWidgets(); return; }
        if (page == Page.PICKER && editingPreset) { page(Page.PRESETS); return; }
        if (page == Page.PRESETS) { page(destination(state)); return; }
        if (page == Page.PICKER && state.hasUUID("room")) { pinRoom(); return; }
        if (page == Page.CREATE) { page(Page.LOBBY); return; }
        if (page == Page.INVITE && state.hasUUID("room")) { pinRoom(); return; }
        if (state.hasUUID("room")) { pinRoom(); return; }
        super.onClose();
    }

    private void itemTooltip(ItemStack stack) {
        var lines = new ArrayList<>(getTooltipFromItem(minecraft, stack));
        if (lines.size() < 2) {
            var language = net.minecraft.locale.Language.getInstance();
            String itemKey = stack.getItem().getDescriptionId();
            String tooltipKey = itemKey.startsWith("item.") ? "tooltip." + itemKey.substring(5) + ".tooltip" : "";
            if (language.has(itemKey + ".tooltip")) lines.add(Component.translatable(itemKey + ".tooltip"));
            else if (!tooltipKey.isEmpty() && language.has(tooltipKey)) lines.add(Component.translatable(tooltipKey));
            else lines.add(text(FrontierRoster.isMega(stack) ? "mega_item_info" : "held_item_info"));
        }
        setTooltipForNextRenderPass(lines.stream().flatMap(line -> font.split(line, 235).stream()).toList());
    }

    private static Component moveName(String id) { return Component.translatable("cobblemon.move." + id); }
    private static Component text(String key, Object... args) { return Component.translatable(KEY + key, args); }
}
