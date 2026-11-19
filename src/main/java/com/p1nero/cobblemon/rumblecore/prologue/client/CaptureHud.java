package com.p1nero.cobblemon.rumblecore.prologue.client;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.item.PokeBallItem;
import com.johnn.realtimecobblemon.client.ClientSmash;
import com.p1nero.cobblemon.rumblecore.combat.reward.WildCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Draw the closest visible capture window in HUD space, beside rather than behind its model. */
final class CaptureHud {
    private static final double DISTANCE = 18;
    private static InteractionHud.Anchor anchor;
    private static int seconds;

    private CaptureHud() {}

    static void project(RenderLevelStageEvent event) {
        anchor = null;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.options.hideGui) return;
        double best = DISTANCE * DISTANCE;
        var camera = event.getCamera().getPosition();
        for (var target : mc.level.getEntitiesOfClass(PokemonEntity.class, mc.player.getBoundingBox().inflate(DISTANCE),
                mon -> mon.isAlive() && !mon.isInvisible() && WildCapture.seconds(mon) > 0)) {
            double distance = target.distanceToSqr(mc.player);
            if (distance >= DISTANCE * DISTANCE) continue;
            var candidate = InteractionHud.locate(event, target, Math.max(.45, target.getBbWidth() * .52));
            if (candidate == null) continue;
            // Prefer the Pokémon near the crosshair when multiple capture windows are visible.
            double score = (Math.abs(candidate.x() - .5) + Math.abs(candidate.y() - .5)) * 70
                    + target.distanceToSqr(camera) * .08;
            if (score >= best) continue;
            best = score;
            anchor = candidate;
            seconds = WildCapture.seconds(target);
        }
    }

    static void render(GuiGraphics graphics) {
        var projection = anchor;
        anchor = null;
        var mc = Minecraft.getInstance();
        if (projection == null || mc.player == null || mc.screen != null || mc.options.hideGui) return;
        boolean active = ClientSmash.active();
        boolean holdingBall = mc.player.getMainHandItem().getItem() instanceof PokeBallItem
                || mc.player.getOffhandItem().getItem() instanceof PokeBallItem;
        var binding = active ? ClientSmash.SMASH : mc.options.keyUse;
        Component key = (active || holdingBall) && !binding.isUnbound() ? binding.getTranslatedKeyMessage() : null;
        Component action = Component.translatable(active ? "rumble.capture.leave_combat"
                : holdingBall ? "rumble.capture.throw" : "rumble.capture.equip");
        Component timer = Component.translatable("rumble.capture.countdown", seconds);
        var font = mc.font;
        int keyWidth = key == null ? 0 : Math.max(16, font.width(key) + 8);
        int textOffset = keyWidth == 0 ? 0 : keyWidth + 5;
        int width = Math.max(font.width(timer), textOffset + font.width(action));
        int screen = graphics.guiWidth();
        int x = Math.round(projection.x() * screen);
        int y = Mth.clamp(Math.round(projection.y() * graphics.guiHeight()), 20, graphics.guiHeight() - 20);
        int gap = 9 + Mth.clamp(Math.round(projection.offset() * screen), 10, Math.max(10, screen / 4));
        boolean right = x + gap + width + 6 <= screen || x - gap - width < 6 && x < screen / 2;
        int start = Mth.clamp(right ? x + gap : x - gap - width, 6, Math.max(6, screen - width - 6));
        int accent = seconds <= 5 ? 0xffffa58b : 0xfff8dc81;
        int dash = right ? start - 7 : start + width + 3;
        graphics.fill(dash, y - 3, dash + 4, y - 2, accent);
        graphics.drawString(font, timer, start, y - 13, accent);
        if (key != null) {
            graphics.fill(start, y - 4, start + keyWidth, y + 11, 0xd91b2028);
            graphics.renderOutline(start, y - 4, keyWidth, 15, 0xffdce9ef);
            graphics.drawCenteredString(font, key, start + keyWidth / 2, y, 0xfff5fbff);
        }
        graphics.drawString(font, action, start + textOffset, y, 0xfff5fbff);
    }
}
