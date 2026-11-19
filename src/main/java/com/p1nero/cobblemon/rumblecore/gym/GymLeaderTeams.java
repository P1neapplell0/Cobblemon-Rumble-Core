package com.p1nero.cobblemon.rumblecore.gym;

import com.cobblemon.mod.common.pokemon.Pokemon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Exact RCT-style competitive sets used by CIV's current Gym Leaders. */
public final class GymLeaderTeams {


    private GymLeaderTeams() {
    }

    /** Validates every configured species, move, ability and held item after data registries load. */
    public static boolean validateAll() {
        int[] normalSizes = {
                rock().size(), grass().size(), ghost().size(),
                ice().size(), ground().size(), fire().size(), dark().size(), electric().size()
        };
        int[] hardSizes = {
                hardRock().size(), hardGrass().size(), hardGhost().size(), hardIce().size(),
                hardGround().size(), hardFire().size(), hardDark().size(), hardElectric().size()
        };
        int[] expectedNormal = {3, 4, 4, 6, 6, 6, 6, 6};
        int[] expectedHard = {4, 5, 5, 6, 6, 6, 6, 6};
        for (int i = 0; i < expectedNormal.length; i++) {
            if (normalSizes[i] != expectedNormal[i] || hardSizes[i] != expectedHard[i]) {
                GymCatalog.LOG.error("[GymTeam] Validation failed at team index {} (normal {}/{}, hard {}/{})",
                        i, normalSizes[i], expectedNormal[i], hardSizes[i], expectedHard[i]);
                return false;
            }
        }
        GymCatalog.LOG.info("[GymTeam] Validated all normal and hard Gym Leader teams (85 Pokemon)");
        return true;
    }

    public static List<Pokemon> rock() {
        return team("Rock Gym Leader",
                pokemon("rockruff", 16, "adamant", "vitalspirit", "muscle_band",
                        moves("tackle", "leer", "sandattack", "rockthrow"),
                        stats(31, 31, 0, 0, 0, 31), stats(0, 252, 0, 0, 0, 252)),
                pokemon("onix", 16, "adamant", "sturdy", "leftovers",
                        moves("rockthrow", "dragonbreath", "rockpolish", "harden"),
                        stats(31, 31, 31, 0, 0, 0), stats(252, 252, 0, 0, 0, 0)),
                pokemon("minior", 16, null, "shieldsdown", "focus_sash",
                        moves("rollout", "swift", "confuseray", "tackle"),
                        stats(0, 31, 0, 31, 0, 31), stats(0, 252, 0, 252, 0, 0)));
    }

    public static List<Pokemon> grass() {
        return team("Grass Gym Leader",
                pokemon("roselia", 23, "timid", "poisonpoint", "wise_glasses",
                        moves("toxicspikes", "magicalleaf", "leechseed", "gigadrain"),
                        stats(31, 0, 0, 31, 0, 31), stats(0, 0, 0, 252, 0, 252)),
                pokemon("shroomish", 23, "bold", "effectspore", "sitrus_berry",
                        moves("leechseed", "gigadrain", "poisonpowder", "stunspore"),
                        stats(31, 0, 31, 0, 31, 0), stats(252, 0, 0, 252, 0, 0)),
                pokemon("weepinbell", 23, "hardy", "gluttony", "aguav_berry",
                        moves("vinewhip", "acid", "stunspore", "poisonpowder"),
                        stats(31, 31, 0, 31, 0, 0), stats(252, 252, 0, 0, 0, 0)),
                pokemon("electrode", 23, "modest", "aftermath", "rocky_helmet",
                        moves("chargebeam", "bulletseed", "rollout", "stunspore"),
                        stats(31, 0, 31, 31, 0, 0), stats(252, 0, 0, 252, 0, 0), "hisuian"));
    }

