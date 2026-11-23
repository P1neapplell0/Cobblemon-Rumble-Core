package com.p1nero.cobblemon.rumblecore.client.quest;

import com.johnn.realtimecobblemon.client.ClientSmash;
import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.arcadia.arc_quest.client.events.ClientEventHandler;
import org.arcadia.arc_quest.client.hud.HudAnimUtil;
import org.arcadia.arc_quest.client.hud.HudRenderUtil;
import org.arcadia.arc_quest.quest.api.QuestCategory;

/** Queued ArcQuest-style notice shown when a Rumble Core quest is accepted. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class AdventureQuestAcceptedOverlay {
    private static final long ENTER_MILLIS = 420L;
    private static final long HOLD_MILLIS = 4_180L;
    private static final long EXIT_MILLIS = 400L;
    private static final long TOTAL_MILLIS = ENTER_MILLIS + HOLD_MILLIS + EXIT_MILLIS;
    private static final int MIN_WIDTH = 250;
    private static final int MAX_WIDTH = 340;
    private static final int MAX_TITLE_LINES = 2;
    private static final int MAX_DESCRIPTION_LINES = 4;
    private static final int THEME = QuestCategory.ADVENTURE.getThemeColor();
    private static final int BACKGROUND = 0x11141D;
    private static final Deque<Notice> QUEUE = new ArrayDeque<>();

    private static Notice active;
    private static long startedAt;

    private AdventureQuestAcceptedOverlay() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hud(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen != null) return;
        var graphics = event.getGuiGraphics();
        render(graphics, graphics.guiWidth(), graphics.guiHeight());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void screen(ScreenEvent.Render.Post event) {
        render(event.getGuiGraphics(), event.getScreen().width, event.getScreen().height);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    public static void show(Component title, Component description) {
        Notice notice = new Notice(title.copy(), description.copy());
        if (active == null) {
            active = notice;
            startedAt = Util.getMillis();
        } else {
            QUEUE.addLast(notice);
        }
    }

    public static boolean isActive() {
        return active != null;
    }

    public static void clear() {
        active = null;
        QUEUE.clear();
        startedAt = 0L;
    }

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (ClientSmash.active()) return;
        if (active == null) return;
        long elapsed = Util.getMillis() - startedAt;
        if (elapsed >= TOTAL_MILLIS) {
            active = QUEUE.pollFirst();
            if (active == null) {
                startedAt = 0L;
                return;
            }
            startedAt = Util.getMillis();
            elapsed = 0L;
        }

        float reveal;
        float visibility;
        float textVisibility;
        float offsetY;
        if (elapsed < ENTER_MILLIS) {
            float progress = elapsed / (float) ENTER_MILLIS;
            reveal = HudAnimUtil.easeOutQuintic(progress);
            visibility = HudAnimUtil.easeOutCubic(progress);
            textVisibility = Mth.clamp((progress - 0.22F) / 0.78F, 0.0F, 1.0F);
            offsetY = 12.0F * (1.0F - HudAnimUtil.easeOutBack(progress));
        } else if (elapsed < ENTER_MILLIS + HOLD_MILLIS) {
            reveal = 1.0F;
            visibility = 1.0F;
            textVisibility = 1.0F;
            offsetY = 0.0F;
        } else {
            float progress = Math.min(1.0F,
                    (elapsed - ENTER_MILLIS - HOLD_MILLIS) / (float) EXIT_MILLIS);
            float eased = HudAnimUtil.easeInQuartic(progress);
            reveal = 1.0F - eased;
            visibility = 1.0F - HudAnimUtil.easeInCubic(progress);
            textVisibility = 1.0F - eased;
            offsetY = -10.0F * eased;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        float uiScale = HudRenderUtil.getUniversalUiScale(screenWidth, screenHeight);
        int virtualWidth = Math.round(screenWidth / uiScale);
        int virtualHeight = Math.round(screenHeight / uiScale);
        Component heading = Component.translatable(
                "rumble.quest_received.title", active.title());
        Component summaryLabel = Component.translatable(
                "rumble.quest_received.summary");
        Component footer = Component.translatable(
                "rumble.quest_received.open",
                ClientEventHandler.KEY_OPEN_JOURNAL.getTranslatedKeyMessage());

        int maxAvailable = Math.max(220, virtualWidth - 32);
        int naturalWidth = Math.max(font.width(heading) + 68, font.width(footer) + 52);
        int panelWidth = Math.min(maxAvailable,
                Mth.clamp(naturalWidth, MIN_WIDTH, MAX_WIDTH));
        int textWidth = panelWidth - 34;
        List<FormattedCharSequence> headingLines = limitedLines(font, heading, textWidth,
                MAX_TITLE_LINES);
        List<FormattedCharSequence> descriptionLines = limitedLines(font,
                active.description(), textWidth, MAX_DESCRIPTION_LINES);
        List<FormattedCharSequence> footerLines = limitedLines(font, footer, textWidth, 2);

        int headerHeight = 24 + headingLines.size() * 12;
        int descriptionHeight = 18 + Math.max(1, descriptionLines.size()) * 11;
        int footerHeight = 14 + footerLines.size() * 10;
        int panelHeight = headerHeight + descriptionHeight + footerHeight;
        int centerX = virtualWidth / 2;
        int panelX = centerX - panelWidth / 2;
        int panelY = Math.max(24,
                Math.round(virtualHeight * 0.30F - panelHeight / 2.0F + offsetY));
        int animatedWidth = Math.max(4, Math.round(panelWidth * reveal));
        int animatedX = centerX - animatedWidth / 2;
        int textAlpha = Math.round(255 * textVisibility);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 9_500.0F);
        graphics.pose().scale(uiScale, uiScale, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        drawArcQuestPanel(graphics, animatedX, panelY, animatedWidth, panelHeight,
                visibility, elapsed, reveal);
        if (textVisibility > 0.02F) {
            int firstDivider = panelY + headerHeight;
            int secondDivider = firstDivider + descriptionHeight;
            int lineHalfWidth = Math.round((panelWidth - 34) * textVisibility / 2.0F);
            int lineColor = HudAnimUtil.withAlpha(THEME, Math.round(115 * textVisibility));
            graphics.fill(centerX - lineHalfWidth, firstDivider,
                    centerX + lineHalfWidth, firstDivider + 1, lineColor);
            graphics.fill(centerX - lineHalfWidth, secondDivider,
                    centerX + lineHalfWidth, secondDivider + 1, lineColor);

            float contentOffset = (1.0F - textVisibility) * 8.0F;
            drawScaledCenteredText(graphics, font,
                    Component.translatable(
                            "rumble.quest_received.system"),
                    centerX, Math.round(panelY + 7 + contentOffset),
                    HudAnimUtil.withAlpha(THEME, Math.round(205 * textVisibility)), 0.72F);
            int y = Math.round(panelY + 18 + contentOffset);
            for (FormattedCharSequence line : headingLines) {
                drawCenteredLine(graphics, font, line, centerX, y,
                        HudAnimUtil.withAlpha(0xFFFFFF, textAlpha), 1.08F);
                y += 12;
            }

            y = Math.round(firstDivider + 5 + contentOffset);
            drawScaledCenteredText(graphics, font, summaryLabel, centerX, y,
                    HudAnimUtil.withAlpha(THEME, Math.round(220 * textVisibility)), 0.78F);
            y += 11;
            if (descriptionLines.isEmpty()) {
                drawScaledCenteredText(graphics, font, Component.literal("..."), centerX, y,
                        HudAnimUtil.withAlpha(0xC8CDD6, textAlpha), 0.92F);
            } else {
                for (FormattedCharSequence line : descriptionLines) {
                    drawCenteredLine(graphics, font, line, centerX, y,
                            HudAnimUtil.withAlpha(0xD3D7DF, textAlpha), 0.92F);
                    y += 11;
                }
            }

            y = Math.round(secondDivider + 7 + contentOffset);
            float pulse = 0.82F + 0.18F
                    * (float) Math.sin(elapsed / 230.0F) * (float) Math.sin(elapsed / 230.0F);
            for (FormattedCharSequence line : footerLines) {
                drawCenteredLine(graphics, font, line, centerX, y,
                        HudAnimUtil.withAlpha(THEME,
                                Math.round(255 * textVisibility * pulse)), 0.86F);
                y += 10;
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }

    private static void drawArcQuestPanel(GuiGraphics graphics, int x, int y, int width,
                                          int height, float visibility, long elapsed,
                                          float reveal) {
        if (width <= 4) return;
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x + 3, y + 4, right + 3, bottom + 4,
                HudAnimUtil.withAlpha(0x000000, Math.round(90 * visibility)));
        graphics.fillGradient(x, y, right, bottom,
                HudAnimUtil.withAlpha(0x181D29, Math.round(238 * visibility)),
                HudAnimUtil.withAlpha(BACKGROUND, Math.round(246 * visibility)));
        HudRenderUtil.drawCyberneticEdge(graphics, x, y, height, THEME,
                Math.round(255 * visibility));
        graphics.fill(right - 1, y + 5, right, bottom - 5,
                HudAnimUtil.withAlpha(THEME, Math.round(70 * visibility)));
        graphics.fill(x + 4, y, right, y + 1,
                HudAnimUtil.withAlpha(THEME, Math.round(150 * visibility)));
        graphics.fill(x + 4, bottom - 1, right, bottom,
                HudAnimUtil.withAlpha(THEME, Math.round(120 * visibility)));

        int bracketColor = HudAnimUtil.withAlpha(0xFFFFFF,
                Math.round(185 * visibility));
        drawBracket(graphics, x + 4, y + 4, 1, 1, bracketColor);
        drawBracket(graphics, right - 4, y + 4, -1, 1, bracketColor);
        drawBracket(graphics, x + 4, bottom - 4, 1, -1, bracketColor);
        drawBracket(graphics, right - 4, bottom - 4, -1, -1, bracketColor);

        float time = elapsed / 1000.0F;
        HudRenderUtil.drawBreathingRhombus(graphics, x + width / 2, y,
                THEME, time, visibility);
        int scanX = x + 7 + Math.round((width - 14)
                * ((elapsed % 1_600L) / 1_600.0F));
        graphics.fill(Math.max(x + 7, scanX - 12), y + 2, scanX, y + 4,
                HudAnimUtil.withAlpha(0xFFFFFF, Math.round(95 * visibility * reveal)));

        for (int i = 0; i < 6; i++) {
            float phase = (elapsed / 700.0F + i * 0.31F) % 1.0F;
            int px = x + width / 2 + Math.round((i - 2.5F) * 17.0F);
            int py = y - 5 - Math.round(phase * 5.0F);
            int sparkAlpha = Math.round(95 * visibility * (1.0F - phase));
            graphics.fill(px, py, px + 1, py + 2,
                    HudAnimUtil.withAlpha(THEME, sparkAlpha));
        }
    }

    private static void drawBracket(GuiGraphics graphics, int x, int y,
                                    int horizontal, int vertical, int color) {
        int horizontalMinX = horizontal > 0 ? x : x - 10;
        int horizontalMaxX = horizontal > 0 ? x + 10 : x;
        int horizontalMinY = vertical > 0 ? y : y - 1;
        int horizontalMaxY = vertical > 0 ? y + 1 : y;
        graphics.fill(horizontalMinX, horizontalMinY,
                horizontalMaxX, horizontalMaxY, color);
        int verticalMinX = horizontal > 0 ? x : x - 1;
        int verticalMaxX = horizontal > 0 ? x + 1 : x;
        int verticalMinY = vertical > 0 ? y : y - 7;
        int verticalMaxY = vertical > 0 ? y + 7 : y;
        graphics.fill(verticalMinX, verticalMinY,
                verticalMaxX, verticalMaxY, color);
    }

    private static void drawScaledCenteredText(GuiGraphics graphics, Font font, Component text,
                                               int centerX, int y, int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -font.width(text) / 2, 0, color, true);
        graphics.pose().popPose();
    }

    private static void drawCenteredLine(GuiGraphics graphics, Font font,
                                         FormattedCharSequence line, int centerX, int y,
                                         int color, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, line, -font.width(line) / 2, 0, color, true);
        graphics.pose().popPose();
    }

    private static List<FormattedCharSequence> limitedLines(Font font, Component text,
                                                             int width, int maximum) {
        List<FormattedCharSequence> all = font.split(text, Math.max(1, width));
        if (all.size() <= maximum) return all;
        List<FormattedCharSequence> result = new ArrayList<>(all.subList(0, maximum - 1));
        result.add(font.split(Component.literal("..."), Math.max(1, width)).getFirst());
        return result;
    }

    private record Notice(Component title, Component description) {
    }
}
