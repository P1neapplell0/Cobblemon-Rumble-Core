package com.p1nero.cobblemon.rumblecore.client.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import com.p1nero.cobblemon.rumblecore.client.combat.ServerHallScreen;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Inline credits in the pause menu, laid out locally without resizing the vanilla menu. */
public final class PackMenus {
    private static final String SPAKE = "https://spake404api.me/register?aff=4VK85L9NJ3FQ";
    private static final String QQ_CHANNEL = "https://pd.qq.com/s/Ngfkasp6hd";
    private static final String[] CN = {"https://www.xyebbs.com/members/386?tab=resources", "https://bbsmc.net/user/P1nero"};
    private static final String[] GLOBAL = {"https://www.curseforge.com/members/p1nero/projects?page=1&classIds=4471",
            "https://www.curseforge.com/members/p1nero/projects"};
    private static final int GOLD = 0xFFF0C479, PAPER = 0xFFEEE3D1, LINK = 0xFF8ED5FF;
    private static final int CENTER_GAP = 106, MARGIN = 7, PITCH = 11;

    private PackMenus() { }

    static String discordUrl() {
        return System.getProperty("rumble.discordUrl", "https://discord.gg/UfVF8b2GFk");
    }

    public record Link(float x, float y, float width, float height, String url) {
        public boolean contains(double mx, double my) {
            return mx >= x && mx < x + width && my >= y && my < y + height;
        }

        public void open() {
            Util.getPlatform().openUri(url);
        }
    }

    private record Line(FormattedCharSequence text, int color, int x, String url) { }

    public static final class PauseLayout {
        private final List<Line> left = new ArrayList<>(), right = new ArrayList<>();
        private int gutter, top;
        private float scale;
    }

    public static PauseLayout pauseLayout(int width, int height, Font font) {
        var layout = new PauseLayout();
        int gutter = Math.min(220, width / 2 - CENTER_GAP - MARGIN);
        if (gutter <= 0) return layout;
        int wrap = Math.max(85, gutter);
        boolean chinese = Minecraft.getInstance().getLanguageManager().getSelected().equals("zh_cn");
        var left = layout.left;
        var right = layout.right;
        add(left, font, text("credits"), GOLD, 0, wrap, null);
        space(left);
        for (String role : List.of("role_design", "role_code", "role_teams", "role_builds")) {
            add(left, font, text(role), PAPER, 0, wrap, null);
        }
        add(left, font, Component.literal("WorldBlocks Team"), PAPER, 6, wrap, null);
        add(left, font, Component.literal("GENESISVLLGAMER"), PAPER, 6, wrap, null);
        add(left, font, text("thanks_spake"), LINK, 0, wrap, SPAKE);
        add(left, font, text("thanks_pokebrick", Component.literal("马了还想马")), PAPER, 0, wrap, null);
        space(left);
        for (String thanks : List.of("thanks1", "thanks2", "thanks3")) {
            add(left, font, text(thanks), PAPER, 0, wrap, null);
        }
        add(right, font, text("more"), GOLD, 0, wrap, null);
        space(right);
        add(right, font, Component.translatable("rumble.title.discord"), LINK, 0, wrap, discordUrl());
        add(right, font, text("qq_channel"), LINK, 0, wrap, QQ_CHANNEL);
        space(right);
        add(right, font, text("play"), PAPER, 0, wrap, null);
        space(right);
        add(right, font, text("recommend", Component.literal(chinese ? "炽白真形" : "Pokéverie")), PAPER, 0, wrap, null);
        space(right);
        add(right, font, text("get_packs"), PAPER, 0, wrap, null);
        String[] routes = chinese ? CN : GLOBAL;
        add(right, font, text("route1"), LINK, 0, wrap, routes[0]);
        add(right, font, text("route2"), LINK, 0, wrap, routes[1]);

        layout.gutter = gutter;
        layout.top = Math.min(42, Math.max(MARGIN, height / 6));
        layout.scale = Math.min((float) gutter / wrap,
                (float) Math.max(1, height - layout.top - MARGIN) / (PITCH * Math.max(left.size(), right.size())));
        return layout;
    }

    public static Button serverHallButton(Screen parent, Button disconnect) {
        return OreTheme.action(disconnect.getX(), disconnect.getY() + disconnect.getHeight() + 4,
                disconnect.getWidth(), 20, text("server_hall"), true,
                b -> Minecraft.getInstance().setScreen(new ServerHallScreen(parent)));
    }

    public static List<Link> renderPause(GuiGraphics g, int width, Font font, PauseLayout layout, int mx, int my) {
        if (layout.gutter <= 0) return List.of();
        var links = new ArrayList<Link>(5);
        draw(g, font, layout.left, width / 2 - CENTER_GAP - layout.gutter, layout.top, layout.scale, mx, my, links);
        draw(g, font, layout.right, width / 2 + CENTER_GAP, layout.top, layout.scale, mx, my, links);
        if (links.stream().anyMatch(link -> SPAKE.equals(link.url()) && link.contains(mx, my))) {
            g.renderTooltip(font, text("thanks_spake_hover"), mx, my);
        }
        return links;
    }

    private static void space(List<Line> lines) {
        lines.add(new Line(FormattedCharSequence.EMPTY, PAPER, 0, null));
    }

    private static void add(List<Line> rows, Font font, Component value, int color, int indent, int width, String url) {
        Component content = url == null ? value : value.copy().withStyle(ChatFormatting.UNDERLINE);
        for (var line : font.split(content, width - indent)) rows.add(new Line(line, color, indent, url));
    }

    private static void draw(GuiGraphics g, Font font, List<Line> rows, float x, float y, float scale,
                             int mx, int my, List<Link> links) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1);
        for (int i = 0; i < rows.size(); i++) {
            var line = rows.get(i);
            int rowY = i * PITCH;
            int color = line.color;
            if (line.url != null) {
                var link = new Link(x + line.x * scale, y + rowY * scale, font.width(line.text) * scale, PITCH * scale, line.url);
                links.add(link);
                if (link.contains(mx, my)) color = 0xFFFFFFFF;
            }
            g.drawString(font, line.text, line.x, rowY, color, true);
        }
        g.pose().popPose();
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("rumble.menu." + key, args);
    }
}