    public static List<Pokemon> ghost() {
        return team("Ghost Gym Leader",
                pokemon("haunter", 27, "timid", "levitate", "spell_tag",
                        moves("hex", "venoshock", "confuseray", "hypnosis"),
                        stats(31, 0, 0, 31, 31, 31), stats(4, 0, 0, 252, 0, 252)),
                pokemon("misdreavus", 26, "timid", "levitate", "eviolite",
                        moves("hex", "painsplit", "meanlook", "thunderwave"),
                        stats(31, 0, 31, 31, 31, 31), stats(4, 0, 0, 252, 0, 252)),
                pokemon("honedge", 26, "brave", "noguard", "eviolite",
                        moves("sacredsword", "shadowclaw", "shadowsneak", "swordsdance"),
                        stats(31, 31, 31, 0, 31, 0), stats(252, 252, 4, 0, 0, 0)),
                pokemon("phantump", 27, "adamant", "naturalcure", "miracle_seed",
                        moves("hornleech", "leechseed", "shadowsneak", "willowisp"),
                        stats(31, 31, 31, 0, 31, 0), stats(252, 252, 4, 0, 0, 0)));
    }

    public static List<Pokemon> ice() {
        return team("Ice Gym Leader",
                pokemon("delibird", 36, "jolly", "hustle", "focus_sash",
                        moves("icepunch", "aerialace", "spikes", "uturn"),
                        stats(31, 31, 31, 0, 31, 31), stats(4, 252, 0, 0, 0, 252)),
                pokemon("glaceon", 36, "modest", "snowcloak", "never_melt_ice",
                        moves("icebeam", "shadowball", "aurorabeam", "iceshard"),
                        stats(31, 0, 31, 31, 31, 31), stats(4, 0, 0, 252, 0, 252)),
                pokemon("ninetales", 36, "timid", "snowwarning", "light_clay",
                        moves("auroraveil", "blizzard", "dazzlinggleam", "freezedry"),
                        stats(31, 0, 0, 31, 31, 31), stats(4, 0, 0, 252, 0, 252), "alolan"),
                pokemon("lapras", 36, "bold", "waterabsorb", "leftovers",
                        moves("surf", "icebeam", "confuseray", "perishsong"),
                        stats(31, 0, 31, 31, 31, 0), stats(252, 0, 252, 4, 0, 0)),
                pokemon("empoleon", 36, "modest", "torrent", "mystic_water",
                        moves("bubblebeam", "steelwing", "flashcannon", "drillpeck"),
                        stats(31, 0, 31, 31, 31, 31), stats(252, 0, 0, 252, 4, 0)),
                pokemon("spheal", 36, "calm", "thickfat", "eviolite",
                        moves("icebeam", "waterpulse", "rest", "snore"),
                        stats(31, 0, 31, 31, 31, 0), stats(252, 0, 0, 4, 252, 0)));
    }

    public static List<Pokemon> ground() {
        return team("Ground Gym Leader",
                pokemon("hippowdon", 42, "adamant", "sandstream", "smooth_rock",
                        moves("earthquake", "firefang", "thunderfang", "crunch"),
                        stats(31, 31, 31, 0, 0, 31), stats(252, 252, 0, 0, 0, 0)),
                pokemon("nidoking", "MALE", 42, "jolly", "poisonpoint", "focus_sash",
                        moves("toxicspikes", "stealthrock", "icepunch", "thunderpunch"),
                        stats(31, 0, 31, 0, 0, 31), stats(252, 0, 0, 0, 0, 252)),
                pokemon("krookodile", 42, "jolly", "intimidate", "muscle_band",
                        moves("earthquake", "crunch", "thunderfang", "rockslide"),
                        stats(31, 31, 0, 0, 31, 31), stats(0, 252, 0, 0, 0, 252)),
                pokemon("steelix", 42, "adamant", "sturdy", "mega_showdown:steelixite",
                        moves("ironhead", "icefang", "earthquake", "rockslide"),
                        stats(31, 31, 31, 0, 31, 0), stats(252, 252, 0, 0, 0, 0)),
                pokemon("marowak", 42, null, "battlearmor", "sitrus_berry",
                        moves("bonemerang", "firepunch", "thunderpunch", "rockslide"),
                        stats(31, 31, 31, 0, 31, 0), stats(252, 252, 0, 0, 0, 0)),
                pokemon("excadrill", 42, "adamant", "sandrush", "life_orb",
                        moves("swordsdance", "highhorsepower", "ironhead", "rockslide"),
                        stats(31, 31, 31, 0, 0, 31), stats(252, 252, 0, 0, 0, 0)));
    }

