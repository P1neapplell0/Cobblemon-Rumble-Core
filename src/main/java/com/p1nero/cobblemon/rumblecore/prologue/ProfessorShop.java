package com.p1nero.cobblemon.rumblecore.prologue;

import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import net.minecraft.server.level.ServerPlayer;
import org.arcadia.arc_quest.questplayer.ArcQuestPlayerManager;

/** ArcQuest owns the unlock and dialogue; CobbleDollars owns both native menus. */
public final class ProfessorShop {
    static final String UNLOCKED = "rumble.professor.trading";

    private ProfessorShop() {}

    static void afterDialogue(ServerPlayer player, boolean sell) {
        if (!PrologueStory.atDialogueProfessor(player)) return;
        PrologueStory.after(player, () -> {
            var doctor = PrologueStory.nearby(player);
            if (doctor == null || !canTrade(player, doctor)) return;
            if (sell) PlayerExtensionKt.openBank(player, doctor);
            else {
                PlayerExtensionKt.openShop(player, doctor);
                doctor.getTradingPlayers().add(player);
            }
        });
    }

    public static boolean canTrade(ServerPlayer player, ProfessorEntity doctor) {
        return PrologueStory.at(player, doctor) && ArcQuestPlayerManager.getOrCreate(player).hasFlag(UNLOCKED);
    }
}
