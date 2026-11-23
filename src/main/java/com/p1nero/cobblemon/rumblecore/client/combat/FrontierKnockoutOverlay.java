package com.p1nero.cobblemon.rumblecore.client.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.FrontierKnockout;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class FrontierKnockoutOverlay {
    private static final ResourceLocation ICON = PrologueWorld.id("textures/gui/arena_ko.png");
    private static FrontierKnockout.Cue active;
    private static long start;

    private FrontierKnockoutOverlay() {}

    public static void start(FrontierKnockout.Cue cue) {
        ArenaIntroOverlay.finishForKnockout(cue.dimension());
        active = cue;
        start = Util.getMillis();
        ArenaCinematicAudio.play("arena.knockout", .94f);
    }

    public static FrontierKnockout.Cue slowCue(Entity entity) {
        var cue = active;
        var level = Minecraft.getInstance().level;
        return entity != null && level != null && entity.level() == level && cue != null
                && level.dimension().location().equals(cue.dimension())
                && Util.getMillis() - start < FrontierKnockout.SLOW_TICKS * 50L
                && cue.actors().contains(entity.getId()) ? cue : null;
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { active = null; }

    @SubscribeEvent
    public static void draw(RenderGuiEvent.Post event) {
        if (active == null) return;
        var level = Minecraft.getInstance().level;
        float age = (Util.getMillis() - start) / (50f * Math.max(1, active.ticks()));
        if (age >= 1 || level == null || !level.dimension().location().equals(active.dimension())) {
            active = null;
            return;
        }
        var g = event.getGuiGraphics();
        int w = (int) (Math.min(244, g.guiWidth() * 0.47f) * (1 + .16f * Math.max(0, 1 - age * 9)));
        int h = w * 261 / 640;
        int alpha = Math.round(255 * Mth.clamp((1 - age) * 5, 0, 1));
        g.flush();
        RenderSystem.enableBlend();
        g.setColor(1, 1, 1, alpha / 255f);
        g.blit(ICON, (g.guiWidth() - w) / 2, g.guiHeight() * 2 / 5 - h / 2, w, h, 0, 0, 640, 261, 640, 261);
        g.flush();
        g.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }
}