    public static List<Pokemon> fire() {
        return team("Fire Gym Leader",
                pokemon("incineroar", 50, "careful", "intimidate", "assault_vest",
                        moves("fakeout", "flareblitz", "knockoff", "uturn"),
                        stats(31, 31, 31, 0, 31, 31), stats(252, 4, 0, 0, 252, 0)),
                pokemon("ninetales", 48, "timid", "drought", "heat_rock",
                        moves("flamethrower", "solarbeam", "darkpulse", "willowisp"),
                        stats(31, 0, 0, 31, 31, 31), stats(4, 0, 0, 252, 0, 252)),
                pokemon("flareon", 49, "adamant", "guts", "toxic_orb",
                        moves("flareblitz", "superpower", "crunch", "quickattack"),
                        stats(31, 31, 31, 0, 31, 31), stats(4, 252, 0, 0, 0, 252)),
                pokemon("ceruledge", 47, "jolly", "flashfire", "life_orb",
                        moves("bitterblade", "shadowclaw", "swordsdance", "shadowsneak"),
                        stats(31, 31, 31, 0, 31, 31), stats(4, 252, 0, 0, 0, 252)),
                pokemon("rapidash", 48, "jolly", "flashfire", "expert_belt",
                        moves("flareblitz", "wildcharge", "megahorn", "hypnosis"),
                        stats(31, 31, 31, 0, 31, 31), stats(4, 252, 0, 0, 0, 252)),
                pokemon("charizard", 50, "jolly", "blaze", "mega_showdown:charizardite_x",
                        moves("dragondance", "flareblitz", "dragonclaw", "earthquake"),
                        stats(31, 31, 31, 0, 31, 31), stats(4, 252, 0, 0, 0, 252)));
    }

    public static List<Pokemon> dark() {
        return team("Dark Gym Leader",
                pokemon("zoroark", 57, "timid", "illusion", "wise_glasses",
                        moves("nightdaze", "sludgebomb", "flamethrower", "uturn"),
                        stats(0, 31, 0, 31, 0, 31), stats(0, 0, 0, 252, 0, 252)),
                pokemon("spiritomb", 57, "brave", "pressure", "leftovers",
                        moves("protect", "shadowsneak", "smokescreen", "toxic"),
                        stats(31, 0, 31, 0, 31, 0), stats(252, 0, 0, 0, 252, 0)),
                pokemon("umbreon", 57, "bold", "synchronize", "sitrus_berry",
                        moves("protect", "snarl", "lightscreen", "toxic"),
                        stats(31, 0, 31, 31, 31, 0), stats(252, 0, 252, 0, 4, 0)),
                pokemon("hydreigon", 57, "timid", "levitate", "life_orb",
                        moves("darkpulse", "flamethrower", "dragonbreath", "earthpower"),
                        stats(31, 0, 31, 31, 0, 31), stats(0, 0, 4, 252, 0, 252)),
                pokemon("roaringmoon", 57, "jolly", "protosynthesis",
                        "mega_showdown:booster_energy",
                        moves("earthquake", "throatchop", "outrage", "dragondance"),
                        stats(31, 31, 31, 0, 0, 31), stats(0, 252, 4, 0, 0, 252)),
                pokemon("absol", 57, "jolly", "superluck", "mega_showdown:absolite",
                        moves("throatchop", "shadowclaw", "closecombat", "playrough"),
                        stats(31, 31, 31, 0, 0, 31), stats(0, 252, 0, 0, 0, 252)));
    }

