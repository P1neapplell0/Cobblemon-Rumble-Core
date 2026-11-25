package com.p1nero.cobblemon.rumblecore.client.combat;

import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.johnn.realtimecobblemon.moves.Fighting;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import com.johnn.realtimecobblemon.moves.MoveRegistry;
import com.johnn.realtimecobblemon.moves.MoveStyle;
import com.p1nero.cobblemon.rumblecore.mixin.realtimecobblemon.combat.FightingAccess;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Shared RTC move-shape mapping for Frontier and Cobblemon's native move swap.
 *
 * Maintenance: the mapping reads RTC 1.0.3alpha.1 MoveInfo/MoveStyle/Fighting
 * data. If RTC renames a shape or changes role precedence, update this class
 * once and keep both screen integrations in sync.
 */
public final class RealtimeMoveShapeUi {
    private RealtimeMoveShapeUi() {}

    private record Shape(Component label, Component description, int color) {}

    /** A render-only label; the actual move name and search key stay unchanged. */
    public static MutableComponent displayNameWithShape(MoveTemplate move) {
        if (move == null) return Component.empty();
        Shape shape = shape(move);
        return Component.translatable("rumble.frontier.move_name_with_shape", move.getDisplayName(), coloredLabel(shape));
    }

    private static MutableComponent coloredLabel(Shape shape) {
        return shape.label().copy().withStyle(style -> style.withColor(shape.color()));
    }

    private static Shape shape(MoveTemplate move) {
        if (move == null) return null;
        MoveInfo info = MoveRegistry.info(move.getName());
        MoveStyle.Shape style = info == null ? null : MoveRegistry.style(info).shape();
        String id = info != null && FightingAccess.rumble$role(info) == Fighting.Role.GRAB
                ? "grab" : switch (style == null ? "" : style.name().toLowerCase(java.util.Locale.ROOT)) {
            case "beam" -> "beam";
            case "projectile" -> "projectile";
            case "bolt" -> "bolt";
            case "stream" -> "stream";
            case "dash" -> "dash";
            case "melee" -> "melee";
            case "shockwave" -> "shockwave";
            case "strike" -> "strike";
            default -> "other";
        };
        int color = switch (id) {
            case "grab" -> 0xffd66bff;
            case "beam", "stream" -> 0xff72d9ff;
            case "projectile", "bolt" -> 0xffffc857;
            case "dash", "melee", "strike" -> 0xffff8d72;
            case "shockwave" -> 0xffb98cff;
            default -> 0xffbfc7d5;
        };
        return new Shape(
                Component.translatable("rumble.frontier.move_shape." + id),
                Component.translatable("rumble.frontier.move_shape." + id + ".desc"), color);
    }

    /** Text shared by Frontier's existing move information card. */
    public static Component shapeLabel(MoveTemplate move) {
        Shape shape = shape(move);
        return shape == null ? Component.empty() : coloredLabel(shape);
    }

    /** Mechanism text shared by Frontier's existing move information card. */
    public static Component shapeDescription(MoveTemplate move) {
        Shape shape = shape(move);
        return shape == null ? Component.empty() : shape.description();
    }

    /** Renders the two-line tooltip after the native summary panels. */
    public static void renderShapeTooltip(GuiGraphics graphics, MoveTemplate move, int mouseX, int mouseY) {
        Shape shape = shape(move);
        if (shape == null) return;
        var font = Minecraft.getInstance().font;
        var lines = new ArrayList<>(font.split(coloredLabel(shape), 180));
        lines.addAll(font.split(shape.description(), 180));
        graphics.renderTooltip(font, lines, mouseX, mouseY);
    }

}
