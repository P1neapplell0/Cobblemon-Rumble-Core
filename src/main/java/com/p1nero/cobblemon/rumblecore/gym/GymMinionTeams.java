package com.p1nero.cobblemon.rumblecore.gym;

import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** RCT-ready teams for the Gym attendants listed in the review checklist. */
public final class GymMinionTeams {

    private static final GymTeamPokemon.TrainingStats UNTRAINED_STATS =
            new GymTeamPokemon.TrainingStats(0, 0, 0, 0, 0, 0);

    private GymMinionTeams() {
    }

    public static boolean validateAll() {
        for (String actorId : actorIds()) {
            if (create(actorId).isEmpty()) {
                GymCatalog.LOG.error("[GymMinionTeam] No usable Pokemon for {}", actorId);
                return false;
            }
        }
        GymCatalog.LOG.info("[GymMinionTeam] Validated all 32 Gym attendant teams");
        return true;
    }

    public static List<Pokemon> create(String actorId) {
        return switch (actorId) {
            case "rock_1" -> team(actorId,
                    p("roggenrola", 10, "impish", "sturdy", "",
                            "tackle", "harden", "sandattack"));
            case "rock_2a" -> team(actorId,
                    p("cranidos", 12, "adamant", "moldbreaker", "",
                            "headbutt", "rocksmash", "leer"));
            case "rock_2b" -> team(actorId,
                    p("shieldon", 12, "sassy", "sturdy", "",
                            "tackle", "protect", "bulldoze"));

            case "grass_1" -> team(actorId,
                    p("oddish", 18, "bold", "chlorophyll", "",
                            "acid", "poisonpowder", "gigadrain", "growth"),
                    f("voltorb", 20, "modest", "aftermath", "", "GENDERLESS",
                            Set.of("hisuian"), "chargebeam", "swift", "leechseed", "charge"));
            case "grass_2" -> team(actorId,
                    p("deerling", 18, "jolly", "sapsipper", "",
                            "sandattack", "doublekick", "tackle", "bulletseed"),
                    p("skiddo", 20, "impish", "sapsipper", "",
                            "razorleaf", "leechseed", "rollout", "growth"));

            case "ghost_1a" -> team(actorId,
                    p("bramblin", 21, "impish", "windrider", "",
                            "shadowsneak", "bulletseed", "confuseray", "rollout"),
                    p("sandygast", 22, "quiet", "watercompaction", "",
                            "sandtomb", "gigadrain", "confuseray", "rocktomb"));
            case "ghost_1b" -> team(actorId,
                    p("litwick", 21, "modest", "flamebody", "",
                            "smog", "willowisp", "ember", "hex"),
                    p("golett", 24, "adamant", "ironfist", "",
                            "shadowpunch", "stompingtantrum", "curse", "defensecurl"));
            case "ghost_2a" -> team(actorId,
                    p("shuppet", 22, "jolly", "frisk", "",
                            "shadowsneak", "willowisp", "screech", "protect"),
                    p("sinistea", 24, "relaxed", "cursedbody", "",
                            "gigadrain", "confuseray", "suckerpunch", "protect"));
            case "ghost_2b" -> team(actorId,
                    p("duskull", 22, "impish", "levitate", "",
                            "shadowsneak", "payback", "curse", "confuseray"),
                    p("drifloon", 24, "bold", "aftermath", "",
                            "shadowball", "gust", "clearsmog", "aircutter"));

            case "ice_1" -> team(actorId,
                    p("bergmite", 24, "careful", "sturdy", "",
                            "avalanche", "bite", "mirrorcoat", "rocktomb"),
                    p("vanillite", 24, "bold", "snowcloak", "focus_sash",
                            "hail", "icywind", "lightscreen", "taunt"),
                    p("cetitan", 32, "adamant", "slushrush", "sitrus_berry",
                            "bellydrum", "avalanche", "bulldoze", "superpower"));
            case "ice_2a" -> team(actorId,
                    p("snover", 30, "relaxed", "snowwarning", "",
                            "icywind", "razorleaf", "iceshard", "mist"),
                    f("mrmime", 32, "bold", "icebody", "", "MALE",
                            Set.of("galarian"), "psybeam", "icywind", "fakeout", "lightscreen"));
            case "ice_2b" -> team(actorId,
                    f("sandslash", 32, "jolly", "slushrush", "focus_sash", "MALE",
                            Set.of("alolan"), "metalclaw", "rockslide", "iciclespear", "swordsdance"));

            case "ground_1" -> team(actorId,
                    p("rhydon", 42, "sassy", "reckless", "",
                            "bulldoze", "rockslide", "takedown", "hammerarm"),
                    p("golem", 40, "adamant", "sturdy", "normal_gem",
                            "explosion", "earthquake", "rockslide", "heavyslam"),
                    p("donphan", 43, "adamant", "sturdy", "life_orb",
                            "earthquake", "doubleedge", "ironhead", "rockslide"));
            case "ground_2" -> team(actorId,
                    p("clodsire", 40, "relaxed", "poisonpoint", "",
                            "stealthrock", "toxicspikes", "poisonjab", "highhorsepower"),
                    p("golurk", 43, "adamant", "ironfist", "sitrus_berry",
                            "shadowpunch", "thunderpunch", "firepunch", "highhorsepower"),
                    p("claydol", 43, "bold", "levitate", "focus_sash",
                            "extrasensory", "earthpower", "icebeam", "shadowball"));
            case "ground_3" -> team(actorId,
                    p("hippowdon", 44, "careful", "sandstream", "smooth_rock",
                            "earthquake", "crunch", "doubleedge", "stealthrock"),
                    p("sandslash", 42, "adamant", "sandrush", "life_orb",
                            "bulldoze", "knockoff", "rockslide", "brickbreak"),
                    p("flygon", 45, "jolly", "levitate", "focus_sash",
                            "earthquake", "dragonclaw", "stoneedge", "dragondance"));

            case "fire_1" -> team(actorId,
                    p("salazzle", 48, "timid", "corrosion", "",
                            "venoshock", "dragonpulse", "burningjealousy", "fakeout"),
                    p("centiskorch", 50, "adamant", "flamebody", "life_orb",
                            "firelash", "powerwhip", "knockoff", "leechlife"));
            case "fire_2" -> team(actorId,
                    p("heatmor", 48, "quiet", "flashfire", "",
                            "overheat", "firelash", "suckerpunch", "rocktomb"),
                    p("magmortar", 50, "timid", "flamebody", "life_orb",
                            "flamethrower", "scorchingsands", "thunderbolt", "psychic"));
            case "fire_3a" -> team(actorId,
                    p("torkoal", 48, "bold", "drought", "heat_rock",
                            "protect", "solarbeam", "heatwave", "earthpower"),
                    p("chandelure", 50, "modest", "flamebody", "focus_sash",
                            "solarbeam", "fireblast", "psychic", "shadowball"));
            case "fire_3b" -> team(actorId,
                    p("turtonator", 48, "timid", "shellarmor", "white_herb",
                            "shellsmash", "heatwave", "dragonpulse", "flashcannon"),
                    p("pyroar", 50, "timid", "unnerve", "life_orb",
                            "flamethrower", "hypervoice", "darkpulse", "willowisp"));
            case "fire_4a" -> team(actorId,
                    p("talonflame", 48, "jolly", "galewings", "flying_gem",
                            "tailwind", "acrobatics", "flareblitz", "uturn"),
                    p("arcanine", 48, "jolly", "intimidate", "assault_vest",
                            "extremespeed", "flareblitz", "closecombat", "playrough"),
                    p("volcarona", 50, "timid", "swarm", "focus_sash",
                            "quiverdance", "heatwave", "bugbuzz", "gigadrain"));
            case "fire_4b" -> team(actorId,
                    p("darmanitan", 48, "adamant", "sheerforce", "choice_band",
                            "flareblitz", "earthquake", "rockslide", "superpower"),
                    p("armarouge", 48, "modest", "weakarmor", "assault_vest",
                            "flamethrower", "expandingforce", "energyball", "scorchingsands"),
                    p("camerupt", 50, "bold", "solidrock", "",
                            "earthpower", "heatwave", "ancientpower", "protect"));

            case "dark_1" -> team(actorId,
                    f("linoone", 50, "jolly", "quickfeet", "flame_orb", "MALE",
                            Set.of("galarian"), "protect", "facade", "knockoff", "partingshot"),
                    p("crawdaunt", 52, "adamant", "adaptability", "assault_vest",
                            "crunch", "knockoff", "crabhammer", "aquajet"));
            case "dark_2" -> team(actorId,
                    p("mightyena", 50, "adamant", "intimidate", "sitrus_berry",
                            "crunch", "takedown", "poisonfang", "yawn"),
                    p("cacturne", 52, "adamant", "waterabsorb", "life_orb",
                            "poisonjab", "suckerpunch", "spikyshield", "destinybond"));
            case "dark_3" -> team(actorId,
                    p("liepard", 50, "adamant", "prankster", "",
                            "thunderwave", "suckerpunch", "nightslash", "fakeout"),
                    p("drapion", 52, "jolly", "battlearmor", "assault_vest",
                            "crosspoison", "xscissor", "earthquake", "knockoff"),
                    p("meowscarada", 54, "jolly", "protean", "muscle_band",
                            "flowertrick", "knockoff", "uturn", "playrough"),
                    p("lurantis", 54, "jolly", "contrary", "leftovers",
                            "knockoff", "superpower", "leafblade", "poisonjab"));
            case "dark_4" -> team(actorId,
                    p("malamar", 50, "adamant", "contrary", "",
                            "psychocut", "knockoff", "liquidation", "superpower"),
                    p("overqwil", 52, "adamant", "intimidate", "focus_sash",
                            "poisonjab", "crunch", "liquidation", "selfdestruct"),
                    p("greninja", 54, "timid", "protean", "life_orb",
                            "surf", "darkpulse", "icebeam", "extrasensory"),
                    p("sharpedo", 54, "jolly", "roughskin", "",
                            "crunch", "liquidation", "icefang", "closecombat"));
            case "dark_5" -> team(actorId,
                    p("honchkrow", 52, "adamant", "moxie", "flying_gem",
                            "acrobatics", "tailwind", "uturn", "perishsong"),
                    p("houndoom", 52, "timid", "flashfire", "life_orb",
                            "flamethrower", "darkpulse", "toxic", "sludgebomb"),
                    p("bisharp", 54, "adamant", "defiant", "eviolite",
                            "suckerpunch", "throatchop", "ironhead", "swordsdance"),
                    p("ironjugulis", 54, "timid", "quarkdrive",
                            "mega_showdown:booster_energy",
                            "darkpulse", "fireblast", "earthpower", "hurricane"),
                    p("absol", 54, "jolly", "superluck", "mega_showdown:absolite",
                            "suckerpunch", "knockoff", "closecombat", "nightslash"));

            case "electric_1a" -> team(actorId,
                    p("galvantula", 60, "timid", "swarm", "focus_sash",
                            "stickyweb", "bugbuzz", "thunderbolt", "gigadrain"),
                    f("oricorio", 60, "timid", "dancer", "sitrus_berry", "FEMALE",
                            Set.of("pom_pom-style"), "quiverdance", "revelationdance", "airslash", "helpinghand"),
                    p("bellibolt", 62, "sassy", "electromorphosis", "scope_lens",
                            "toxic", "thunderbolt", "muddywater", "zapcannon"));
            case "electric_1b" -> team(actorId,
                    p("kilowattrel", 60, "modest", "competitive", "choice_specs",
                            "thunderbolt", "voltswitch", "airslash", "weatherball"),
                    p("boltund", 60, "jolly", "strongjaw", "life_orb",
                            "crunch", "playrough", "wildcharge", "firefang"),
                    p("heliolisk", 62, "timid", "solarpower", "choice_scarf",
                            "thunderbolt", "electroweb", "grassknot", "hypervoice"));
            case "electric_2a" -> team(actorId,
                    f("toxtricity", 62, "modest", "punkrock", "throat_spray", "MALE",
                            Set.of("amped-form"), "overdrive", "boomburst", "toxic", "sludgewave"),
                    p("electrode", 60, "timid", "aftermath", "air_balloon",
                            "curse", "thunderbolt", "eerieimpulse", "thunderwave"),
                    f("golem", 62, "adamant", "galvanize", "focus_sash", "MALE",
                            Set.of("alolan"), "explosion", "doubleedge", "ironhead", "protect"));
            case "electric_2b" -> team(actorId,
                    f("toxtricity", 62, "modest", "punkrock", "focus_sash", "MALE",
                            Set.of("low_key-form"), "overdrive", "boomburst", "toxic", "sludgewave"),
                    f("electrode", 60, "modest", "aftermath", "rocky_helmet", "GENDERLESS",
                            Set.of("hisuian"), "voltswitch", "thunderbolt", "chloroblast", "reflect"),
                    p("lanturn", 62, "bold", "waterabsorb", "leftovers",
                            "thunderbolt", "muddywater", "icebeam", "thunderwave"));
            case "electric_3a" -> team(actorId,
                    p("pincurchin", 60, "impish", "electricsurge", "terrain_extender",
                            "toxicspikes", "zingzap", "poisonjab", "curse"),
                    p("vikavolt", 64, "calm", "levitate", "assault_vest",
                            "thunderbolt", "flashcannon", "discharge", "bugbuzz"),
                    p("magnezone", 64, "calm", "sturdy", "weakness_policy",
                            "flashcannon", "triattack", "thunderbolt", "discharge"));
            case "electric_3b" -> team(actorId,
                    f("raichu", 64, "modest", "surgesurfer", "life_orb", "MALE",
                            Set.of("alolan"), "risingvoltage", "psychic", "grassknot", "focusblast"),
                    p("manectric", 65, "timid", "static", "",
                            "thunderbolt", "voltswitch", "flamethrower", "hypervoice"),
                    p("electivire", 64, "jolly", "motordrive", "choice_scarf",
                            "supercellslam", "icepunch", "firepunch", "stompingtantrum"));
            default -> List.of();
        };
    }

