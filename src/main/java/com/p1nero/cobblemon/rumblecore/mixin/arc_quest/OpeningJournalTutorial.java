package com.p1nero.cobblemon.rumblecore.mixin.arc_quest;

import com.p1nero.cobblemon.rumblecore.client.quest.OpeningTutorialOverlay;
import com.p1nero.cobblemon.rumblecore.prologue.OpeningTutorial.Step;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import org.arcadia.arc_quest.client.hud.quest.journal.JournalConstants;
import org.arcadia.arc_quest.client.hud.quest.journal.JournalListPanel;
import org.arcadia.arc_quest.client.hud.quest.journal.JournalTypes;
import org.arcadia.arc_quest.client.hud.quest.journal.QuestJournalScreen;
import org.arcadia.arc_quest.client.hud.quest.journal.detail.JournalDetailControls;
import org.arcadia.arc_quest.quest.api.QuestDefinition;
import org.arcadia.arc_quest.quest.data.QuestRuntimeData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native journal bounds, sampled after the journal lays out and scrolls its own rows. */
public final class OpeningJournalTutorial {
    private OpeningJournalTutorial() { }

    @Mixin(targets = "org.arcadia.arc_quest.client.hud.quest.journal.JournalListLayout$GroupRow", remap = false)
    public interface GroupRowAccess {
        @Invoker("quests") List<JournalTypes.QuestListEntry> rumble$quests();
    }

    @Mixin(targets = "org.arcadia.arc_quest.client.hud.quest.journal.JournalListLayout$QuestRow", remap = false)
    public interface QuestRowAccess {
        @Invoker("entry") JournalTypes.QuestListEntry rumble$entry();
    }

    @Mixin(targets = "org.arcadia.arc_quest.client.hud.quest.journal.JournalGroupEntryRenderer", remap = false)
    abstract static class Group {
        @Shadow @Final private QuestJournalScreen screen;

        @Inject(method = "render", at = @At("TAIL"))
        private void rumble$targetMainGroup(GuiGraphics graphics, @Coerce Object row, int x, int y, int w,
                                            int theme, float expansion, float hover, float alpha, CallbackInfo ci) {
            if (OpeningTutorialOverlay.at(Step.MAIN_GROUP) && ((GroupRowAccess) row).rumble$quests().stream()
                    .anyMatch(entry -> OpeningTutorialOverlay.mainQuest(entry.questId()))) {
                OpeningTutorialOverlay.observeMainGroup(screen, expansion >= .98f,
                        x + 2, y, w - 10, JournalConstants.ENTRY_HEIGHT - 2);
            }
        }
    }

    @Mixin(value = JournalListPanel.class, remap = false)
    abstract static class QuestRow {
        @Shadow @Final private QuestJournalScreen screen;

        @Inject(method = "renderQuestRow", at = @At("TAIL"))
        private void rumble$targetMainQuest(GuiGraphics graphics, @Coerce Object row, boolean selected, int x, int y,
                                           int w, float height, float hover, float alpha, CallbackInfo ci) {
            if (OpeningTutorialOverlay.at(Step.MAIN_QUEST)
                    && OpeningTutorialOverlay.mainQuest(((QuestRowAccess) row).rumble$entry().questId())) {
                OpeningTutorialOverlay.pointJournal(Step.MAIN_QUEST, screen, x + 2, y, w - 10, Math.round(height) - 2);
            }
        }
    }

    @Mixin(value = JournalDetailControls.class, remap = false)
    abstract static class Detail {
        @Shadow @Final private QuestJournalScreen screen;
        @Shadow protected abstract boolean shouldShowActiveBtns(QuestRuntimeData runtime);
        @Shadow protected abstract boolean shouldShowShop(QuestDefinition definition);
        @Shadow protected abstract boolean shouldShowConfirmBtn(QuestRuntimeData runtime);
        @Shadow protected abstract boolean shouldShowAbandon(QuestDefinition definition, QuestRuntimeData runtime);
        @Shadow protected abstract boolean shouldShowFailedBtns(JournalTypes.QuestListEntry entry);

        @Inject(method = "render", at = @At("TAIL"))
        private void rumble$targetTrack(GuiGraphics graphics, JournalTypes.QuestListEntry entry, QuestDefinition definition,
                                        QuestRuntimeData runtime, int x, int y, int w, int h, int mouseX, int mouseY,
                                        float delta, int theme, CallbackInfo ci) {
            if (!OpeningTutorialOverlay.at(Step.TRACK) || OpeningTutorialOverlay.trackingMainQuest()
                    || !OpeningTutorialOverlay.mainQuest(entry.questId())
                    || !shouldShowActiveBtns(runtime)) return;
            int count = 1 + (shouldShowShop(definition) ? 1 : 0) + (shouldShowConfirmBtn(runtime) ? 1 : 0)
                    + (shouldShowAbandon(definition, runtime) ? 1 : 0) + (shouldShowFailedBtns(entry) ? 1 : 0);
            int btnW = Math.min(110, (w - 8 * (count + 1)) / count);
            OpeningTutorialOverlay.pointJournal(Step.TRACK, screen, x + w - 8 - btnW, y + h - 28, btnW, 20);
        }
    }
}
