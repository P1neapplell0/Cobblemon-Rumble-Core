package com.p1nero.cobblemon.rumblecore.mixin.minecraft;

import com.p1nero.cobblemon.rumblecore.client.menu.PackMenus;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
abstract class PackPauseMenu extends Screen {
    @Shadow private Button disconnectButton;
    @Shadow public abstract boolean showsPauseMenu();
    @Unique private List<PackMenus.Link> rumble$links = List.of();
    @Unique private PackMenus.PauseLayout rumble$layout;

    protected PackPauseMenu(Component title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void rumble$resetPauseLinks(CallbackInfo ci) {
        rumble$links = List.of();
        if (showsPauseMenu()) {
            rumble$layout = PackMenus.pauseLayout(width, height, font);
            if (disconnectButton != null) {
                int shift = Math.max(0, disconnectButton.getY() + disconnectButton.getHeight() + 28 - height);
                int top = children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                        .mapToInt(AbstractWidget::getY).min().orElse(0);
                int offset = Math.min(shift, Math.max(0, top - 24));
                if (offset > 0) for (var child : children()) {
                    if (child instanceof AbstractWidget widget) widget.setY(widget.getY() - offset);
                }
                addRenderableWidget(PackMenus.serverHallButton(this, disconnectButton));
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void rumble$pauseText(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (showsPauseMenu() && rumble$layout != null) {
            rumble$links = PackMenus.renderPause(graphics, width, font, rumble$layout, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && showsPauseMenu()) {
            for (var link : rumble$links) {
                if (link.contains(mouseX, mouseY)) {
                    link.open();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
