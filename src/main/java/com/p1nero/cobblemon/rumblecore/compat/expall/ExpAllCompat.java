package com.p1nero.cobblemon.rumblecore.compat.expall;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** Optional bridge to Exp All's own equipment resolver, player toggle and live server config. */
public final class ExpAllCompat {
    private record Access(Method equipped, Method enabled, Method config, Field multiplier) {}
    private static final class Lazy {
        static final Access ACCESS = load();
    }
    private static boolean failed;

    private ExpAllCompat() {}

    private static Access load() {
        if (!ModList.get().isLoaded("cobblemonexpall")) return null;
        try {
            var prefix = "net.axelwf.cobblemonexpall.";
            var equip = Class.forName(prefix + "service.ExpAllEquipResolver");
            var toggle = Class.forName(prefix + "service.ExpAllToggleService");
            var config = Class.forName(prefix + "config.ExpAllConfigManager").getMethod("get");
            return new Access(equip.getMethod("isExpAllEquipped", ServerPlayer.class),
                    toggle.getMethod("isExperienceSharingEnabled", ServerPlayer.class), config,
                    config.getReturnType().getField("shareMultiplier"));
        } catch (ReflectiveOperationException | LinkageError e) {
            LogUtils.getLogger().warn("Cobblemon Exp All real-time experience API unavailable", e);
            return null;
        }
    }

    public static double multiplier(ServerPlayer player) {
        var access = Lazy.ACCESS;
        if (access == null || failed) return 0;
        try {
            if (!(boolean) access.enabled.invoke(null, player) || !(boolean) access.equipped.invoke(null, player)) return 0;
            return access.multiplier.getDouble(access.config.invoke(null));
        } catch (ReflectiveOperationException | LinkageError e) {
            failed = true;
            LogUtils.getLogger().warn("Could not query Cobblemon Exp All for real-time experience", e);
            return 0;
        }
    }
}
