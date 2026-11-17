package com.p1nero.cobblemon.rumblecore.client.phone;

import com.p1nero.cobblemon.rumblecore.phone.PhoneApp;
import java.lang.ref.WeakReference;
import net.minecraft.world.entity.player.Player;

public final class ClientPhoneState {
    private static WeakReference<Player> owner = new WeakReference<>(null);
    private static int mask;
    private ClientPhoneState() {}

    public static void update(Player player, int bits) {
        owner = new WeakReference<>(player);
        mask = bits;
    }

    public static boolean unlocked(Player player, PhoneApp app) {
        return player != null && owner.get() == player && (mask & app.bit()) != 0;
    }
}