    public static List<Pokemon> electric() {
        return team("Electric Gym Leader",
                pokemon("rotom", "GENDERLESS", 63, "bold", "levitate", "leftovers",
                        moves("hydropump", "voltswitch", "willowisp", "painsplit"),
                        stats(31, 0, 31, 31, 31, 31), stats(252, 0, 252, 4, 0, 0),
                        "wash-appliance"),
                pokemon("luxray", 63, "adamant", "intimidate", "life_orb",
                        moves("wildcharge", "crunch", "icefang", "closecombat"),
                        stats(31, 31, 31, 0, 31, 31), stats(4, 252, 0, 0, 0, 252)),
                pokemon("pikachu", 63, "naive", "static", "light_ball",
                        moves("volttackle", "fakeout", "icepunch", "voltswitch"),
                        stats(31, 31, 0, 31, 0, 31), stats(0, 252, 0, 4, 0, 252)),
                pokemon("jolteon", 63, "timid", "voltabsorb", "focus_sash",
                        moves("thunderbolt", "shadowball", "voltswitch", "hypervoice"),
                        stats(31, 0, 0, 31, 31, 31), stats(4, 0, 0, 252, 0, 252)),
                pokemon("eelektross", 63, "quiet", "levitate", "assault_vest",
                        moves("thunderbolt", "flamethrower", "gigadrain", "icebeam"),
                        stats(31, 31, 31, 31, 31, 0), stats(252, 4, 0, 252, 0, 0)),
                pokemon("ampharos", 63, "modest", "static", "mega_showdown:ampharosite",
                        moves("dragonpulse", "voltswitch", "focusblast", "cottonguard"),
                        stats(31, 0, 31, 31, 31, 31), stats(252, 0, 0, 252, 4, 0)));
    }

    public static List<Pokemon> hardRock() {
        return hardTeam("Hard Rock Gym Leader",
                hardPokemon("archen", 17, "adamant", "defeatist", "sitrus_berry",
                        moves("leer", "quickattack", "rockthrow", "pluck"),
                        stats(0, 88, 0, 0, 0, 88)),
                hardPokemon("nosepass", 17, "sassy", "sturdy", "leftovers",
                        moves("protect", "rockthrow", "thundershock", "thunderwave"),
                        stats(88, 0, 88, 0, 0, 0)),
                hardPokemon("minior", 17, "modest", "shieldsdown", "rocky_helmet",
                        moves("ancientpower", "defensecurl", "confuseray", "swift"),
                        stats(88, 0, 0, 88, 0, 0)),
                hardPokemon("lileep", 17, "bold", "stormdrain", null,
                        moves("confuseray", "gigadrain", "ingrain", "acid"),
                        stats(88, 0, 0, 88, 0, 0)));
    }

    public static List<Pokemon> hardGrass() {
        return hardTeam("Hard Grass Gym Leader",
                hardPokemon("electrode", 25, "modest", "aftermath", "rocky_helmet",
                        moves("chloroblast", "chargebeam", "thunderwave", "worryseed"),
                        stats(0, 0, 0, 168, 0, 88), "hisuian"),
                hardPokemon("sunflora", 24, "modest", "solarpower", "leftovers",
                        moves("ingrain", "gigadrain", "leechseed", "growth"),
                        stats(168, 0, 88, 0, 0, 0)),
                hardPokemon("ludicolo", 25, "timid", "raindish", "assault_vest",
                        moves("bubblebeam", "gigadrain", "icywind", "mudshot"),
                        stats(168, 0, 0, 88, 0, 0)),
                hardPokemon("victreebel", 25, "hardy", "gluttony", "figy_berry",
                        moves("acid", "leaftornado", "stunspore", "toxic"),
                        stats(168, 0, 0, 88, 0, 0)),
                hardPokemon("lilligant", 26, "timid", "chlorophyll", "life_orb",
                        moves("quiverdance", "gigadrain", "petaldance", "charm"),
                        stats(0, 0, 0, 88, 0, 168)));
    }