    private static List<String> actorIds() {
        return List.of(
                "rock_1", "rock_2a", "rock_2b", "grass_1", "grass_2",
                "ghost_1a", "ghost_1b", "ghost_2a", "ghost_2b",
                "ice_1", "ice_2a", "ice_2b", "ground_1", "ground_2", "ground_3",
                "fire_1", "fire_2", "fire_3a", "fire_3b", "fire_4a", "fire_4b",
                "dark_1", "dark_2", "dark_3", "dark_4", "dark_5",
                "electric_1a", "electric_1b", "electric_2a", "electric_2b",
                "electric_3a", "electric_3b");
    }

    private static List<Pokemon> team(String context, PokemonSpec... specs) {
        List<Pokemon> team = new ArrayList<>(specs.length);
        for (PokemonSpec spec : specs) {
            Pokemon pokemon = createPokemon(spec, context);
            if (pokemon == null) return List.of();
            team.add(pokemon);
        }
        return List.copyOf(team);
    }

    private static Pokemon createPokemon(PokemonSpec spec, String context) {
        String heldItem = availableItem(spec.heldItem(), context);
        try {
            boolean fixedMoves = context.equals("dark_3") || context.equals("dark_5");
            Pokemon pokemon = GymTeamPokemon.create(spec.species(), spec.gender(), spec.level(),
                    spec.nature(), spec.ability(), fixedMoves ? spec.moves() : Set.of(),
                    UNTRAINED_STATS, UNTRAINED_STATS, heldItem, spec.aspects(), context);
            if (!fixedMoves) {
                LevelUpMovesets.apply(pokemon, context + "/" + spec.species());
            }
            pokemon.heal();
            return pokemon;
        } catch (RuntimeException exception) {
            GymCatalog.LOG.error("[GymMinionTeam] Invalid Pokemon {} for {}",
                    spec.species(), context, exception);
            return null;
        }
    }

    private static String availableItem(String configured, String context) {
        if (configured == null || configured.isBlank()) return "";
        ResourceLocation id = configured.contains(":")
                ? ResourceLocation.tryParse(configured)
                : ResourceLocation.fromNamespaceAndPath("cobblemon", configured);
        if (id != null && BuiltInRegistries.ITEM.containsKey(id)) return id.toString();
        GymCatalog.LOG.warn("[GymMinionTeam] Skipping unavailable held item {} for {}",
                configured, context);
        return "";
    }

    private static PokemonSpec p(String species, int level, String nature, String ability,
                                 String heldItem, String... moves) {
        return f(species, level, nature, ability, heldItem, "MALE", Set.of(), moves);
    }

    private static PokemonSpec f(String species, int level, String nature, String ability,
                                 String heldItem, String gender, Set<String> aspects,
                                 String... moves) {
        return new PokemonSpec(species, gender, level, nature, ability, heldItem,
                new LinkedHashSet<>(Arrays.asList(moves)), aspects);
    }

    private record PokemonSpec(String species, String gender, int level, String nature,
                               String ability, String heldItem, Set<String> moves,
                               Set<String> aspects) {
    }
}
