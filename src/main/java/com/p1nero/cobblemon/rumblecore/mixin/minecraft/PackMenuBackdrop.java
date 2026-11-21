package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.client.menu.TitleBackdrop;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class PackMenuBackdrop {
    @Unique private static final ResourceLocation RUMBLE_INWORLD = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_background.png");

    @Inject(method = "renderMenuBackgroundTexture", at = @At("HEAD"), cancellable = true)
    private static void rumble$replaceTiledBackground(GuiGraphics graphics, ResourceLocation texture,
            int x, int y, float u, float v, int width, int height, CallbackInfo ci) {
        if (!Screen.MENU_BACKGROUND.equals(texture) && !RUMBLE_INWORLD.equals(texture)) return;
        if (x == 0 && y == 0 && width == graphics.guiWidth() && height == graphics.guiHeight()) {
            TitleBackdrop.render(graphics);
        } else {
            // Tabs and NeoForge scroll panels use the same tile as the full-screen background.
            TitleBackdrop.shade(graphics, x, y, width, height);
        }
        ci.cancel();
    }
}