    public static List<Pokemon> hardGhost() {
        return hardTeam("Hard Ghost Gym Leader",
                hardPokemon("gengar", 30, "modest", "cursedbody", "focus_sash",
                        moves("toxicspikes", "toxic", "hex", "icywind"), specialAttackSpeedEvs()),
                hardPokemon("spiritomb", 28, "bold", "infiltrator", "leftovers",
                        moves("toxic", "willowisp", "snarl", "icywind"), hpSpecialAttackEvs()),
                hardPokemon("mismagius", 30, "timid", "levitate", "life_orb",
                        moves("hex", "psybeam", "drainingkiss", "mysticalfire"), specialAttackSpeedEvs()),
                hardPokemon("drifblim", 28, "bold", "aftermath", "rocky_helmet",
                        moves("shadowball", "airslash", "hex", "psybeam"), hpDefenceEvs()),
                hardPokemon("mimikyu", 29, "adamant", "disguise", "red_card",
                        moves("babydolleyes", "doubleteam", "shadowsneak", "playrough"), hpAttackEvs()));
    }

    public static List<Pokemon> hardIce() {
        return hardTeam("Hard Ice Gym Leader",
                hardPokemon("ninetales", 38, "modest", "snowwarning", "icy_rock",
                        moves("auroraveil", "dazzlinggleam", "icebeam", "extrasensory"), specialAttackSpeedEvs(), "alolan"),
                hardPokemon("cetitan", 39, "adamant", "slushrush", "life_orb",
                        moves("earthquake", "iciclecrash", "rockslide", "knockoff"), hpAttackEvs()),
                hardPokemon("sandslash", 38, "adamant", "slushrush", "assault_vest",
                        moves("earthquake", "iciclespear", "rockslide", "metalclaw"), attackSpeedEvs(), "alolan"),
                hardPokemon("glaceon", 38, "calm", "snowcloak", "sitrus_berry",
                        moves("babydolleyes", "sandattack", "icebeam", "mudshot"), hpSpecialAttackEvs()),
                hardPokemon("lapras", 38, "modest", "waterabsorb", "leftovers",
                        moves("whirlpool", "icebeam", "chillingwater", "lifedew"), hpSpecialAttackEvs()),
                hardPokemon("spheal", 36, "relaxed", "oblivious", "focus_sash",
                        moves("defensecurl", "rest", "snore", "growl"), hpDefenceEvs()));
    }

    public static List<Pokemon> hardGround() {
        return hardTeam("Hard Ground Gym Leader",
                hardPokemon("hippowdon", 45, "impish", "sandstream", "smooth_rock",
                        moves("stealthrock", "earthquake", "crunch", "stoneedge"),
                        stats(252, 0, 88, 0, 168, 0)),
                hardPokemon("nidoking", "MALE", 45, "modest", "poisonpoint", "choice_scarf",
                        moves("earthpower", "thunderbolt", "icebeam", "flamethrower"),
                        stats(252, 0, 88, 0, 168, 0)),
                hardPokemon("garchomp", 48, "jolly", "sandveil", "life_orb",
                        moves("earthquake", "stoneedge", "outrage", "crunch"),
                        stats(88, 168, 0, 0, 40, 214)),
                hardPokemon("excadrill", 48, "jolly", "sandrush", "mega_showdown:excadrite",
                        moves("earthquake", "ironhead", "rapidspin", "poisonjab"),
                        stats(168, 252, 0, 0, 0, 88)),
                hardPokemon("krookodile", 46, "jolly", "intimidate", "focus_sash",
                        moves("earthquake", "crunch", "stoneedge", "gunkshot"), attackSpeedEvs()),
                hardPokemon("golem", 46, "adamant", "sturdy", "choice_band",
                        moves("explosion"), hpAttackEvs()));
    }

