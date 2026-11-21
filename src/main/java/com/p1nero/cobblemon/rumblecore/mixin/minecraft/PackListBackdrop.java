package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.client.menu.TitleBackdrop;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSelectionList.class)
abstract class PackListBackdrop extends AbstractWidget {
    protected PackListBackdrop(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderListBackground", at = @At("HEAD"), cancellable = true)
    private void rumble$shadeList(GuiGraphics graphics, CallbackInfo ci) {
        TitleBackdrop.shade(graphics, getX(), getY(), getWidth(), getHeight());
        ci.cancel();
    }
}
