package com.p1nero.cobblemon.rumblecore.combat.arena.frontier;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public final class FrontierBracketCheck {
    public static void main(String[] args) {
        for (int count = 3; count <= 128; count++) complete(count);
        var seeds = seeds(3);
        UUID a = seeds.get(0), b = seeds.get(1);
        var bout = new FrontierBracket.Bout(a, b, false);
        check(!bout.resolveUnavailable(false, false, false, true, false), "wait before deadline");
        check(!bout.resolveUnavailable(false, false, true, true, true), "both ready can launch");
        check(bout.resolveUnavailable(false, false, true, true, false) && a.equals(bout.winner), "one ready wins at deadline");
        bout.resolve(b);
        check(a.equals(bout.winner), "duplicate result ignored");
        bout = new FrontierBracket.Bout(a, b, false);
        check(bout.resolveUnavailable(true, false, false, false, false) && b.equals(bout.winner), "withdrawal forfeits immediately");
        bout = new FrontierBracket.Bout(a, b, false);
        bout.running = true;
        check(!bout.resolveUnavailable(false, false, true, false, false), "deadline does not interrupt a match");
        var noChampion = new FrontierBracket(seeds, 0);
        noChampion.current().forEach(match -> match.resolve(match.left));
        noChampion.advance(0);
        noChampion.current().getFirst().resolveUnavailable(false, false, true, false, false);
        noChampion.advance(0);
        check(noChampion.finished && noChampion.prizes(BigInteger.TEN).isEmpty(), "no champion takes the refund path");
        System.out.println("Frontier bracket checks passed (3-128 entrants, byes, placement, timeout, forfeit, no champion)");
    }

    private static void complete(int count) {
        var bracket = new FrontierBracket(seeds(count), 10);
        check(bracket.current().stream().filter(b -> b.done).count() == (1 << bracket.depth) - count, "opening byes: " + count);
        bracket.advance(100);
        check(bracket.rounds.size() == 1, "do not advance past unfinished bouts");
        int played = 0;
        while (!bracket.finished) {
            var seen = new HashSet<UUID>();
            for (var bout : bracket.current()) {
                check((bout.left == null || seen.add(bout.left)) && (bout.right == null || seen.add(bout.right)), "one bout per player per round");
                if (!bout.done) { played++; bout.resolve(bout.left); }
            }
            bracket.advance(100);
        }
        check(played == count - 1 + (count >= 4 ? 1 : 0), "elimination plus bronze match: " + count);
        check(bracket.current().size() == (count == 3 ? 1 : 2), "finals: " + count);
        var pot = BigInteger.valueOf(103);
        var prizes = bracket.prizes(pot);
        check(prizes.size() == Math.min(4, count), "paid places: " + count);
        check(prizes.values().stream().reduce(BigInteger.ZERO, BigInteger::add).equals(pot), "pot conservation: " + count);
        check(prizes.get(bracket.current().getFirst().winner).intValueExact() == (count == 3 ? 63 : 53), "rounding goes to champion");
    }

    private static List<UUID> seeds(int count) {
        return IntStream.range(0, count).mapToObj(i -> new UUID(0, i + 1)).toList();
    }

    private static void check(boolean valid, String label) {
        if (!valid) throw new AssertionError(label);
    }
}
