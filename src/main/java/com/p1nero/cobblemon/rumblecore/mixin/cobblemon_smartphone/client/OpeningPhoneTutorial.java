package com.p1nero.cobblemon.rumblecore.mixin.cobblemon_smartphone.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nbp.cobblemon_smartphone.api.SmartphoneAction;
import com.nbp.cobblemon_smartphone.client.gui.PokeInfoDetailScreen;
import com.nbp.cobblemon_smartphone.client.gui.SmartphoneScreen;
import com.p1nero.cobblemon.rumblecore.client.quest.OpeningTutorialOverlay;
import com.p1nero.cobblemon.rumblecore.prologue.OpeningTutorial.Step;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kotlin.Pair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes actual app activations and spawn examples; it does not replace the phone controls. */
public final class OpeningPhoneTutorial {
    private OpeningPhoneTutorial() { }

    @Mixin(value = SmartphoneScreen.class, remap = false)
    abstract static class Home {
        @Shadow private int screenX, screenY;
        @Shadow @Final private static int BUTTON_WIDTH, BUTTON_HEIGHT;
        @Invoker("pagedActions") protected abstract List<SmartphoneAction> rumble$actions();
        @Invoker("getButtonPosition") protected abstract Pair<Integer, Integer> rumble$position(int slot);

        @Inject(method = "render", at = @At("TAIL"))
        private void rumble$targetApp(GuiGraphics graphics, int mx, int my, float delta, CallbackInfo ci) {
            if (!OpeningTutorialOverlay.at(Step.CLOUD) && !OpeningTutorialOverlay.at(Step.WAYSTONES)
                    && !OpeningTutorialOverlay.at(Step.POKEMON_INFO) && !OpeningTutorialOverlay.at(Step.DISTRIBUTION)) return;
            List<SmartphoneAction> actions = rumble$actions();
            for (int i = 0; i < actions.size(); i++) {
                var step = OpeningTutorialOverlay.phoneStep(actions.get(i).getId());
                if (step == Step.POKEMON_INFO && OpeningTutorialOverlay.at(Step.DISTRIBUTION)) step = Step.DISTRIBUTION;
                if (step == null || !OpeningTutorialOverlay.at(step)) continue;
                var pos = rumble$position(i);
                OpeningTutorialOverlay.point(step, (Screen) (Object) this, screenX + pos.getFirst(),
                        screenY + pos.getSecond(), BUTTON_WIDTH, BUTTON_HEIGHT);
                break;
            }
        }

        @WrapOperation(method = "mouseClicked", at = @At(value = "INVOKE",
                target = "Lcom/nbp/cobblemon_smartphone/api/SmartphoneAction;onClick()V"))
        private void rumble$appOpened(SmartphoneAction app, Operation<Void> original) {
            original.call(app);
            var step = OpeningTutorialOverlay.phoneStep(app.getId());
            if (step != null) OpeningTutorialOverlay.action(step);
        }
    }

    @Mixin(targets = "com.nbp.cobblemon_smartphone.client.gui.PokeInfoDetailScreen$Rect", remap = false)
    public interface RectAccess {
        @Invoker("getX1") int rumble$x1();
        @Invoker("getY1") int rumble$y1();
        @Invoker("getX2") int rumble$x2();
        @Invoker("getY2") int rumble$y2();
    }

    @Mixin(value = PokeInfoDetailScreen.class, remap = false)
    abstract static class Distribution {
        @Shadow private int screenY;
        @Shadow @Final private static int CONTENT_START_Y, CONTENT_END_Y;
        @Shadow @Final private Map<String, ?> bucketHeaderRects;
        @Shadow @Final private Set<String> expandedBuckets;

        @Inject(method = "render", at = @At("TAIL"))
        private void rumble$targetDistribution(GuiGraphics graphics, int mx, int my, float delta, CallbackInfo ci) {
            if (!OpeningTutorialOverlay.at(Step.DISTRIBUTION)) return;
            var screen = (Screen) (Object) this;
            for (var box : bucketHeaderRects.values()) {
                var rect = (RectAccess) box;
                if (rect.rumble$y1() < screenY + CONTENT_START_Y || rect.rumble$y2() >= screenY + CONTENT_END_Y) continue;
                OpeningTutorialOverlay.point(Step.DISTRIBUTION, screen, rect.rumble$x1(), rect.rumble$y1(),
                        rect.rumble$x2() - rect.rumble$x1(), rect.rumble$y2() - rect.rumble$y1());
                break;
            }
        }

        @Inject(method = "mouseClicked", at = @At("RETURN"))
        private void rumble$distributionOpened(double mx, double my, int button, CallbackInfoReturnable<Boolean> cir) {
            if (button != 0 || !cir.getReturnValueZ() || !OpeningTutorialOverlay.at(Step.DISTRIBUTION)
                    || my < screenY + CONTENT_START_Y || my >= screenY + CONTENT_END_Y) return;
            for (var entry : bucketHeaderRects.entrySet()) {
                var rect = (RectAccess) entry.getValue();
                if (expandedBuckets.contains(entry.getKey()) && mx >= rect.rumble$x1() && mx <= rect.rumble$x2()
                        && my >= rect.rumble$y1() && my <= rect.rumble$y2()) {
                    OpeningTutorialOverlay.action(Step.DISTRIBUTION);
                    return;
                }
            }
        }
    }
}