    public static List<Pokemon> hardFire() {
        return hardTeam("Hard Fire Gym Leader",
                hardPokemon("ninetales", 52, "timid", "drought", "heat_rock",
                        moves("willowisp", "solarbeam", "extrasensory", "fireblast"), specialAttackSpeedEvs()),
                hardPokemon("darmanitan", 52, "adamant", "sheerforce", "choice_scarf",
                        moves("flareblitz", "earthquake", "stoneedge", "superpower"), attackSpeedEvs()),
                hardPokemon("charizard", 54, "modest", "solarpower", "mega_showdown:charizardite_y",
                        moves("fireblast", "solarbeam", "dragonpulse", "focusblast"), specialAttackSpeedEvs()),
                hardPokemon("gougingfire", "GENDERLESS", 54, "adamant", "protosynthesis",
                        "heavy_duty_boots",
                        moves("burningbulwark", "ragingfury", "outrage", "dragondance"),
                        stats(252, 168, 0, 0, 88, 0)),
                hardPokemon("arcanine", 52, "jolly", "intimidate", "choice_band",
                        moves("flareblitz", "playrough", "closecombat", "extremespeed"),
                        stats(252, 0, 168, 0, 88, 0)),
                hardPokemon("ceruledge", 52, "adamant", "weakarmor", "shuca_berry",
                        moves("flareblitz", "solarblade", "poltergeist", "closecombat"),
                        stats(168, 252, 88, 0, 0, 0)));
    }

    public static List<Pokemon> hardDark() {
        return hardTeam("Hard Dark Gym Leader",
                hardPokemon("meowscarada", 60, "jolly", "protean", "focus_sash",
                        moves("spikes", "flowertrick", "knockoff", "playrough"), attackSpeedEvs()),
                hardPokemon("spiritomb", 60, "relaxed", "pressure", "roseli_berry",
                        moves("toxic", "protect", "suckerpunch", "confuseray"),
                        stats(252, 0, 88, 0, 168, 0)),
                hardPokemon("roaringmoon", "GENDERLESS", 62, "jolly", "protosynthesis",
                        "mega_showdown:booster_energy",
                        moves("outrage", "knockoff", "ironhead", "tailwind"),
                        stats(0, 168, 88, 0, 0, 252)),
                hardPokemon("gyarados", 65, "adamant", "intimidate", "mega_showdown:gyaradosite",
                        moves("waterfall", "earthquake", "ironhead", "outrage"),
                        stats(252, 252, 4, 0, 0, 0)),
                hardPokemon("umbreon", 60, "bold", "synchronize", "sitrus_berry",
                        moves("toxic", "babydolleyes", "snarl", "protect"),
                        stats(252, 0, 168, 0, 88, 0)),
                hardPokemon("obstagoon", 62, "adamant", "guts", "flame_orb",
                        moves("obstruct", "facade", "suckerpunch", "gunkshot"), hpAttackEvs()));
    }

    public static List<Pokemon> hardElectric() {
        return hardTeam("Hard Electric Gym Leader",
                hardPokemon("magnezone", "GENDERLESS", 66, "modest", "sturdy", "rocky_helmet",
                        moves("lightscreen", "thunderwave", "voltswitch", "flashcannon"), hpDefenceEvs()),
                hardPokemon("rotom", "GENDERLESS", 66, "modest", "levitate", "leftovers",
                        moves("thunderbolt", "thunderwave", "reflect", "shadowball"), hpSpecialAttackEvs(), "wash-appliance"),
                hardPokemon("luxray", 66, "jolly", "intimidate", "choice_scarf",
                        moves("crunch", "supercellslam", "playrough", "icefang"), hpAttackEvs()),
                hardPokemon("eelektross", 66, "jolly", "levitate", "sitrus_berry",
                        moves("wildcharge", "knockoff", "closecombat", "outrage"), hpAttackEvs()),
                hardPokemon("ragingbolt", "GENDERLESS", 68, "timid", "protosynthesis", "mega_showdown:booster_energy",
                        moves("thunder", "hypervoice", "dragonpulse", "thunderclap"), specialAttackSpeedEvs()),
                hardPokemon("manectric", 68, "modest", "lightningrod", "mega_showdown:manectite",
                        moves("flamethrower", "thunder", "hypervoice", "voltswitch"), specialAttackSpeedEvs()));
    }

    private static List<Pokemon> hardTeam(String context, PokemonSpec... specs) {
        return team(context, specs);
    }

