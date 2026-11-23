package com.p1nero.rumble.earlydisplay;

import java.util.Set;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper;

/** FML discovers this service even on dedicated servers, where LWJGL is absent. */
public final class RumbleWindowBootstrap implements GraphicsBootstrapper {
    static final String NAME = "rumbleearlywindow";
    private static final Set<String> CLIENT_TARGETS = Set.of("forgeclient", "forgeclientuserdev", "forgeclientdev");

    @Override public String name() { return NAME; }

    @Override public void bootstrap(String[] arguments) {
        if (!CLIENT_TARGETS.contains(launchTarget(arguments))) return;
        var key = FMLConfig.ConfigValue.EARLY_WINDOW_PROVIDER;
        if ("fmlearlywindow".equals(FMLConfig.getConfigValue(key)) && FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL)) {
            FMLConfig.updateConfig(key, NAME);
        }
    }

    private static String launchTarget(String[] arguments) {
        for (int i = 0; i < arguments.length; i++) {
            String argument = arguments[i];
            if (argument.startsWith("--launchTarget=")) return argument.substring("--launchTarget=".length());
            if (argument.equals("--launchTarget") && i + 1 < arguments.length) return arguments[i + 1];
        }
        return "";
    }
}
