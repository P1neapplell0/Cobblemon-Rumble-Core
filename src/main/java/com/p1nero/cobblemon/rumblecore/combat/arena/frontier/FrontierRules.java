package com.p1nero.cobblemon.rumblecore.combat.arena.frontier;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.IntStream;

public final class FrontierRules {
    public static final List<Integer> TIMED_MINUTES = List.of(3, 5, 10, 20);
    public static final int DEFAULT_TIMED_MINUTES = 5;

    private FrontierRules() {}

    public static boolean isValidTeamDuration(int minutes) {
        return minutes == 0 || TIMED_MINUTES.contains(minutes);
    }

    static boolean draftTurn(boolean host, int home, int away) {
        if (home < 0 || away < 0 || home > FrontierRoster.TEAM_SIZE || away > FrontierRoster.TEAM_SIZE) return false;
        return host ? home < FrontierRoster.TEAM_SIZE && home == away : home == away + 1;
    }

    public static int winner(boolean timed, boolean blueStanding, boolean redStanding, int blueScore, int redScore) {
        if (blueStanding != redStanding) return blueStanding ? 0 : 1;
        if (!timed || !blueStanding || blueScore == redScore) return -1;
        return blueScore > redScore ? 0 : 1;
    }

    static List<BigInteger> teamPayout(BigInteger stake, int total, int winners) {
        if (winners < 1 || winners > total || stake.signum() < 0) throw new IllegalArgumentException("Invalid team pot");
        var split = stake.multiply(BigInteger.valueOf(total)).divideAndRemainder(BigInteger.valueOf(winners));
        return IntStream.range(0, winners).mapToObj(i -> split[0].add(i < split[1].intValueExact()
                ? BigInteger.ONE : BigInteger.ZERO)).toList();
    }
}
