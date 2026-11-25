package com.p1nero.cobblemon.rumblecore.combat.arena;

public final class GymReplacementsCheck {
    public static void main(String[] args) {
        var shared = new GymReplacements();
        var otherTeam = new GymReplacements();
        // Any number of players may check availability without charging the team.
        for (int player = 0; player < 6; player++) check(shared.available() && shared.remaining() == 6);
        for (int left = 6; left > 0; left--) {
            check(shared.available() && shared.remaining() == left);
            shared.spend();
        }
        check(!shared.available() && shared.remaining() == 0);
        try {
            shared.spend();
            throw new AssertionError("A seventh replacement was accepted");
        } catch (IllegalStateException expected) {
            check(shared.remaining() == 0 && otherTeam.remaining() == 6);
        }
        System.out.println("Gym shared replacement limit, exhaustion, and per-room isolation checks passed");
    }

    private static void check(boolean pass) { if (!pass) throw new AssertionError("Gym replacement limit regression"); }
}
