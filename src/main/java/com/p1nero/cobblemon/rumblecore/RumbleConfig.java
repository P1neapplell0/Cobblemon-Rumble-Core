package com.p1nero.cobblemon.rumblecore;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class RumbleConfig {
    static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue SKIP_NPC_BATTLES;
    private static final ModConfigSpec.BooleanValue HARD_GYM_BATTLES;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Developer shortcuts. Disabled by default.").push("developerMode");
        SKIP_NPC_BATTLES = builder.comment("In a private singleplayer world, resolve NPC arena battles as wins once combat starts.")
                .define("skipNpcBattles", false);
        HARD_GYM_BATTLES = builder.comment("Use the higher level, six-Pokemon gym leader roster for leader challenges.")
                .define("hardGymBattles", true);
        builder.pop();
        SPEC = builder.build();
    }

    private RumbleConfig() {}

    public static boolean skipNpcBattles(MinecraftServer server) {
        return server.isSingleplayer() && !server.isPublished() && server.getPlayerList().getPlayerCount() == 1
                && SKIP_NPC_BATTLES.get();
    }

    public static boolean hardGymBattles() { return HARD_GYM_BATTLES.get(); }
}
