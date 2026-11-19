package com.p1nero.cobblemon.rumblecore.combat.arena;

/** The recall body remains alive briefly after the target is knocked out. */
public interface ArenaKnockoutVisual {
    void rumble$markKnockedOut();
    boolean rumble$isKnockedOut();
}