    private static PokemonSpec hardPokemon(String species, int level, String nature, String ability,
                                           String heldItem, Set<String> moves,
                                           GymTeamPokemon.TrainingStats evs, String... aspects) {
        return hardPokemon(species, "MALE", level, nature, ability, heldItem, moves, evs, aspects);
    }

    private static PokemonSpec hardPokemon(String species, String gender, int level, String nature,
                                           String ability, String heldItem, Set<String> moves,
                                           GymTeamPokemon.TrainingStats evs, String... aspects) {
        return pokemon(species, gender, level, nature, ability, heldItem, moves,
                stats(31, 31, 31, 31, 31, 31), evs, aspects);
    }

    private static GymTeamPokemon.TrainingStats hpAttackEvs() {
        return stats(252, 252, 0, 0, 0, 0);
    }

    private static GymTeamPokemon.TrainingStats attackSpeedEvs() {
        return stats(0, 252, 0, 0, 0, 252);
    }

    private static GymTeamPokemon.TrainingStats hpDefenceEvs() {
        return stats(252, 0, 252, 0, 0, 0);
    }

    private static GymTeamPokemon.TrainingStats hpSpecialAttackEvs() {
        return stats(252, 0, 0, 252, 0, 0);
    }

    private static GymTeamPokemon.TrainingStats specialAttackSpeedEvs() {
        return stats(0, 0, 0, 252, 0, 252);
    }

    private static List<Pokemon> team(String context, PokemonSpec... specs) {
        List<Pokemon> result = new ArrayList<>(specs.length);
        for (PokemonSpec spec : specs) {
            Pokemon pokemon = create(spec, context);
            if (pokemon == null) return List.of();
            result.add(pokemon);
        }
        return result;
    }

    private static Pokemon create(PokemonSpec spec, String context) {
        try {
            boolean fixedMoves = context.startsWith("Hard ") || Set.of(
                    "Rock Gym Leader", "Grass Gym Leader", "Ground Gym Leader",
                    "Dark Gym Leader").contains(context);
            Pokemon pokemon = GymTeamPokemon.create(spec.species(), spec.gender(), spec.level(),
                    spec.nature(), spec.ability(), fixedMoves ? spec.moves() : Set.of(),
                    spec.ivs(), spec.evs(), spec.heldItem(), spec.aspects(), context);
            if (!fixedMoves) {
                LevelUpMovesets.apply(pokemon, context + "/" + spec.species());
            }
            pokemon.heal();
            return pokemon;
        } catch (RuntimeException exception) {
            GymCatalog.LOG.error("[GymTeam] Invalid {} set for {}", context, spec.species(),
                    exception);
            return null;
        }
    }

    private static PokemonSpec pokemon(String species, int level, String nature, String ability,
                                       String heldItem, Set<String> moves,
                                       GymTeamPokemon.TrainingStats ivs,
                                       GymTeamPokemon.TrainingStats evs, String... aspects) {
        return pokemon(species, "MALE", level, nature, ability, heldItem, moves, ivs, evs,
                aspects);
    }

    private static PokemonSpec pokemon(String species, String gender, int level, String nature,
                                       String ability, String heldItem, Set<String> moves,
                                       GymTeamPokemon.TrainingStats ivs,
                                       GymTeamPokemon.TrainingStats evs, String... aspects) {
        return new PokemonSpec(species, gender, level, nature, ability, heldItem, moves, ivs, evs,
                Set.of(aspects));
    }

    private static Set<String> moves(String... moves) {
        return new LinkedHashSet<>(List.of(moves));
    }

    private static GymTeamPokemon.TrainingStats stats(int hp, int attack, int defence,
                                                 int specialAttack, int specialDefence, int speed) {
        return new GymTeamPokemon.TrainingStats(
                hp, attack, defence, specialAttack, specialDefence, speed);
    }

    private record PokemonSpec(String species, String gender, int level, String nature,
                               String ability, String heldItem, Set<String> moves,
                               GymTeamPokemon.TrainingStats ivs, GymTeamPokemon.TrainingStats evs,
                               Set<String> aspects) {
    }
}
