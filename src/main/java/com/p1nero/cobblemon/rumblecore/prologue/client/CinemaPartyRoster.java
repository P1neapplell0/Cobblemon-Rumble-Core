package com.p1nero.cobblemon.rumblecore.prologue.client;

import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TrialPortraits;
import com.p1nero.cobblemon.rumblecore.gym.CinemaTrials;
import com.p1nero.misc.custom_instances.client.screen.FantasyScreen;
import com.p1nero.misc.custom_instances.client.widget.PartyRosterWidget;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

/** Keep each compact role portrait aligned with its player. */
public final class CinemaPartyRoster extends AbstractWidget {
    private static final int ROLE_Y = 61, ROLE_SIZE = 24;
    private static final int RED = 0xFFFF6565;
    private final ListTag members;
    private final CompoundTag roles;
    private final CompoundTag selections;
    private final CinemaTrials.Trial trial;
    private final TrialPortraits portraits;
    private final UUID viewer;
    private final boolean canKick, canChoose;
    private final Runnable choose;
    private final Consumer<UUID> kick;

    public CinemaPartyRoster(int x, int y, int width, CompoundTag party, CinemaTrials.Trial trial, TrialPortraits portraits,
                             UUID viewer, boolean canKick, Runnable choose, Consumer<UUID> kick) {
        super(x, y, width, 100, text("choose_role"));
        this.members = party.getList("members", Tag.TAG_COMPOUND);
        this.roles = party.getCompound("rumbleCinema");
        this.selections = roles.getCompound("selections");
        this.trial = trial;
        this.portraits = portraits;
        this.viewer = viewer;
        this.canKick = canKick;
        this.canChoose = party.getString("state").equals("WAITING");
        this.choose = choose;
        this.kick = kick;
    }

    private int cellWidth() { return Math.min(106, width / Math.max(1, members.size())); }
    private int center(int index) { return getX() + (width - members.size() * cellWidth()) / 2 + cellWidth() * index + cellWidth() / 2; }
    private int frameLeft(int cx) {
        int label = Minecraft.getInstance().font.width(text("role_label")) + 3;
        return cx + (label - ROLE_SIZE) / 2;
    }

    @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        var font = Minecraft.getInstance().font;
        Component tooltip = null;
        int y = getY();
        for (int i = 0; i < members.size(); i++) {
            var member = members.getCompound(i);
            UUID id = member.getUUID("id");
            boolean own = id.equals(viewer), ready = member.getBoolean("ready");
            int cx = center(i), x = frameLeft(cx), roleY = y + ROLE_Y;
            g.fill(cx - 16, y + 2, cx + 16, y + 34, ready ? 0xFF729853 : 0xFF98704D);
            PartyRosterWidget.face(g, member, cx - 14, y + 4, 28);
            if (member.getBoolean("owner")) g.drawString(font, "♛", cx - 25, y + 1, OreTheme.COINS, false);
            if (canKick && !member.getBoolean("owner")) {
                boolean hovered = kickHit(cx, mouseX, mouseY);
                g.fill(cx + 10, y, cx + 24, y + 14, hovered ? 0xFFD45948 : 0xFF783A30);
                g.drawCenteredString(font, "×", cx + 17, y + 3, OreTheme.TEXT);
                if (hovered) tooltip = FantasyScreen.text("kick_member", member.getString("name"));
            }
            g.drawCenteredString(font, font.plainSubstrByWidth(member.getString("name"), cellWidth() - 4), cx, y + 37, OreTheme.TEXT);
            g.drawCenteredString(font, FantasyScreen.text(ready ? "member_ready" : "member_not_ready"), cx, y + 49,
                    ready ? OreTheme.GREEN : OreTheme.COINS);

            var selection = selections.getCompound(id.toString());
            boolean conflict = selection.getBoolean("conflict"), hover = roleHit(cx, mouseX, mouseY);
            var cast = selection.contains("slot") ? trial.castAt(selection.getInt("side"), selection.getInt("slot")) : null;
            var label = text("role_label");
            g.drawString(font, label, x - 3 - font.width(label), roleY + 8, OreTheme.TEXT, false);
            int border = conflict ? RED : own && canChoose && (hover || isFocused()) ? OreTheme.TEXT : 0xFF202024;
            g.fill(x, roleY, x + ROLE_SIZE, roleY + ROLE_SIZE, border);
            g.fill(x + 1, roleY + 1, x + ROLE_SIZE - 1, roleY + ROLE_SIZE - 1,
                    conflict ? 0xFF54292C : own && canChoose && hover ? 0xFF505057 : 0xFF2F2E33);
            if (cast != null) {
                if (!portraits.drawFace(g, cast.speciesId(), cast.aspects(), x + 2, roleY + 2, ROLE_SIZE - 4, partial, false))
                    portraits.render(g, cast.speciesId(), cast.aspects(), x + 2, roleY + 2, ROLE_SIZE - 4, partial);
            } else {
                g.drawCenteredString(font, "?", x + ROLE_SIZE / 2, roleY + 8, OreTheme.MUTED);
            }
            if (hover) {
                tooltip = own && canChoose ? text("choose_role_hint") : cast == null ? text("role_not_selected") : null;
                if (conflict) tooltip = tooltip == null ? text("role_conflict") : tooltip.copy().append("\n").append(text("role_conflict"));
            }
        }
        if (roles.getBoolean("conflict")) g.drawCenteredString(font, text("role_conflict"), getX() + width / 2, y + 89, RED);
        if (tooltip != null) g.renderTooltip(font, tooltip, mouseX, mouseY);
    }

    private boolean roleHit(int cx, double x, double y) {
        int left = frameLeft(cx);
        return x >= left && x < left + ROLE_SIZE && y >= getY() + ROLE_Y && y < getY() + ROLE_Y + ROLE_SIZE;
    }

    private boolean kickHit(int cx, double x, double y) {
        return x >= cx + 10 && x < cx + 24 && y >= getY() && y < getY() + 14;
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button != 0 || !isMouseOver(x, y)) return false;
        for (int i = 0; i < members.size(); i++) {
            var member = members.getCompound(i);
            UUID id = member.getUUID("id");
            if (canChoose && id.equals(viewer) && roleHit(center(i), x, y)) {
                playDownSound(Minecraft.getInstance().getSoundManager());
                choose.run();
                return true;
            }
            if (canKick && !member.getBoolean("owner") && kickHit(center(i), x, y)) {
                kick.accept(id);
                return true;
            }
        }
        return false;
    }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (!canChoose || !CommonInputs.selected(key)) return false;
        choose.run();
        return true;
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    private static Component text(String key) { return Component.translatable("rumble.cinema." + key); }
}
