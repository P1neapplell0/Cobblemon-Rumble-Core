package com.p1nero.cobblemon.rumblecore.gym;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.johnn.realtimecobblemon.moves.Kits;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.RegistryAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Opt-in smoke test against the loaded Cobblemon and RTC registries. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class ArenaRosterAudit {
    private final RegistryAccess registries;
    private int rosters, pokemon, basic, unavailable, noRanged, failures;

    private ArenaRosterAudit(RegistryAccess registries) { this.registries = registries; }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean("rumble.auditArenaRosters")) return;
        new ArenaRosterAudit(event.getServer().registryAccess()).run();
    }

    private void run() {
        for (var encounter : GymCatalog.ALL) {
            boolean authored = encounter.leader() && encounter.gym() == GymCatalog.Gym.GROUND;
            for (var actor : encounter.actors()) check(actor.id(), actor.team(), authored);
            if (encounter.leader()) check(encounter.id() + "/hard", () -> GymLeaderTeams.hard(encounter.gym()), authored);
        }
        check("restock_jessie", GymTeamPokemon::jessie, true);
        check("restock_james", GymTeamPokemon::james, true);
        check("tera_cassidy", GymTeamPokemon::cassidy, true);
        check("tera_butch", GymTeamPokemon::butch, true);
        check("may", GymTeamPokemon::may, true);
        check("champion", GymTeamPokemon::champion, true);
        for (var id : MegaTrials.ids()) check(id, MegaTrials.get(id).actor().team(), true);
        for (var trial : LegendaryTrials.all()) {
            if (!trial.supported()) { unavailable(trial.id()); continue; }
            check(trial.id(), () -> List.of(trial.foe().create(trial.id())), true);
        }
        for (var trial : CinemaTrials.all()) {
            if (!trial.supported()) { unavailable(trial.id()); continue; }
            for (int side = 0; side < trial.sides().size(); side++) {
                var cast = trial.sides().get(side);
                for (var slot : cast) checkCinema(trial.template() + "/" + side, slot);
            }
        }
        GymCatalog.LOG.info("[ArenaAudit] {} rosters / {} Pokémon; {} basic-attack fallbacks; {} unavailable; {} without legal ranged options; {} failed",
                rosters, pokemon, basic, unavailable, noRanged, failures);
        if (failures != 0) throw new IllegalStateException("Arena roster audit failed: " + failures);
    }

    private void checkCinema(String id, CinemaTrials.Cast cast) {
        try {
            // Inspect the actual movie loadout, without the gym audit's extra preparation step.
            var mon = cast.create(id);
            var moves = mon.getMoveSet().getMoveTemplates();
            var actual = moves.stream().map(move -> move.getName()).toList();
            if (!actual.equals(cast.moves()) || !mon.getForm().getMoves().getAllLegalMoves().containsAll(moves)
                    || !moves.stream().allMatch(GymTeamPokemon::isArenaAttack)
                    || moves.stream().noneMatch(GymTeamPokemon::isCinemaRangedAttack)) {
                throw new IllegalStateException("Invalid movie loadout for " + cast.species() + "/" + cast.aspect()
                        + ": expected " + cast.moves() + ", got " + actual);
            }
            if (!Kits.moves(mon, false).equals(mon.getMoveSet().getMoves())) {
                throw new IllegalStateException("RTC replaced the scripted movie loadout: " + cast.species());
            }
            rosters++;
            pokemon++;
        } catch (RuntimeException error) {
            failures++;
            GymCatalog.LOG.error("[ArenaAudit] Failed to prepare movie role in {}", id, error);
        }
    }

    private void unavailable(String id) {
        unavailable++;
        GymCatalog.LOG.warn("[ArenaAudit] {} unavailable with the current registries", id);
    }

    private void check(String id, Supplier<List<Pokemon>> source, boolean preferAuthored) {
        try {
            var team = source.get();
            if (team.isEmpty() || team.size() > 6) throw new IllegalStateException("Invalid team size: " + team.size());
            for (var original : team) {
                var mon = original.clone(false, registries);
                GymTeamPokemon.prepareArenaMoves(mon, id, preferAuthored);
                var moves = mon.getMoveSet().getMoveTemplates();
                var legal = new HashSet<>(mon.getForm().getMoves().getAllLegalMoves());
                if (!legal.containsAll(moves) || moves.size() > 4 || new HashSet<>(moves).size() != moves.size()) {
                    throw new IllegalStateException("Illegal resulting moveset for " + mon.getSpecies().getName());
                }
                boolean usesBasic = moves.stream().noneMatch(GymTeamPokemon::isArenaAttack);
                if (usesBasic) {
                    basic++;
                    throw new IllegalStateException("No damaging RTC move for " + mon.getSpecies().getName());
                }
                boolean ranged = moves.stream().anyMatch(GymTeamPokemon::isRangedAttack);
                if (!ranged) {
                    if (legal.stream().anyMatch(GymTeamPokemon::isRangedAttack))
                        throw new IllegalStateException("Lost legal ranged option for " + mon.getSpecies().getName());
                    noRanged++;
                    GymCatalog.LOG.warn("[ArenaAudit] {} {} has no legal ranged option", id, mon.getSpecies().getName());
                }
                if (preferAuthored) {
                    for (var move : original.getMoveSet().getMoveTemplates()) {
                        if (legal.contains(move) && GymTeamPokemon.isArenaAttack(move) && !moves.contains(move)
                                && !(ranged && original.getMoveSet().getMoveTemplates().stream().noneMatch(GymTeamPokemon::isRangedAttack))) {
                            throw new IllegalStateException("Lost authored move " + move.getName() + " for " + mon.getSpecies().getName());
                        }
                    }
                }
                pokemon++;
            }
            rosters++;
        } catch (RuntimeException error) {
            failures++;
            GymCatalog.LOG.error("[ArenaAudit] Failed to prepare {}", id, error);
        }
    }
}
