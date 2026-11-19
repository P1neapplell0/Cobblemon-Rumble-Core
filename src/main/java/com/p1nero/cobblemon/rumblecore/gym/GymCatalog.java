package com.p1nero.cobblemon.rumblecore.gym;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/** Attendant placements and scripts from the original Trial Hall. */
public final class GymCatalog {
    static final Logger LOG = LogUtils.getLogger();

    private GymCatalog() {}

    public enum Gym {
        ROCK, GRASS, GHOST, ICE, GROUND, FIRE, DARK, ELECTRIC;

        public String path() { return name().toLowerCase(Locale.ROOT); }
    }

    public record Actor(String id, BlockPos pos, Supplier<List<Pokemon>> team, Component displayName) {
        public Actor(String id, BlockPos pos, Supplier<List<Pokemon>> team) { this(id, pos, team, null); }
        public String key() { return "rumble.gym.actor." + id; }
        public Component name() { return displayName == null ? Component.translatable(key()) : displayName.copy(); }
        String aspect() { return "rumble-gym-" + id.replace('_', '-'); }
    }

    public record Encounter(String id, Gym gym, boolean leader, List<Actor> actors) {}

    // The original position stood over a Supplementaries lamp. Use the nearby clear boardwalk.
    public static final Actor GRASS_SECOND_ATTENDANT = a("grass_2", -154, 47, -87);

    static final List<Encounter> ALL = createEncounters();

    static List<Encounter> forGym(Gym gym) { return ALL.stream().filter(e -> e.gym() == gym).toList(); }
    public static Encounter leader(Gym gym) {
        return ALL.stream().filter(e -> e.gym() == gym && e.leader()).findFirst().orElseThrow();
    }

    private static Actor a(String id, int x, int y, int z) {
        return new Actor(id, new BlockPos(x, y, z), () -> GymMinionTeams.create(id));
    }

    private static Encounter leader(Gym gym, int x, int y, int z, Supplier<List<Pokemon>> team) {
        String id = gym.path() + "_leader";
        return new Encounter(id, gym, true, List.of(new Actor(id, new BlockPos(x, y, z), team)));
    }

    static boolean validateTeams() {
        return GymMinionTeams.validateAll() & GymLeaderTeams.validateAll();
    }

    private static List<Encounter> createEncounters() {
        return List.of(
                e(Gym.ROCK, 1,
                        a("rock_1", -35, 58, -155)),
                e(Gym.ROCK, 2,
                        a("rock_2a", 14, 58, -163),
                        a("rock_2b", 13, 58, -163)),

                e(Gym.GRASS, 1,
                        a("grass_1", -105, 48, -168)),
                e(Gym.GRASS, 2, GRASS_SECOND_ATTENDANT),

                e(Gym.GHOST, 1,
                        a("ghost_1a", -150, 59, 38),
                        a("ghost_1b", -151, 59, 38)),
                e(Gym.GHOST, 2,
                        a("ghost_2a", -184, 53, -36),
                        a("ghost_2b", -184, 53, -35)),

                e(Gym.ICE, 1,
                        a("ice_1", -122, 57, 132)),
                e(Gym.ICE, 2,
                        a("ice_2a", -148, 58, 117),
                        a("ice_2b", -148, 58, 116)),

                e(Gym.GROUND, 1,
                        a("ground_1", -19, 49, 141)),
                e(Gym.GROUND, 2,
                        a("ground_2", -32, 49, 157)),
                e(Gym.GROUND, 3,
                        a("ground_3", 6, 50, 160)),

                e(Gym.FIRE, 1,
                        a("fire_1", 124, 56, 120)),
                e(Gym.FIRE, 2,
                        a("fire_2", 124, 59, 149)),
                e(Gym.FIRE, 3,
                        a("fire_3a", 100, 57, 143),
                        a("fire_3b", 99, 57, 143)),
                e(Gym.FIRE, 4,
                        a("fire_4a", 143, 58, 141),
                        a("fire_4b", 143, 58, 142)),

                e(Gym.DARK, 1,
                        a("dark_1", 129, 61, -10)),
                e(Gym.DARK, 2,
                        a("dark_2", 140, 61, 18)),
                e(Gym.DARK, 3,
                        a("dark_3", 125, 61, 25)),
                e(Gym.DARK, 4,
                        a("dark_4", 158, 61, 29)),
                e(Gym.DARK, 5,
                        a("dark_5", 154, 65, -19)),

                e(Gym.ELECTRIC, 1,
                        a("electric_1a", 96, 57, -110),
                        a("electric_1b", 97, 57, -110)),
                e(Gym.ELECTRIC, 2,
                        a("electric_2a", 111, 57, -104),
                        a("electric_2b", 111, 57, -105)),
                e(Gym.ELECTRIC, 3,
                        a("electric_3a", 80, 59, -145),
                        a("electric_3b", 81, 59, -145)),
                leader(Gym.ROCK, -12, 74, -209, GymLeaderTeams::rock),
                leader(Gym.GRASS, -165, 57, -139, GymLeaderTeams::grass),
                leader(Gym.GHOST, -195, 67, 15, GymLeaderTeams::ghost),
                leader(Gym.ICE, -155, 71, 164, GymLeaderTeams::ice),
                leader(Gym.GROUND, -15, 63, 202, GymLeaderTeams::ground),
                leader(Gym.FIRE, 150, 68, 188, GymLeaderTeams::fire),
                leader(Gym.DARK, 195, 71, 8, GymLeaderTeams::dark),
                leader(Gym.ELECTRIC, 143, 66, -139, GymLeaderTeams::electric));
    }

    private static Encounter e(Gym gym, int slot, Actor... actors) {
        return new Encounter(gym.path() + "_" + slot, gym, false, List.of(actors));
    }
}
