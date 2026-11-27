package com.p1nero.cobblemon.rumblecore.client.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.CobblemonEntities;
import com.mojang.logging.LogUtils;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.GimmickCinematics;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** A blocking but non-pausing screen: the server and the other viewer's animation keep ticking. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class GimmickCinematicScreen extends Screen {
    private static GimmickCinematicScreen current;
    private final GimmickCinematics.Start scene;
    private final Screen previous;
    private final long started = Util.getMillis();
    private boolean acknowledged, rendering;

    private GimmickCinematicScreen(GimmickCinematics.Start scene, Screen previous) {
        super(Component.translatable("rumble.cinematic." + GimmickCinematics.Kind.values()[scene.kind()].name().toLowerCase(Locale.ROOT)));
        this.scene = scene;
        this.previous = previous;
    }

    public static void start(GimmickCinematics.Start scene) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(scene.dimension())
                || scene.kind() < 0 || scene.kind() >= GimmickCinematics.Kind.values().length) return;
        close(true);
        var screen = new GimmickCinematicScreen(scene, mc.screen);
        current = screen;
        mc.setScreen(screen);
        if (ModList.get().isLoaded("cobblemoncinematics")) screen.rendering = Backend.begin(scene);
    }

    public static void finish(UUID id) {
        if (current != null && current.scene.id().equals(id)) close(true);
    }

    private static void close(boolean restore) {
        var screen = current;
        if (screen == null) return;
        current = null;
        if (screen.rendering) Backend.stop();
        var mc = Minecraft.getInstance();
        if (restore && mc.screen == screen) mc.setScreen(screen.previous);
    }

    @SubscribeEvent
    public static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        if (ModList.get().isLoaded("cobblemoncinematics")) PacketDistributor.sendToServer(new GimmickCinematics.Ready());
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { close(false); }

    @SubscribeEvent
    public static void retainWhilePlaying(ScreenEvent.Opening event) {
        // Other battle UI (e.g. a party or pause screen) must not displace an active presentation.
        var screen = current;
        var mc = Minecraft.getInstance();
        if (screen != null && mc.screen == screen && mc.level != null && mc.player != null && mc.player.isAlive()
                && mc.level.dimension().location().equals(screen.scene.dimension())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tickScene(ClientTickEvent.Post event) {
        var screen = current;
        if (screen == null) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().location().equals(screen.scene.dimension())) {
            close(false);
            if (mc.screen == screen) mc.setScreen(null);
            return;
        }
        long elapsed = Util.getMillis() - screen.started;
        if (!screen.acknowledged && elapsed >= GimmickCinematics.ANIMATION_MS) {
            screen.acknowledged = true;
            PacketDistributor.sendToServer(new GimmickCinematics.Seen(screen.scene.id()));
        }
        // Last-resort client cleanup if dimension/session teardown interrupts the Finish packet.
        if (elapsed >= 12_000) close(true);
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (rendering && Backend.render(graphics)) return;
        // Clients without the optional renderer still get a local, non-pausing input lock and a cue.
        graphics.fill(0, 0, width, height, 0x9c101621);
        graphics.drawCenteredString(font, getTitle(), width / 2, height / 2 - 12, 0xFFF2C4);
        graphics.drawCenteredString(font, scene.pokemon().getDisplayName(false), width / 2, height / 2 + 4, 0xFFFFFF);
    }

    /** Cinematics 1.4.0 only exposes the native turn-based message hook publicly. Its private
     * enqueue/render methods are isolated here so the mod is still optional on both sides. */
    private static final class Backend {
        private static Object manager;
        private static Method enqueue, render, stop;
        private static Class<?> type;
        private static boolean broken;

        private static void resolve() throws ReflectiveOperationException {
            if (enqueue != null) return;
            var cls = Class.forName("com.p1nero.cceib.client.compat.megashowdown.MegaShowdownCinematicManager");
            type = Class.forName(cls.getName() + "$GimmickType");
            manager = cls.getField("INSTANCE").get(null);
            stop = cls.getMethod("stopTest");
            render = cls.getDeclaredMethod("render", GuiGraphics.class);
            render.setAccessible(true);
            enqueue = cls.getDeclaredMethod("enqueue", type, Component.class, UUID.class, PokemonEntity.class);
            enqueue.setAccessible(true);
        }

        static boolean begin(GimmickCinematics.Start scene) {
            if (broken) return false;
            try {
                resolve();
                stop.invoke(manager);
                var level = Minecraft.getInstance().level;
                var found = level.getEntity(scene.entityId());
                var snapshot = found instanceof PokemonEntity pokemon && pokemon.getPokemon().getUuid().equals(scene.pokemon().getUuid())
                        ? null : new PokemonEntity(level, scene.pokemon(), CobblemonEntities.POKEMON);
                var name = GimmickCinematics.Kind.values()[scene.kind()].name();
                Object gimmick = java.util.Arrays.stream(type.getEnumConstants())
                        .filter(entry -> ((Enum<?>) entry).name().equals(name)).findFirst().orElseThrow();
                enqueue.invoke(manager, gimmick, scene.pokemon().getDisplayName(false), scene.pokemon().getUuid(), snapshot);
                return true;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                failed(e);
                return false;
            }
        }

        static boolean render(GuiGraphics graphics) {
            if (broken) return false;
            try { render.invoke(manager, graphics); return true; }
            catch (ReflectiveOperationException | RuntimeException | LinkageError e) { failed(e); return false; }
        }

        static void stop() {
            try { if (stop != null && manager != null) stop.invoke(manager); }
            catch (ReflectiveOperationException | RuntimeException | LinkageError e) { failed(e); }
        }

        private static void failed(Throwable e) {
            if (!broken) LogUtils.getLogger().warn("Unable to play real-time gimmick via Cobblemon Cinematics; using the basic cue", e);
            broken = true;
        }
    }
}
