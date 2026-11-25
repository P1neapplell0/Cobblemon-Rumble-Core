package com.p1nero.cobblemon.rumblecore.client.combat;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.client.gui.summary.Summary;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Draws the native move-shape tooltip after Cobblemon has finished painting
 * its summary panels. This intentionally lives outside MoveSlot's list scissor.
 * NeoForge posts Render.Post after Screen.renderWithTooltip, outside the list scissor.
 */
@EventBusSubscriber(modid = "cobblemon_rumble_core", value = Dist.CLIENT)
public final class RealtimeMoveShapeTooltip {
    private static Screen screen;
    private static MoveTemplate move;
    private static int mouseX;
    private static int mouseY;

    private RealtimeMoveShapeTooltip() {}

    public static void capture(Screen owner, MoveTemplate hovered, int x, int y) {
        if (!(owner instanceof Summary) || hovered == null) return;
        screen = owner;
        move = hovered;
        mouseX = x;
        mouseY = y;
    }

    @SubscribeEvent
    public static void beforeRender(ScreenEvent.Render.Pre event) {
        // A fresh frame must not reuse the previous frame's hovered move.
        screen = null;
        move = null;
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (screen != event.getScreen() || move == null) return;
        GuiGraphics graphics = event.getGuiGraphics();
        // Cobblemon Summary renders its main panel at z=1000.  Render above it
        // so the tooltip cannot be clipped by the right-hand move list or panel.
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(0, 0, 1100);
            RealtimeMoveShapeUi.renderShapeTooltip(graphics, move, mouseX, mouseY);
        } finally {
            graphics.pose().popPose();
        }
        screen = null;
        move = null;
    }
}
