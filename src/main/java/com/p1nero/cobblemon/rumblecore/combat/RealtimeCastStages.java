package com.p1nero.cobblemon.rumblecore.combat;

/** Per-cast state implemented by the RTC Cast mixin. */
public interface RealtimeCastStages {
    boolean rumble$hasPendingStatDrop();

    void rumble$applyStatDrop();
}
