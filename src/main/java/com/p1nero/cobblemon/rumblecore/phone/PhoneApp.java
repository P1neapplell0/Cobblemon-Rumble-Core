package com.p1nero.cobblemon.rumblecore.phone;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import java.util.Arrays;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/** Stable persisted card names; the bit positions are only used by the current wire protocol. */
public enum PhoneApp {
    NATURE_COMPASS("nature_compass", "gps", null),
    STRUCTURE_COMPASS("structure_compass", "structure_compass", null),
    WAYSTONE("waystone", "waystones_warp_stone", "waystones"),
    HEALING("healing", "heal", null),
    PC("pc", "pc", null),
    CLOUD("cloud", "cloud", null),
    POKEDEX("pokedex", "pokedex", null),
    POKEMON_TRACKER("pokemon_tracker", "cobblenav_pokenav", "cobblenav"),
    FISHING_NAVIGATOR("fishing_navigator", "cobblenav_fishingnav", "cobblenav"),
    ULTRA_SPACE("ultra_space", null, "pokemon_ultra_space"),
    MINE_CHAT("mine_chat", CobblemonRumbleCore.MOD_ID + ":mine_chat", "mine_chat");

    private final String key;
    private final String actionId;
    private final String dependency;

    PhoneApp(String key, String action, String dependency) {
        this.key = key;
        this.actionId = action == null || action.contains(":") ? action : "cobblemon_smartphone:" + action;
        this.dependency = dependency;
    }

    public String key() { return key; }
    public String itemId() { return key + "_app_sd_card"; }
    public String actionId() { return actionId; }
    public int bit() { return 1 << ordinal(); }
    public Component title() { return Component.translatable("rumble.phone.app." + key); }
    public boolean available() {
        return actionId != null && ModList.get().isLoaded("cobblemon_smartphone")
                && (dependency == null || ModList.get().isLoaded(dependency));
    }
    public static PhoneApp forAction(String id) {
        return Arrays.stream(values()).filter(app -> id != null && id.equals(app.actionId)).findFirst().orElse(null);
    }
}
