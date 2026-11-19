package com.p1nero.cobblemon.rumblecore.mixin.arc_quest;

import org.arcadia.arc_quest.client.hud.quest.journal.detail.JournalDetailControls;
import org.arcadia.arc_quest.quest.api.QuestDefinition;
import org.arcadia.arc_quest.quest.data.QuestRuntimeData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = JournalDetailControls.class, remap = false)
abstract class JournalAbandonButton {
    // The journal shares this predicate between layout, rendering and hit-testing.
    @Inject(method = "shouldShowAbandon", at = @At("HEAD"), cancellable = true)
    private void rumble$hideAbandon(QuestDefinition definition, QuestRuntimeData runtime,
                                    CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }
}
