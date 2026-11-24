package com.p1nero.cobblemon.rumblecore.client.combat;

import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import com.p1nero.misc.custom_instances.api.battle.BattleReport;
import com.p1nero.misc.custom_instances.api.battle.PlayerBattleStats;
import com.p1nero.misc.custom_instances.client.screen.battle.DpsChart;
import com.p1nero.misc.custom_instances.client.screen.information.InstanceInformationScreen;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/** Renders the team's samples and legend in separate areas. */
final class TeamDpsChart {
    private static final int[] COLORS = {0xFF86D562, 0xFF63B8E8, 0xFFF2B84B, 0xFFE477A7,
            0xFFB58CF2, 0xFF6ED3C1, 0xFFE8835B, 0xFFB9C95A};
    private static final int ROW_HEIGHT = 12;

    private TeamDpsChart() {}

    static Legend layoutLegend(Font font, BattleReport report, int width) {
        List<Label> labels = new ArrayList<>();
        int x = 0, y = 0;
        for (int index = 0; index < report.players().size(); index++) {
            String name = report.players().get(index).name();
            int nameWidth = width - 9;
            if (font.width(name) > nameWidth) {
                name = font.plainSubstrByWidth(name, Math.max(0, nameWidth - font.width("…"))) + "…";
            }
            int labelWidth = 9 + font.width(name);
            if (x > 0 && x + labelWidth > width) {
                x = 0;
                y += ROW_HEIGHT;
            }
            labels.add(new Label(name, x, y, color(index)));
            x += labelWidth + 10;
        }
        return new Legend(List.copyOf(labels), labels.isEmpty() ? 0 : y + ROW_HEIGHT);
    }

    private record Label(String name, int x, int y, int color) {}

    record Legend(List<Label> labels, int height) {
        void render(GuiGraphics g, Font font, int left, int top) {
            for (Label label : labels) {
                int x = left + label.x(), y = top + label.y();
                g.fill(x, y + 1, x + 6, y + 7, label.color());
                g.drawString(font, label.name(), x + 9, y, OreTheme.TEXT);
            }
        }
    }

    static void render(GuiGraphics g, Font font, BattleReport report, int left, int top, int width, int height,
                       int mouseX, int mouseY) {
        g.fill(left, top, left + width, top + height, 0xFF18191A);
        for (int index = 0; index <= 2; index++) {
            int y = top + index * (height - 1) / 2;
            g.fill(left, y, left + width, y + 1, 0xFF48494A);
        }
        double peak = 1;
        for (PlayerBattleStats player : report.players()) {
            for (int index = 0; index < player.damageSamples().size(); index++) {
                peak = Math.max(peak, DpsChart.sampleDps(report, player, index));
            }
        }
        long elapsedTicks = Math.max(1, report.elapsedTicks());
        for (int playerIndex = 0; playerIndex < report.players().size(); playerIndex++) {
            PlayerBattleStats player = report.players().get(playerIndex);
            int previousX = left, previousY = top + height - 1;
            for (int index = 0; index < player.damageSamples().size(); index++) {
                long sampleEnd = Math.min(elapsedTicks, (index + 1L) * report.sampleTicks());
                int x = left + (int) (sampleEnd * (width - 1) / elapsedTicks);
                int y = top + height - 1 - (int) Math.round(DpsChart.sampleDps(report, player, index) / peak * (height - 2));
                line(g, previousX, index == 0 ? y : previousY, x, y, color(playerIndex));
                previousX = x;
                previousY = y;
            }
        }
        g.drawString(font, InstanceInformationScreen.number(peak), left + 3, top + 3, OreTheme.TEXT);
        int axisY = top + height + 3;
        g.drawString(font, "0", left, axisY, OreTheme.MUTED);
        String duration = InstanceInformationScreen.duration(report.elapsedTicks());
        g.drawString(font, duration, left + width - font.width(duration), axisY, OreTheme.MUTED);
        if (mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height) {
            g.fill(mouseX, top, mouseX + 1, top + height, 0x77FFFFFF);
        }
    }

    private static int color(int index) {
        return COLORS[index % COLORS.length];
    }

    private static void line(GuiGraphics g, int startX, int startY, int endX, int endY, int color) {
        int steps = Math.max(1, Math.max(Math.abs(endX - startX), Math.abs(endY - startY)));
        for (int step = 0; step <= steps; step++) {
            int x = startX + Math.round((endX - startX) * step / (float) steps);
            int y = startY + Math.round((endY - startY) * step / (float) steps);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }
}
