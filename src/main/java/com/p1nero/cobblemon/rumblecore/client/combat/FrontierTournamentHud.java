package com.p1nero.cobblemon.rumblecore.client.combat;

import com.mojang.blaze3d.platform.InputConstants;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierRooms;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.UUID;
import static com.p1nero.cobblemon.rumblecore.client.combat.FrontierConsole.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.arcadia.arc_quest.client.hud.style.OreUiRenderer;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class FrontierTournamentHud {
    private static final String BINDING = "key.cobblemon_rumble_core.frontier_bracket";
    private static final KeyMapping RESUME = new KeyMapping(BINDING, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R, "key.categories.cobblemon_rumble_core");
    private static CompoundTag room = new CompoundTag();
    private static UUID pinnedRoom;

    private FrontierTournamentHud() {}

    @EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Keys {
        @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) { event.register(RESUME); }
    }

    static void sync(CompoundTag data) {
        // A tournament round can close the UI before warping without removing room membership.
        if (data.getBoolean("close") && data.hasUUID("room")) return;
        room = data;
        if (!isPinned()) pinnedRoom = null;
    }

    static void pin(CompoundTag data) { if (data.hasUUID("room")) pinnedRoom = data.getUUID("room"); }
    static boolean isPinned() { return room.hasUUID("room") && room.getUUID("room").equals(pinnedRoom); }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        room = new CompoundTag();
        pinnedRoom = null;
    }

    private static boolean shown() {
        var mc = Minecraft.getInstance();
        return room.hasUUID("room") && mc.player != null && mc.screen == null
                && !mc.player.level().dimension().location().getNamespace().equals("custom_instances")
                && !com.johnn.realtimecobblemon.client.ClientSmash.active();
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!room.hasUUID("room")) return;
        while (RESUME.consumeClick()) if (shown()) {
            pinnedRoom = null;
            var data = new CompoundTag();
            data.putString("action", "resume");
            PacketDistributor.sendToServer(new FrontierRooms.Command(data));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST) public static void hud(RenderGuiEvent.Post event) {
        if (!shown() || Minecraft.getInstance().options.hideGui) return;
        int round = room.getInt("round");
        var own = room.getCompound(round == 0 ? room.getBoolean("owner") ? "host" : "guest" : "own");
        var opponent = room.getCompound(round == 0 ? room.getBoolean("owner") ? "guest" : "host" : "currentOpponent");
        var player = Minecraft.getInstance().player;
        if (own.isEmpty()) {
            own = new CompoundTag();
            own.putUUID("player", player.getUUID());
            own.putString("name", player.getGameProfile().getName());
        }
        var status = tr(room.getBoolean("finished") ? "panel.awards" : room.getBoolean("bye") ? "hud_bye"
                : room.getBoolean("boutDone") ? "hud_wait_round"
                : round > 0 ? room.contains("own") ? "hud_lineup" : "hud_schedule"
                : room.getBoolean("multi") ? "hud_wait_room"
                : opponent.isEmpty() ? "hud_wait_opponent" : room.getBoolean("selfReady") ? "hud_ready" : "hud_lineup");
        var hint = tr("hud_resume", Component.keybind(BINDING));
        var font = Minecraft.getInstance().font;
        int width = Math.min(event.getGuiGraphics().guiWidth() - 14, 214);
        int x = 7, y = 7;
        var g = event.getGuiGraphics();
        g.flush();
        g.pose().pushPose();
        try {
            g.pose().translate(0, 0, 500);
            OreUiRenderer.panel(g, x, y, width, 65, 1, GOLD);
            center(g, font, status, x + width / 2, y + 7, width - 14, GOLD);
            int mid = x + width / 2;
            FrontierConsole.player(g, own, x + 9, y + 22, 17, CYAN);
            text(g, font, Component.literal(own.getString("name")), x + 32, y + 27, width / 2 - 48, TEXT);
            if (!opponent.isEmpty() && (round > 0 || !room.getBoolean("multi"))) {
                center(g, font, Component.literal("VS"), mid, y + 27, 24, GOLD);
                FrontierConsole.player(g, opponent, x + width - 26, y + 22, 17, PINK);
                center(g, font, Component.literal(opponent.getString("name")), mid + (width / 2 - 18) / 2, y + 27, width / 2 - 52, TEXT);
            } else {
                var detail = round > 0 ? tr("round", round) : tr("hud_players", room.getInt("count"));
                center(g, font, detail, x + width * 3 / 4, y + 27, width / 2 - 18, MUTED);
            }
            g.fill(x + 8, y + 45, x + width - 8, y + 46, NEUTRAL);
            center(g, font, hint, mid, y + 51, width - 14, TEXT);
            g.flush();
        } finally { g.pose().popPose(); }
    }

    private static Component tr(String key, Object... args) { return Component.translatable("rumble.frontier." + key, args); }
}
