package com.p1nero.cobblemon.rumblecore.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.johnn.realtimecobblemon.MoveCombat;
import com.johnn.realtimecobblemon.moves.Conditions;
import com.johnn.realtimecobblemon.moves.MoveInfo;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

public final class RealtimeAiDamage {
    private RealtimeAiDamage() {}

    public static boolean basic(MoveInfo move) {
        return move.id().equals("brawljab") || move.id().equals("brawlheavy");
    }

    public static boolean critical(MoveInfo move, RandomSource random) {
        if (basic(move)) return false;
        // Same Gen 9 critical tiers as RTC's player move pipeline.
        return random.nextDouble() < switch (move.critRatio()) {
            case 0, 1 -> 1.0 / 24;
            case 2 -> 1.0 / 8;
            case 3 -> .5;
            default -> 1;
        };
    }

    public static float calculate(PokemonEntity actor, PokemonEntity body, LivingEntity target, double power,
            boolean special, boolean stab, float type, float random, boolean crit) {
        var user = actor.getPokemon();
        var defender = body.getPokemon();
        int attackStage = Conditions.stage(actor, special ? "spa" : "atk");
        int defenseStage = Conditions.stage(target, special ? "spd" : "def");
        if (crit) {
            attackStage = Math.max(0, attackStage);
            defenseStage = Math.min(0, defenseStage);
        }
        double attack = (special ? user.getSpecialAttack() : user.getAttack()) * Conditions.stageMult(attackStage);
        double defense = (special ? defender.getSpecialDefence() : defender.getDefence()) * Conditions.stageMult(defenseStage);
        if (!special && "brn".equals(Conditions.status(actor))) attack *= .5;
        float result = MoveCombat.scaled(user.getLevel(), defender.getLevel(), power, attack, defense, stab, type, random);
        return crit ? result * 1.5F : result;
    }
}
