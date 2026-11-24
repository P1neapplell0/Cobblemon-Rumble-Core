package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.p1nero.misc.custom_instances.client.scroll.SmoothScroll;
import com.p1nero.misc.custom_instances.client.widget.PartyRosterWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PartyRosterWidget.class, remap = false)
abstract class GymRosterLabels extends AbstractWidget {
    @Shadow @Final private ListTag members;
    @Shadow @Final private SmoothScroll scroll;

    protected GymRosterLabels(int x, int y, int width, int height, Component title) { super(x, y, width, height, title); }

    @Inject(method = "renderWidget", at = @At("TAIL"), remap = true)
    private void rumble$quota(GuiGraphics g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        var font = Minecraft.getInstance().font;
        g.enableScissor(getX(), getY() + 62, getX() + getWidth(), getY() + 70);
        for (int i = 0; i < members.size(); i++) {
            int limit = members.getCompound(i).getInt("rumbleRosterLimit");
            if (limit > 0) g.drawCenteredString(font, Component.translatable("rumble.gym.deploy_quota", limit),
                    getX() + i * 60 + 26 - (int) Math.round(scroll.position()), getY() + 62, 0xFFEED698);
        }
        g.disableScissor();
    }
}
