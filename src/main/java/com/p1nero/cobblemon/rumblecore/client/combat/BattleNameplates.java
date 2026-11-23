package com.p1nero.cobblemon.rumblecore.client.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.johnn.realtimecobblemon.client.ClientSmash;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaPackets;
import com.p1nero.misc.custom_instances.instance.InstanceManager;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import org.joml.Matrix4f;

/** Suppress world labels; Realtime's dedicated battle HUD remains the source of battle status. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class BattleNameplates {
    private static ArenaPackets.Allies allies;

    private BattleNameplates() {}

    public static void sync(ArenaPackets.Allies update) { allies = update; }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { allies = null; }

    private static boolean sameArena(Entity entity) {
        return allies != null && entity.level().dimension().location().equals(allies.dimension());
    }

    public static boolean allied(Entity entity) {
        if (!sameArena(entity)) return false;
        int id = entity.getId();
        return allies.actors().contains(id) || allies.players().stream().anyMatch(p -> p.playerEntity() == id || p.body() == id);
    }

    public static boolean hidden(PokemonEntity pokemon) {
        return InstanceManager.isInstanceDimension(pokemon.level().dimension())
                || ClientSmash.active() && Objects.equals(ClientSmash.controlledEntity(), pokemon.getId());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void vanillaLabel(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof PokemonEntity pokemon) || !hidden(pokemon)) return;
        event.setCanRender(TriState.FALSE);
        var mc = Minecraft.getInstance();
        var local = mc.player;
        if (local == null || !sameArena(pokemon) || !pokemon.isAlive() || pokemon.isInvisibleTo(local)) return;
        var controller = allies.players().stream().filter(p -> p.body() == pokemon.getId()).findFirst().orElse(null);
        if (controller == null || controller.player().equals(local.getUUID()) || local.distanceToSqr(pokemon) > 48 * 48) return;

        var connection = mc.getConnection();
        var info = connection == null ? null : connection.getPlayerInfo(controller.player());
        var skin = info == null ? DefaultPlayerSkin.get(controller.player()) : info.getSkin();
        var pose = event.getPoseStack();
        pose.pushPose();
        try {
            pose.translate(0, pokemon.getBbHeight() + .4, 0);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            pose.scale(.025F, -.025F, .025F);
            var matrix = pose.last().pose();
            var buffers = event.getMultiBufferSource();
            float left = -(mc.font.width(controller.name()) + 13) / 2F;
            var head = buffers.getBuffer(RenderType.text(skin.texture()));
            face(head, matrix, left, 0, 8, 8);
            face(head, matrix, left, .01F, 40, 8);
            mc.font.drawInBatch(controller.name(), left + 13, 0, 0xffe4f3ff, false, matrix, buffers,
                    Font.DisplayMode.NORMAL, 0xaa17191d, LightTexture.FULL_BRIGHT);
        } finally {
            pose.popPose();
        }
    }

    private static void face(VertexConsumer out, Matrix4f pose, float x, float z, int u, int v) {
        vertex(out, pose, x, 9, z, u, v + 8);
        vertex(out, pose, x + 10, 9, z, u + 8, v + 8);
        vertex(out, pose, x + 10, -1, z, u + 8, v);
        vertex(out, pose, x, -1, z, u, v);
    }

    private static void vertex(VertexConsumer out, Matrix4f pose, float x, float y, float z, int u, int v) {
        out.addVertex(pose, x, y, z).setColor(0xffffffff).setUv(u / 64F, v / 64F).setLight(LightTexture.FULL_BRIGHT);
    }
}
