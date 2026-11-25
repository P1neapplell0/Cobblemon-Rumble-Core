package com.p1nero.cobblemon.rumblecore.combat.reward;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource;
import com.cobblemon.mod.common.api.tags.CobblemonItemTags;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.Smash;
import com.johnn.realtimecobblemon.SmashConfig;
import com.p1nero.cobblemon.rumblecore.compat.expall.ExpAllCompat;
import java.util.function.ToIntFunction;
import net.minecraft.resources.ResourceLocation;

/** Adds bench rewards to RTC's existing one-time wild knockout award. */
public final class RealtimeSharedExperience {
    private static final ResourceLocation GAINED_PACKET = ResourceLocation.fromNamespaceAndPath("cobblemon", "exp_gained_data");
    private static final SidemodExperienceSource HELD_SHARE = new SidemodExperienceSource("realtimecobblemon");
    private static final SidemodExperienceSource EXP_ALL = new SidemodExperienceSource("cobblemonexpall");

    private RealtimeSharedExperience() {}

    public static void award(Smash.Session session, ToIntFunction<Pokemon> baseExperience) {
        var player = session.player;
        var participant = session.pokemon.getPokemon();
        var config = Cobblemon.INSTANCE.getConfig();
        double heldMultiplier = config.getExperienceShareMultiplier();
        double expAllMultiplier = ExpAllCompat.multiplier(player);
        int cap = SmashConfig.get(SmashConfig.XP_LEVEL_CAP);
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);
        for (Pokemon recipient : party.toGappyList()) {
            if (recipient == null || recipient.getUuid().equals(participant.getUuid())) continue;
            boolean held = heldMultiplier > 0 && recipient.heldItem().is(CobblemonItemTags.EXPERIENCE_SHARE);
            if (recipient.isFainted() && (!held || !config.getAwardExperienceToFaintedPokemon())) continue;
            double multiplier = held ? heldMultiplier : expAllMultiplier; // Native share takes precedence, never stack both.
            if (!Double.isFinite(multiplier) || multiplier <= 0) continue;
            int xp = (int) Math.min(Integer.MAX_VALUE, Math.round(baseExperience.applyAsInt(recipient) * multiplier));
            if (cap > 0) xp = Math.min(xp, Math.max(0, recipient.getExperienceToLevel(recipient.getLevel() + cap)));
            if (xp <= 0) continue;
            var source = held ? HELD_SHARE : EXP_ALL;
            if (player.connection.hasChannel(GAINED_PACKET)) recipient.addExperienceWithPlayer(player, source, xp);
            else recipient.addExperience(source, xp);
        }
    }
}
