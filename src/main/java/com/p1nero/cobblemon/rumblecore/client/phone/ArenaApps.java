package com.p1nero.cobblemon.rumblecore.client.phone;

import com.nbp.cobblemon_smartphone.api.SmartphoneAction;
import com.nbp.cobblemon_smartphone.api.SmartphoneActionRegistry;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.client.combat.ServerHallScreen;
import com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierRooms;
import com.p1nero.cobblemon.rumblecore.phone.PhoneApp;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ArenaApps {
    private ArenaApps() {}

    @SubscribeEvent
    public static void register(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (var app : new App[]{new App(PhoneApp.SERVER_HALL, "trainer"), new App(PhoneApp.FRONTIER, "wireless")}) {
                if (app.app.available()) SmartphoneActionRegistry.INSTANCE.register(app);
            }
        });
    }

    private record App(PhoneApp app, String icon) implements SmartphoneAction {
        @Override public String getId() { return app.actionId(); }
        @Override public Component getDisplayName() { return app.title(); }
        @Override public ResourceLocation getTexture() { return texture(""); }
        @Override public ResourceLocation getHoverTexture() { return texture("_hover"); }
        private ResourceLocation texture(String suffix) {
            return ResourceLocation.fromNamespaceAndPath("cobblemon_smartphone", "textures/gui/buttons/" + icon + suffix + ".png");
        }
        @Override public boolean isEnabled() {
            return app.available() && ClientPhoneState.unlocked(Minecraft.getInstance().player, app);
        }
        @Override public void onClick() {
            if (!isEnabled()) return;
            var mc = Minecraft.getInstance();
            if (app == PhoneApp.SERVER_HALL) mc.setScreen(new ServerHallScreen(mc.screen));
            else {
                var command = new CompoundTag();
                command.putString("action", "open_phone");
                PacketDistributor.sendToServer(new FrontierRooms.Command(command));
            }
        }
    }
}
