package com.p1nero.cobblemon.rumblecore;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class RumbleConfig {
    static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue SKIP_NPC_BATTLES;
    private static final ModConfigSpec.BooleanValue HARD_GYM_BATTLES;
    private static final ModConfigSpec.DoubleValue IMPACT_SHAKE_MIN;
    private static final ModConfigSpec.DoubleValue IMPACT_SHAKE_DAMAGE;
    private static final ModConfigSpec.DoubleValue IMPACT_SHAKE_MAX;
    private static final ModConfigSpec.DoubleValue IMPACT_SHAKE_FREQUENCY;
    private static final ModConfigSpec.IntValue IMPACT_SHAKE_DURATION;

    private static final ModConfigSpec.BooleanValue BLOCK_VANILLA_AND_QUARK;
    private static final ModConfigSpec.BooleanValue DISABLE_BEEHIVES;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Developer shortcuts. Disabled by default.").push("developerMode");
        SKIP_NPC_BATTLES = builder.comment("In a private singleplayer world, resolve NPC arena battles as wins once combat starts.")
                .define("skipNpcBattles", false);
        HARD_GYM_BATTLES = builder.comment("Use the higher level, six-Pokemon gym leader roster for leader challenges.")
                .define("hardGymBattles", true);
        builder.pop();
        builder.comment("Realtime impact camera shake. Values are reloaded from the common config while testing.").push("realtimeImpactShake");
        IMPACT_SHAKE_MIN = builder.defineInRange("minimumIntensity", 0.70D, 0D, 20D);
        IMPACT_SHAKE_DAMAGE = builder.defineInRange("damageIntensity", 3.20D, 0D, 30D);
        IMPACT_SHAKE_MAX = builder.defineInRange("maximumIntensity", 4.80D, 0D, 50D);
        IMPACT_SHAKE_FREQUENCY = builder.defineInRange("frequency", 0.055D, 0.001D, 2D);
        IMPACT_SHAKE_DURATION = builder.defineInRange("durationMillis", 260, 20, 2000);
        builder.pop();
        builder.comment("Entity rules migrated from Ebony Phantom Core.").push("entityControl");
        BLOCK_VANILLA_AND_QUARK = builder.comment("Block vanilla and Quark living entities except players, villagers, armor stands and the Ender Dragon.")
                .define("blockVanillaAndQuark", true);
        DISABLE_BEEHIVES = builder.comment("Disable tree-grown bee nests and stored bee release from nests and hives.")
                .define("disableBeehives", true);
        builder.pop();
        SPEC = builder.build();
    }

    private RumbleConfig() {}

    public static boolean skipNpcBattles(MinecraftServer server) {
        return server.isSingleplayer() && !server.isPublished() && server.getPlayerList().getPlayerCount() == 1
                && SKIP_NPC_BATTLES.get();
    }

    public static boolean blocksVanillaAndQuark() { return BLOCK_VANILLA_AND_QUARK.get(); }
    public static boolean disablesBeehives() { return DISABLE_BEEHIVES.get(); }

    public static boolean hardGymBattles() { return HARD_GYM_BATTLES.get(); }
    public static float impactShakeMin() { return IMPACT_SHAKE_MIN.get().floatValue(); }
    public static float impactShakeDamage() { return IMPACT_SHAKE_DAMAGE.get().floatValue(); }
    public static float impactShakeMax() { return IMPACT_SHAKE_MAX.get().floatValue(); }
    public static float impactShakeFrequency() { return IMPACT_SHAKE_FREQUENCY.get().floatValue(); }
    public static int impactShakeDuration() { return IMPACT_SHAKE_DURATION.get(); }
}
