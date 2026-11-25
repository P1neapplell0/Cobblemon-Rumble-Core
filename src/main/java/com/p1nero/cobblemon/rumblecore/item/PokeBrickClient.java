package com.p1nero.cobblemon.rumblecore.item;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class PokeBrickClient {
    private PokeBrickClient() {}

    @SubscribeEvent
    public static void registerRenderer(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ThrownPokeBrick.TYPE.get(), ThrownItemRenderer::new);
    }
}
