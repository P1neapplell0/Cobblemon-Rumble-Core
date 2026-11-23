package com.p1nero.cobblemon.rumblecore.combat.arena.frontier;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Single elimination with opening byes and a third-place match alongside the final. */
final class FrontierBracket {
    static final class Bout {
        final UUID left, right;
        final boolean bronze;
        boolean running, done;
        UUID winner;

        Bout(UUID left, UUID right, boolean bronze) {
            this.left = left;
            this.right = right;
            this.bronze = bronze;
            if (left == null || right == null) resolve(left == null ? right : left);
        }

        boolean includes(UUID player) { return player.equals(left) || player.equals(right); }
        UUID opponent(UUID player) { return player.equals(left) ? right : left; }
        UUID loser() { return winner == null ? null : opponent(winner); }

        boolean resolveUnavailable(boolean leftOut, boolean rightOut, boolean expired, boolean leftReady, boolean rightReady) {
            if (done || running) return false;
            if (leftOut || rightOut) resolve(leftOut == rightOut ? null : leftOut ? right : left);
            else if (expired && !(leftReady && rightReady)) resolve(leftReady == rightReady ? null : leftReady ? left : right);
            else return false;
            return true;
        }

        void resolve(UUID victor) {
            if (done || victor != null && !includes(victor)) return;
            winner = victor;
            running = false;
            done = true;
        }
    }

    final List<List<Bout>> rounds = new ArrayList<>();
    final int size, depth;
    boolean finished;
    int opensAt, closesAt;

    FrontierBracket(List<UUID> shuffled, int now) {
        size = shuffled.size();
        if (size < 3 || size > (1 << 30) || shuffled.stream().distinct().count() != size) throw new IllegalArgumentException("Invalid bracket seeds");
        int slots = Integer.highestOneBit(size - 1) << 1;
        depth = Integer.numberOfTrailingZeros(slots);
        List<Integer> seeds = List.of(0, 1);
        for (int span = 4; span <= slots; span *= 2) {
            var next = new ArrayList<Integer>(span);
            for (int seed : seeds) { next.add(seed); next.add(span - 1 - seed); }
            seeds = next;
        }
        var first = new ArrayList<Bout>();
        for (int i = 0; i < slots; i += 2) first.add(new Bout(seed(shuffled, seeds.get(i)), seed(shuffled, seeds.get(i + 1)), false));
        add(first, now);
    }

    private static UUID seed(List<UUID> seeds, int i) { return i < seeds.size() ? seeds.get(i) : null; }
    private void add(List<Bout> bouts, int now) {
        rounds.add(List.copyOf(bouts));
        opensAt = now + 100;
        closesAt = opensAt + 20 * 180;
    }

    List<Bout> current() { return rounds.getLast(); }
    Bout bout(UUID player) { return current().stream().filter(b -> b.includes(player)).findFirst().orElse(null); }

    void advance(int now) {
        var current = current();
        if (finished || current.stream().anyMatch(b -> !b.done)) return;
        if (rounds.size() == depth) { finished = true; return; }
        var next = new ArrayList<Bout>();
        for (int i = 0; i < current.size(); i += 2) next.add(new Bout(current.get(i).winner, current.get(i + 1).winner, false));
        if (next.size() == 1 && size >= 4) next.add(new Bout(current.getFirst().loser(), current.get(1).loser(), true));
        add(next, now);
    }

    Map<UUID, BigInteger> prizes(BigInteger pot) {
        var awards = new LinkedHashMap<UUID, BigInteger>();
        if (!finished || current().getFirst().winner == null) return awards;
        var finale = current().getFirst();
        UUID[] places = { finale.winner, finale.loser(), null, null };
        if (size == 3) {
            places[2] = rounds.getFirst().stream().map(Bout::loser).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        } else {
            places[2] = current().get(1).winner;
            places[3] = current().get(1).loser();
        }
        int[] rates = size == 3 ? new int[]{60, 25, 15, 0} : new int[]{50, 25, 15, 10};
        int weight = 0;
        for (int i = 0; i < places.length; i++) if (places[i] != null) weight += rates[i];
        BigInteger paid = BigInteger.ZERO;
        for (int i = 0; i < places.length; i++) if (places[i] != null) {
            var prize = pot.multiply(BigInteger.valueOf(rates[i])).divide(BigInteger.valueOf(weight));
            awards.put(places[i], prize);
            paid = paid.add(prize);
        }
        awards.merge(finale.winner, pot.subtract(paid), BigInteger::add);
        return awards;
    }
}
