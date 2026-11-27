package com.p1nero.cobblemon.rumblecore.client.combat;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.client.gui.MoveCategoryIcon;
import com.cobblemon.mod.common.client.gui.TypeIcon;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TrialPortraits;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.arcadia.arc_quest.client.hud.style.OrePalette;
import org.arcadia.arc_quest.client.hud.style.OreUiRenderer;

/** Paints the simulator chassis and its controls; room rules live on the server. */
final class FrontierConsole {
    static final int CYAN = 0xff73dff2, PINK = 0xfff291bd, GOLD = 0xffffd689;
    static final int TEXT = 0xff000000 | OrePalette.TEXT, MUTED = 0xff000000 | OrePalette.MUTED;
    static final int NEUTRAL = 0xffa5a6a5;
    private static final ResourceLocation PARTY_FACE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/party/party_slot_portrait_background.png");
    private static final ResourceLocation UP = asset("summary_move_reorder_up");
    private static final ResourceLocation DOWN = asset("summary_move_reorder_down");
    private static final ResourceLocation SWAP = asset("summary_move_swap");
    private static final ResourceLocation NATIVE_MOVE = asset("summary_move_condensed");
    private static final ResourceLocation NATIVE_MOVE_TINT = asset("summary_move_overlay_condensed");
    private static final ResourceLocation MOVE_INFO = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/tmmachine/tooltip_move_info.png");

    private FrontierConsole() {}

