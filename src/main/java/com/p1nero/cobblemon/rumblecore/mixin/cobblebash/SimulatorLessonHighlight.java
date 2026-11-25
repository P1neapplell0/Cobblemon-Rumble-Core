package com.p1nero.cobblemon.rumblecore.mixin.cobblebash;

import com.nore.cobblebash.gym.GymType;
import com.p1nero.cobblebashscreen.simulator.TrainingSimulatorMenu;
import com.p1nero.cobblebashscreen.simulator.TrainingSimulatorScreen;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TutorialPulse;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TrainingSimulatorScreen.class, remap = false)
abstract class SimulatorLessonHighlight extends AbstractContainerScreen<TrainingSimulatorMenu> {
    @Shadow @Final private static int COLUMNS;
    @Shadow @Final private static int ROW_HEIGHT;
    @Shadow @Final private static int MAX_SCROLL;
    @Shadow private double scrollOffset;
    @Shadow private double targetScrollOffset;
    @Shadow private int pendingChallenge;
    @Shadow private boolean canConfirm() { throw new AssertionError(); }
    @Unique private boolean rumble$positioned;

    private SimulatorLessonHighlight(TrainingSimulatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Unique private boolean rumble$isLesson() {
        return title.getContents() instanceof TranslatableContents content
                && content.getKey().equals("rumble.simulator_test.lesson_title");
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void rumble$showRock(CallbackInfo ci) {
        if (!rumble$isLesson() || rumble$positioned) return;
        rumble$positioned = true;
        scrollOffset = targetScrollOffset = Math.min(MAX_SCROLL, GymType.ROCK.ordinal() / COLUMNS * ROW_HEIGHT);
    }

    @Inject(method = "drawGymCard", at = @At("TAIL"))
    private void rumble$rock(GuiGraphics g, int index, @Coerce Object card, boolean hovered, CallbackInfo ci) {
        if (rumble$isLesson() && pendingChallenge < 0 && index == GymType.ROCK.ordinal())
            rumble$pulse(g, card);
    }

    @Inject(method = "drawButton", at = @At("TAIL"))
    private void rumble$confirm(GuiGraphics g, @Coerce Object button, Component label, @Coerce Object style,
                                int mouseX, int mouseY, CallbackInfo ci) {
        if (pendingChallenge == GymType.ROCK.ordinal() && rumble$isLesson()
                && label.getContents() instanceof TranslatableContents content
                && content.getKey().equals("screen.cobblebash_screen.simulator.enter") && canConfirm())
            rumble$pulse(g, button);
    }

    @Unique private void rumble$pulse(GuiGraphics g, Object rect) {
        var bounds = (SimulatorRectAccess) rect;
        TutorialPulse.render(g, leftPos + bounds.rumble$x(), topPos + bounds.rumble$y(),
                bounds.rumble$width(), bounds.rumble$height());
    }
}
