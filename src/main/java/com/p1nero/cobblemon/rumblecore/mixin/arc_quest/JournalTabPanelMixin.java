package com.p1nero.cobblemon.rumblecore.mixin.arc_quest;

import org.arcadia.arc_quest.client.hud.quest.journal.JournalTabPanel;
import org.arcadia.arc_quest.client.hud.quest.journal.JournalTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Hides the failed tab from ArcQuest's quest journal. */
@Mixin(value = JournalTabPanel.class, remap = false)
public class JournalTabPanelMixin {

    @Redirect(
            method = "buildTabs",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/arcadia/arc_quest/client/hud/quest/journal/JournalTypes$Tab;values()[Lorg/arcadia/arc_quest/client/hud/quest/journal/JournalTypes$Tab;"
            )
    )
    private JournalTypes.Tab[] hideFailedTab() {
        return new JournalTypes.Tab[]{JournalTypes.Tab.ACTIVE, JournalTypes.Tab.COMPLETED};
    }
}
