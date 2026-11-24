package com.p1nero.cobblemon.rumblecore.client.combat;

import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import com.p1nero.misc.custom_instances.api.battle.BattleReport;
import com.p1nero.misc.custom_instances.client.screen.battle.DpsChart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class TeamDpsScreen extends Screen {
    private final Screen parent;
    private final CompoundTag roster;
    private final BattleReport report;
    private final ResourceLocation arena;
    private int left, top, panelWidth, panelHeight, ticks = 100;
    private TeamDpsChart.Legend legend;
    private boolean hasSamples;

    private TeamDpsScreen(Screen parent, CompoundTag roster, BattleReport report, ResourceLocation arena) {
        super(Component.translatable("rumble.gym.team_dps"));
        this.parent = parent;
        this.roster = roster;
        this.report = report;
        this.arena = arena;
    }

    public static void preview(ResourceLocation dimension, CompoundTag data) {
        var mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.dimension().location().equals(dimension))
            mc.setScreen(new TeamDpsScreen(null, null, BattleReport.fromTag(data), dimension));
    }

    public static Screen ranking(Screen parent, CompoundTag roster, CompoundTag report) {
        return new TeamDpsScreen(parent, roster, report.isEmpty() ? null : BattleReport.fromTag(report), null);
    }

    @Override protected void init() {
        panelWidth = Math.min(426, width - 12);
        panelHeight = Math.min(210, height - 12);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        hasSamples = report != null && report.players().stream().anyMatch(p -> !p.damageSamples().isEmpty());
        legend = hasSamples && panelWidth > 36 ? TeamDpsChart.layoutLegend(font, report, panelWidth - 24) : null;
        if (arena == null) {
            addRenderableWidget(OreTheme.action(left + 10, top + panelHeight - 25, 84, 18,
                    Component.translatable("gui.back"), false, b -> onClose()));
            addRenderableWidget(OreTheme.action(left + panelWidth - 114, top + panelHeight - 25, 104, 18,
                    Component.translatable("rumble.champion.roster"), true,
                    b -> minecraft.setScreen(HallOfFameScreen.roster(this, roster))));
        }
    }

    @Override public void tick() {
        if (arena != null && (--ticks <= 0 || minecraft.level == null || !minecraft.level.dimension().location().equals(arena))) onClose();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return arena == null; }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x9C141418);
        OreTheme.panel(g, left, top, panelWidth, panelHeight, 22);
        g.drawCenteredString(font, title, width / 2, top + 8, OreTheme.TEXT);
        int chartX = left + 12, chartY = top + 30, chartW = panelWidth - 24;
        int chartH = panelHeight - 72 - (legend == null ? 0 : legend.height());
        boolean showChart = legend != null && chartH > 16;
        if (showChart) {
            TeamDpsChart.render(g, font, report, chartX, chartY, chartW, chartH, mx, my);
            legend.render(g, font, chartX, chartY + chartH + 16);
        } else if (!hasSamples) {
            g.drawCenteredString(font, Component.translatable("rumble.gym.no_dps_samples"), width / 2, height / 2, OreTheme.MUTED);
        }
        if (arena != null) g.drawCenteredString(font, Component.translatable("rumble.gym.report_countdown", (ticks + 19) / 20),
                width / 2, top + panelHeight - 17, OreTheme.MUTED);
        super.render(g, mx, my, partial);
        if (showChart) DpsChart.renderTeamTooltip(g, font, report, chartX, chartY, chartW, chartH, mx, my);
    }
}