    static void shell(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xa0101012);
        OreUiRenderer.panel(g, x, y, w, h, 1, NEUTRAL);
        OreUiRenderer.field(g, x + 8, y + 30, w - 16, h - 62, false, 1, NEUTRAL);
    }

    static void card(GuiGraphics g, int x, int y, int w, int h, int accent, boolean highlighted) {
        OreUiRenderer.card(g, x, y, w, h, highlighted, highlighted, 1, accent);
    }

    /** A shallow Ore bevel leaves short labels a full line instead of reserving a fixed three-pixel foot. */
    static void shallowCard(GuiGraphics g, int x, int y, int w, int h, int accent, boolean selected) {
        if (w < 4 || h < 4) return;
        int foot = Math.clamp(h / 11, 1, 2);
        g.fill(x, y, x + w, y + h, 0xFF131416);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1 - foot, selected ? 0xFF42484B : 0xFF383B3F);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, selected ? accent : 0xFF686C70);
        g.fill(x + 1, y + h - 1 - foot, x + w - 1, y + h - 1, 0xFF25272B);
    }

    static void placeholder(GuiGraphics g, Font font, int x, int y, int size) {
        OreUiRenderer.field(g, x, y, size, size, false, 1, NEUTRAL);
        center(g, font, Component.literal("?"), x + size / 2, y + (size - 8) / 2, size, MUTED);
    }

    private static ResourceLocation asset(String path) {
        return ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/summary/" + path + ".png");
    }

    static boolean partyFace(GuiGraphics g, TrialPortraits portraits,
                             ResourceLocation species, Set<String> aspects, int x, int y, int size,
                             float tick, boolean mirror, int accent) {
        if (size < 5) return false;
        OreUiRenderer.field(g, x, y, size, size, false, 1, accent);
        g.blit(PARTY_FACE, x + 1, y + 1, size - 2, size - 2, 0, 0, 21, 21, 21, 21);
        return portraits.drawFace(g, species, aspects, x + 2, y + 2, size - 4, tick, mirror);
    }

    /** Reuse the Summary move graphics without invoking its party-mutating move-swap handler. */
    static void summaryMove(GuiGraphics g, Font font, MoveTemplate move, int x, int y, int w, int h, boolean selected) {
        var tint = com.cobblemon.mod.common.util.math.SimpleMathExtensionsKt.toRGB(move.getElementalType().getHue());
        g.flush();
        g.setColor(tint.getFirst().floatValue(), tint.getSecond().floatValue(), tint.getThird().floatValue(), 1F);
        try { g.blit(NATIVE_MOVE, x, y, w, h, 0, selected ? 18 : 0, 91, 18, 91, 36); }
        finally { g.setColor(1, 1, 1, 1); }
        g.blit(NATIVE_MOVE_TINT, x, y, w, h, 0, 0, 91, 18, 91, 18);
        typeIcon(g, move.getElementalType(), x + 4, y + (h - 14) / 2, 14);
        new MoveCategoryIcon(x + w - 16, y + (h - 9) / 2., move.getDamageCategory(), 1).render(g);
        moveLabel(g, font, RealtimeMoveShapeUi.displayNameWithShape(move), x + 22, y + (h - 8) / 2, Math.max(1, w - 42), TEXT);
    }

    static void summaryMoveDetails(GuiGraphics g, Font font, MoveTemplate move, int x, int anchorY, int width, int screenHeight) {
        int pad = 8;
        var description = font.split(move.getDescription(), width - 2 * pad);
        var realtimeShape = RealtimeMoveShapeUi.shapeDescription(move);
        int shapeLines = 1 + font.split(realtimeShape, width - 2 * pad).size();
        int panelH = Math.min(screenHeight - 8, Math.max(69, 46 + (description.size() + shapeLines) * 10));
        int y = Math.clamp(anchorY, 4, Math.max(4, screenHeight - panelH - 4));
        g.blit(MOVE_INFO, x, y, width, panelH, 0, 0, 96, 98, 96, 98);
        // An opaque center keeps localized descriptions legible over the underlying team cards.
        g.fill(x + 3, y + 3, x + width - 3, y + panelH - 3, 0xff1b2224);
        typeIcon(g, move.getElementalType(), x + pad, y + 8, 15);
        new MoveCategoryIcon(x + width - 20, y + 11, move.getDamageCategory(), 1).render(g);
        moveLabel(g, font, RealtimeMoveShapeUi.displayNameWithShape(move).withStyle(net.minecraft.ChatFormatting.BOLD), x + pad + 19, y + 11, width - 48, TEXT);
        summaryStat(g, font, asset("summary_moves_icon_power"), x + pad, y + 28,
                move.getPower() > 0 ? Integer.toString((int) move.getPower()) : "—");
        summaryStat(g, font, asset("summary_moves_icon_accuracy"), x + pad + (width - 2 * pad) / 3, y + 28,
                move.getAccuracy() > 0 ? (int) move.getAccuracy() + "%" : "—");
        g.drawString(font, "PP " + move.getPp(), x + pad + 2 * (width - 2 * pad) / 3, y + 29, GOLD, false);
        int lineY = y + 43;
        for (int i = 0; i < description.size() && lineY < y + panelH - 4; i++, lineY += 10) {
            g.drawString(font, description.get(i), x + pad, lineY, TEXT, false);
        }
        // Frontier's existing move card owns this hover panel; keep RTC's type
        // explanation inside it instead of drawing a second tooltip above it.
        if (lineY < y + panelH - 4) {
            g.drawString(font, RealtimeMoveShapeUi.shapeLabel(move), x + pad, lineY, 0xffd8b9ff, false);
            lineY += 10;
            for (var line : font.split(realtimeShape, width - 2 * pad)) {
                if (lineY >= y + panelH - 4) break;
                g.drawString(font, line, x + pad, lineY, 0xffc5cbd6, false);
                lineY += 10;
            }
        }
    }

    private static void summaryStat(GuiGraphics g, Font font, ResourceLocation icon, int x, int y, String value) {
        g.blit(icon, x, y, 0, 0, 10, 10, 10, 10);
        g.drawString(font, value, x + 13, y + 1, GOLD, false);
    }

    static void typeIcon(GuiGraphics g, ElementalType type, float x, float y, int size) {
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 0);
            g.pose().scale(size / 18F, size / 18F, 1);
            new TypeIcon(0, 0, type, null, false, false, 0, 0, 1).render(g);
        } finally { g.pose().popPose(); }
    }

    static void moveTool(GuiGraphics g, int x, int y, int w, int h, int direction, boolean active, boolean hover) {
        int texW = direction == 0 ? 12 : 8, texH = direction == 0 ? 36 : 12, glyphH = direction == 0 ? 12 : 6;
        float scale = Math.min(1F, Math.min((w - 1F) / texW, h / (float) glyphH));
        g.pose().pushPose();
        if (!active) g.setColor(.46f, .46f, .46f, 1);
        try {
            g.pose().translate(x + (w - texW * scale) / 2, y + (h - glyphH * scale) / 2, 0);
            g.pose().scale(scale, scale, 1);
            g.blit(direction < 0 ? UP : direction > 0 ? DOWN : SWAP, 0, 0,
                    0, hover && active ? glyphH : 0, texW, glyphH, texW, texH);
        } finally {
            g.setColor(1, 1, 1, 1);
            g.pose().popPose();
        }
    }

    static void botIcon(GuiGraphics g, int x, int y, int size, int accent) {
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 0);
            g.pose().scale(size / 12F, size / 12F, 1);
            g.fill(5, 2, 7, 4, 0xff93a7b8); // Antenna stalk
            g.fill(5, 1, 7, 2, accent);
            g.fill(1, 6, 11, 9, accent); // Ear caps
            g.fill(3, 4, 9, 11, 0xffb4c7d6);
            g.fill(2, 5, 10, 10, 0xffb4c7d6);
            g.fill(3, 4, 9, 5, 0xffecf4fb);
            g.fill(3, 6, 9, 9, 0xff142535); // Visor
            g.fill(4, 7, 5, 8, accent);
            g.fill(7, 7, 8, 8, accent);
            g.fill(5, 10, 7, 11, 0xff637e91);
        } finally { g.pose().popPose(); }
    }

    static void player(GuiGraphics g, CompoundTag seat, int x, int y, int size, int accent) {
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, accent);
        if (seat.getBoolean("bot")) {
            g.fill(x, y, x + size, y + size, 0xff122131);
            botIcon(g, x, y, size, accent);
            return;
        }
        if (!seat.hasUUID("player")) {
            g.fill(x, y, x + size, y + size, 0xff122131);
            return;
        }
        var connection = Minecraft.getInstance().getConnection();
        var id = seat.getUUID("player");
        var player = connection == null ? null : connection.getPlayerInfo(id);
        PlayerFaceRenderer.draw(g, player == null ? DefaultPlayerSkin.get(id) : player.getSkin(), x, y, size);
    }

    /** Keep names and their suffixes together on narrow preset cards. */
    static void moveLabel(GuiGraphics g, Font font, Component label, int x, int y, int max, int color) {
        if (max < 1) return;
        float scale = Math.min(1F, (float) max / Math.max(1, font.width(label)));
        g.pose().pushPose();
        try {
            g.pose().translate(x, y + (1F - scale) * font.lineHeight / 2F, 0);
            g.pose().scale(scale, scale, 1);
            g.drawString(font, label, 0, 0, color, false);
        } finally { g.pose().popPose(); }
    }

    static void text(GuiGraphics g, Font font, Component label, int x, int y, int max, int color) {
        g.drawString(font, font.substrByWidth(label, Math.max(0, max)).getString(), x, y, color, false);
    }

    static void center(GuiGraphics g, Font font, Component label, int center, int y, int max, int color) {
        var clipped = font.substrByWidth(label, Math.max(0, max));
        g.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(clipped),
                center - font.width(clipped) / 2, y, color, false);
    }

    static void item(GuiGraphics g, ItemStack item, int x, int y, int size) {
        if (item.isEmpty()) return;
        g.pose().pushPose();
        try {
            g.pose().translate(x, y, 0);
            g.pose().scale(size / 16F, size / 16F, 1);
            g.renderItem(item, 0, 0);
        } finally { g.pose().popPose(); }
    }

}
