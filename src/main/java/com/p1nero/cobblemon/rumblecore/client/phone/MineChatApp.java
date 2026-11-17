package com.p1nero.cobblemon.rumblecore.client.phone;

import com.nbp.cobblemon_smartphone.api.SmartphoneAction;
import com.nbp.cobblemon_smartphone.api.SmartphoneActionRegistry;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.phone.PhoneApp;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.shelmarow.mine_chat.chat.screen.MineChatGlobeScreen;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class MineChatApp implements SmartphoneAction {
    @SubscribeEvent
    public static void register(FMLClientSetupEvent event) {
        if (PhoneApp.MINE_CHAT.available()) event.enqueueWork(() -> SmartphoneActionRegistry.INSTANCE.register(new MineChatApp()));
    }
    @Override public String getId() { return PhoneApp.MINE_CHAT.actionId(); }
    @Override public Component getDisplayName() { return PhoneApp.MINE_CHAT.title(); }
    @Override public ResourceLocation getTexture() { return ResourceLocation.parse("mine_chat:textures/mchat/chat.png"); }
    @Override public ResourceLocation getHoverTexture() { return ResourceLocation.fromNamespaceAndPath(CobblemonRumbleCore.MOD_ID, "textures/gui/smartphone/mine_chat_hover.png"); }
    @Override public void onClick() {
        var client = Minecraft.getInstance();
        if (ClientPhoneState.unlocked(client.player, PhoneApp.MINE_CHAT)) client.setScreen(new MineChatGlobeScreen());
    }
}
