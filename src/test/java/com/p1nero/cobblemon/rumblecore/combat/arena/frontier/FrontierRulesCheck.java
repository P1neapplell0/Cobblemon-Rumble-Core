package com.p1nero.cobblemon.rumblecore.combat.arena.frontier;

import java.math.BigInteger;
import java.util.List;

public final class FrontierRulesCheck {
    public static void main(String[] args) {
        check(FrontierRules.TIMED_MINUTES.equals(List.of(3, 5, 10, 20)));
        check(FrontierRules.DEFAULT_TIMED_MINUTES == 5);
        for (int minutes : List.of(0, 3, 5, 10, 20)) check(FrontierRules.isValidTeamDuration(minutes));
        for (int minutes : List.of(-1, 1, 4, 15, 60)) check(!FrontierRules.isValidTeamDuration(minutes));
        for (int i = 0; i < 3; i++) {
            check(FrontierRules.draftTurn(true, i, i) && !FrontierRules.draftTurn(false, i, i));
            check(!FrontierRules.draftTurn(true, i + 1, i) && FrontierRules.draftTurn(false, i + 1, i));
        }
        check(!FrontierRules.draftTurn(true, 3, 3) && !FrontierRules.draftTurn(false, 3, 3));
        check(FrontierRules.winner(true, true, true, 2, 3) == 1);
        check(FrontierRules.winner(true, true, true, 2, 2) == -1);
        check(FrontierRules.winner(false, false, true, 4, 2) == 1);
        check(FrontierRules.winner(false, false, false, 4, 2) == -1);
        check(FrontierRules.teamPayout(BigInteger.valueOf(5), 5, 3).equals(List.of(BigInteger.valueOf(9), BigInteger.valueOf(8), BigInteger.valueOf(8))));
        System.out.println("Frontier duration, draft, team victory and payout checks passed");
    }

    private static void check(boolean pass) { if (!pass) throw new AssertionError("Frontier rules regression"); }
}
