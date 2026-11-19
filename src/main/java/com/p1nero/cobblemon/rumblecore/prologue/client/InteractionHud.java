package com.p1nero.cobblemon.rumblecore.prologue.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** A world-anchored prompt drawn in the GUI pass, so the model cannot depth-occlude it. */
final class InteractionHud {
    private static final Component ACTION = Component.translatable("rumble.prologue.talk_prompt");
    private static float x, y, sideOffset;
    private static boolean visible;

    private InteractionHud() {}

    static void clear() { visible = false; }

    static void project(RenderLevelStageEvent event, LivingEntity doctor) {
        Anchor anchor = locate(event, doctor, .5);
        if (anchor == null) return;
        x = anchor.x();
        y = anchor.y();
        sideOffset = anchor.offset();
        visible = true;
    }

    record Anchor(float x, float y, float offset) {}

    static Anchor locate(RenderLevelStageEvent event, LivingEntity doctor, double radius) {
        var camera = event.getCamera();
        Vec3 torso = doctor.getPosition(camera.getPartialTickTime()).add(0, doctor.getBbHeight() * .54, 0);
        Vec3 origin = camera.getPosition();
        // Player LOS is checked for interaction; also check the actual camera for third-person views.
        if (doctor.level().clip(new ClipContext(origin, torso, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, camera.getEntity())).getType() != HitResult.Type.MISS) return null;
        var matrix = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
        Vec3 point = torso.subtract(origin);
        var clip = project(matrix, point);
        if (!(clip.w > 0) || Math.abs(clip.x) >= clip.w || Math.abs(clip.y) >= clip.w
                || Math.abs(clip.z) >= clip.w) return null;
        var left = camera.getLeftVector();
        var edge = project(matrix, point.add(left.x() * radius, 0, left.z() * radius));
        float x = (clip.x / clip.w + 1) * .5f;
        float y = (1 - clip.y / clip.w) * .5f;
        float offset = edge.w > 0 ? Math.abs(edge.x / edge.w - clip.x / clip.w) * .5f : 0;
        return new Anchor(x, y, offset);
    }

    private static Vector4f project(Matrix4f matrix, Vec3 point) {
        return matrix.transform(new Vector4f((float) point.x, (float) point.y, (float) point.z, 1));
    }

    static boolean render(GuiGraphics g, Component binding, float opacity) {
        if (!visible || opacity < .04f) return false;
        var font = Minecraft.getInstance().font;
        int w = g.guiWidth();
        int anchorX = Math.round(x * w);
        int centerY = Mth.clamp(Math.round(y * g.guiHeight()), 14, g.guiHeight() - 14);
        int keyWidth = Math.max(18, font.width(binding) + 10);
        int width = keyWidth + 6 + font.width(ACTION);
        int gap = Mth.clamp(Math.round(sideOffset * w), 13, Math.max(13, w / 4)) + 9 + Math.round((1 - opacity) * 5);
        int right = anchorX + gap;
        int left = anchorX - gap - width;
        boolean onRight = right + width <= w - 6 || left < 6 && w - anchorX >= anchorX;
        int start = Mth.clamp(onRight ? right : left, 6, Math.max(6, w - width - 6));
        int keyX = onRight ? start : start + width - keyWidth;
        int actionX = onRight ? keyX + keyWidth + 6 : start;
        int dashX = onRight ? start - 7 : start + width + 3;
        g.fill(dashX, centerY, dashX + 4, centerY + 1, alpha(0xccd8db, opacity));
        g.fill(keyX, centerY - 8, keyX + keyWidth, centerY + 9, alpha(0x111a20, opacity * .87f));
        g.renderOutline(keyX, centerY - 8, keyWidth, 17, alpha(0xe3f4f0, opacity));
        g.drawCenteredString(font, binding, keyX + keyWidth / 2, centerY - 4, alpha(0xffffff, opacity));
        g.drawString(font, ACTION, actionX, centerY - 4, alpha(0xffffff, opacity));
        return true;
    }

    private static int alpha(int rgb, float opacity) { return Math.round(opacity * 255) << 24 | rgb; }
}
