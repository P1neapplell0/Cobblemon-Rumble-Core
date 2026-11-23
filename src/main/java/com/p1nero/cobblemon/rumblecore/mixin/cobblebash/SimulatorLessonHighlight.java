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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TrainingSimulatorScreen.class, remap = false)
abstract class SimulatorLessonHighlight extends AbstractContainerScreen<TrainingSimulatorMenu> {
    @Shadow @Final private static int CARD_WIDTH;
    @Shadow @Final private static int CARD_HEIGHT;
    @Shadow @Final private static int COLUMNS;
    @Shadow @Final private static int ROW_HEIGHT;
    @Shadow private double scrollOffset;
    @Shadow private double targetScrollOffset;
    @Shadow private int pendingChallenge;
    @Shadow private int maxScrollPixels() { throw new AssertionError(); }
    @Unique private boolean rumble$positioned;

    private SimulatorLessonHighlight(TrainingSimulatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private boolean rumble$isLesson() {
        return title.getContents() instanceof TranslatableContents content
                && content.getKey().equals("rumble.simulator_test.lesson_title");
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void rumble$showRock(CallbackInfo ci) {
        if (!rumble$isLesson() || rumble$positioned) return;
        rumble$positioned = true;
        scrollOffset = targetScrollOffset = Math.min(maxScrollPixels(), GymType.ROCK.ordinal() / COLUMNS * ROW_HEIGHT);
    }

    @Inject(method = "drawGymCard", at = @At("TAIL"))
    private void rumble$rock(GuiGraphics g, int index, int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
        if (rumble$isLesson() && pendingChallenge < 0 && index == GymType.ROCK.ordinal())
            TutorialPulse.render(g, leftPos + x, topPos + y, CARD_WIDTH, CARD_HEIGHT);
    }

    @Inject(method = "drawDialogButton", at = @At("TAIL"))
    private void rumble$confirm(GuiGraphics g, int x, int y, int width, Component label, boolean enabled,
                                int mouseX, int mouseY, CallbackInfo ci) {
        if (rumble$isLesson() && enabled && pendingChallenge == GymType.ROCK.ordinal()
                && label.getContents() instanceof TranslatableContents content && !content.getKey().equals("gui.cancel"))
            TutorialPulse.render(g, leftPos + x, topPos + y, width, 20);
    }
}
