package com.p1nero.cobblemon.rumblecore.client.combat;

import com.johnn.realtimecobblemon.client.ClientSmash;
import com.mojang.blaze3d.platform.InputConstants;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class RealtimeMoveKeys {
    private static final IKeyConflictContext REALTIME = new IKeyConflictContext() {
        @Override public boolean isActive() { return ClientSmash.active(); }
        @Override public boolean conflicts(IKeyConflictContext other) { return other == this; }
    };
    public static final KeyMapping[] SLOTS = new KeyMapping[4];
    static {
        int[] defaults = {GLFW.GLFW_KEY_Q, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C};
        for (int i = 0; i < SLOTS.length; i++) SLOTS[i] = new KeyMapping("key.cobblemon_rumble_core.move_" + (i + 1),
                REALTIME, InputConstants.Type.KEYSYM, defaults[i], "key.categories.cobblemon_rumble_core");
    }
    private RealtimeMoveKeys() {}
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) {
        for (var mapping : SLOTS) event.register(mapping);
    }
}
