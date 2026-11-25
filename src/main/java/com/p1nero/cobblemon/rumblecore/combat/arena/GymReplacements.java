package com.p1nero.cobblemon.rumblecore.combat.arena;

/** Shared by the entire gym team; only a successful post-KO replacement spends a use. */
final class GymReplacements {
    private int remaining = 6;

    int remaining() { return remaining; }

    boolean available() { return remaining > 0; }

    void spend() {
        if (!available()) throw new IllegalStateException("Gym replacements exhausted");
        remaining--;
    }
}
