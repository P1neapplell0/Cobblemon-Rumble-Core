package com.p1nero.cobblemon.rumblecore.client.combat;

import com.cobblemon.mod.common.api.moves.Moves;
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

    private FrontierConsole() {}

    static void shell(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xa0101012);
        OreUiRenderer.panel(g, x, y, w, h, 1, NEUTRAL);
        OreUiRenderer.field(g, x + 8, y + 30, w - 16, h - 62, false, 1, NEUTRAL);
    }

    static void card(GuiGraphics g, int x, int y, int w, int h, int accent, boolean highlighted) {
        OreUiRenderer.card(g, x, y, w, h, highlighted, highlighted, 1, accent);
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

    /** Ore UI rows with Cobblemon's elemental and damage-category glyphs. */
    static void moveStrip(GuiGraphics g, Font font, String id, Component label, int x, int y, int width, int height, boolean active) {
        var move = id.isEmpty() ? null : Moves.getByName(id);
        OreUiRenderer.card(g, x, y, width, height, active, active, 1, move == null ? NEUTRAL : 0xff000000 | move.getElementalType().getPrimaryColor());
        if (move == null) {
            text(g, font, label, x + 7, y + (height - 8) / 2, width - 13, MUTED);
            return;
        }
        typeIcon(g, move.getElementalType(), x + 4, y + (height - 18) / 2, 18);
        new MoveCategoryIcon(x + width - 19, y + (height - 9) / 2., move.getDamageCategory(), 1).render(g);
        text(g, font, label, x + 28, y + (height - 8) / 2, width - 51, TEXT);
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

    static void player(GuiGraphics g, CompoundTag seat, int x, int y, int size, int accent) {
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, accent);
        if (!seat.hasUUID("player")) {
            g.fill(x, y, x + size, y + size, 0xff122131);
            return;
        }
        var connection = Minecraft.getInstance().getConnection();
        var id = seat.getUUID("player");
        var player = connection == null ? null : connection.getPlayerInfo(id);
        PlayerFaceRenderer.draw(g, player == null ? DefaultPlayerSkin.get(id) : player.getSkin(), x, y, size);
    }

    static void text(GuiGraphics g, Font font, Component label, int x, int y, int max, int color) {
        g.drawString(font, font.substrByWidth(label, Math.max(0, max)).getString(), x, y, color, false);
    }

    static void center(GuiGraphics g, Font font, Component label, int center, int y, int max, int color) {
        var clipped = font.substrByWidth(label, Math.max(0, max));
        g.drawString(font, clipped.getString(), center - font.width(clipped) / 2, y, color, false);
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
