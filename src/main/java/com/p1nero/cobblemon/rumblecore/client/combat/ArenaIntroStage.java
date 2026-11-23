package com.p1nero.cobblemon.rumblecore.client.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** One procedural background quad; no off-screen framebuffer or post-processing chain. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class ArenaIntroStage {
    private static ShaderInstance shader;

    private ArenaIntroStage() {}

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) {
        shader = null;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), PrologueWorld.id("arena_intro"),
                    DefaultVertexFormat.POSITION_TEX), loaded -> shader = loaded);
        } catch (IOException | RuntimeException e) {
            LogUtils.getLogger().warn("Arena intro shader unavailable; using the gradient stage", e);
        }
    }

    static void draw(GuiGraphics graphics, int width, int height, float time, float impact) {
        if (shader == null || shader.getUniform("Scene") == null) {
            graphics.fillGradient(0, 0, width, height, 0xff224b68, 0xff090d1d);
            graphics.fillGradient(width / 2, 0, width, height, 0xff683653, 0xff170d21);
            return;
        }
        graphics.flush();
        shader.getUniform("Scene").set(time, width / (float) height, impact, 0);
        var matrix = graphics.pose().last().pose();
        var quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        quad.addVertex(matrix, 0, height, 0).setUv(0, 1);
        quad.addVertex(matrix, width, height, 0).setUv(1, 1);
        quad.addVertex(matrix, width, 0, 0).setUv(1, 0);
        quad.addVertex(matrix, 0, 0, 0).setUv(0, 0);
        var previousShader = RenderSystem.getShader();
        try {
            RenderSystem.setShader(() -> shader);
            BufferUploader.drawWithShader(quad.buildOrThrow());
        } finally {
            RenderSystem.setShader(() -> previousShader);
        }
    }
}
