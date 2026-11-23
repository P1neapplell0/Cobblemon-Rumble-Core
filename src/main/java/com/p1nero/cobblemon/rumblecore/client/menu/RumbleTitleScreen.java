package com.p1nero.cobblemon.rumblecore.client.menu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ModListScreen;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class RumbleTitleScreen extends Screen {
    private static final String[] ICONS = {"settings", "discord", "mods", "language", "quit"};
    private final TitleFighters fighters = new TitleFighters();
    private long lastFrameNanos;
    private double elapsed;
    private int iconsY;

    public RumbleTitleScreen() { super(Component.literal("Cobblemon Rumble")); }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void replaceTitle(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof TitleScreen) event.setNewScreen(new RumbleTitleScreen());
    }

    static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(CobblemonRumbleCore.MOD_ID, "textures/title/" + name + ".png");
    }

    static void image(GuiGraphics g, String name, int x, int y, int w, int h, int sw, int sh) {
        ResourceLocation id = texture(name);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Minecraft.getInstance().getTextureManager().getTexture(id).setFilter(true, false);
        g.blit(id, x, y, w, h, 0, 0, sw, sh, sw, sh);
        RenderSystem.disableBlend();
    }

    @Override public void added() {
        lastFrameNanos = 0;
        elapsed = 0;
        fighters.reset();
    }
    @Override public void removed() { fighters.stopSounds(); }
    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override protected void init() {
        int center = width / 2;
        int row = Math.max(12, Math.round(height * .055f));
        int actionWidth = Math.min(width, Math.round(row * 7));
        iconsY = height - Math.max(12, Math.round(height * .058f));
        addRenderableWidget(new Control(center - actionWidth / 2, iconsY - row * 5 / 2, actionWidth, row,
                "singleplayer", Component.literal("Singleplayer"), () -> minecraft.setScreen(new SelectWorldScreen(this)), false));
        var multiplayer = addRenderableWidget(new Control(center - actionWidth / 2, iconsY - row * 3 / 2, actionWidth, row,
                "multiplayer", Component.literal("Multiplayer"), () -> minecraft.setScreen(minecraft.options.skipMultiplayerWarning
                ? new JoinMultiplayerScreen(this) : new SafetyScreen(this)), false));
        multiplayer.active = minecraft.allowsMultiplayer();
        int gap = Math.min(width / 8, Math.max(15, Math.round(height * .071f)));
        int button = Math.max(12, Math.round(height * .048f));
        Runnable[] actions = {
                () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)),
                () -> ConfirmLinkScreen.confirmLinkNow(this, PackMenus.discordUrl(), true),
                () -> minecraft.setScreen(new ModListScreen(this)),
                () -> minecraft.setScreen(new LanguageSelectScreen(this, minecraft.options, minecraft.getLanguageManager())),
                () -> minecraft.stop()
        };
        for (int i = 0; i < ICONS.length; i++) {
            String icon = ICONS[i];
            addRenderableWidget(new Control(center + (i - 2) * gap - button / 2, iconsY - button / 2, button, button,
                    icon, Component.translatable("rumble.title." + icon), actions[i], true));
        }
    }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) { }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean portrait = width / (float) height < 5f / 6;
        float fit = Math.max(width / 1672f, height / 941f);
        int backdropWidth = (int) Math.ceil(1672 * fit), backdropHeight = (int) Math.ceil(941 * fit);
        image(g, "clean-arena", (width - backdropWidth) / 2, (height - backdropHeight) / 2,
                backdropWidth, backdropHeight, 1672, 941);
        long now = System.nanoTime();
        if (lastFrameNanos != 0) elapsed += Math.min(.15, (now - lastFrameNanos) * 1e-9);
        lastFrameNanos = now;
        fighters.render(g, width, height, (float) elapsed);
        g.fillGradient(0, height * 67 / 100, width, height, 0x00080e1b, 0xe8080e1b);
        int logo = Math.min(Math.min((int) (width * (portrait ? .94f : .74f)), (int) (height * (portrait ? .68f : .66f))), 620);
        int mark = Math.min(Math.round(height * .079f), width / 8);
        int top = Math.round(height * (portrait ? .075f : .04f));
        image(g, "cobblemon-mark", (width - mark) / 2, top, mark, mark, 256, 256);
        image(g, "arena-wordmark", (width - logo) / 2, top + mark + Math.max(1, height / 250), logo, Math.round(logo * (461f / 1465)), 1465, 461);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private final class Control extends AbstractWidget {
        private final String asset;
        private final Runnable action;
        private final boolean icon;

        Control(int x, int y, int w, int h, String asset, Component label, Runnable action, boolean icon) {
            super(x, y, w, h, label);
            this.asset = asset;
            this.action = action;
            this.icon = icon;
        }

        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            boolean selected = isHoveredOrFocused();
            int w = icon ? Math.max(8, getWidth() * 62 / 100) : Math.round(getHeight() * (selected ? 5.7f : 5.4f));
            int h = icon ? w : Math.round(getHeight() * .91f);
            int x = getX() + getWidth() / 2;
            int y = getY() + getHeight() / 2;
            if (icon && selected) {
                RenderSystem.setShaderColor(1, .79f, .47f, 1);
            } else if (!active) {
                RenderSystem.setShaderColor(.6f, .6f, .6f, 1);
            }
            try {
                image(g, asset, x - w / 2, y - h / 2, w, h, icon ? 72 : 540, icon ? 72 : 90);
            } finally {
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
            if (icon && selected) {
                int labelX = Math.max(font.width(getMessage()) / 2 + 2, Math.min(x, RumbleTitleScreen.this.width - font.width(getMessage()) / 2 - 2));
                int labelY = Math.min(RumbleTitleScreen.this.height - font.lineHeight - 1, getY() + getHeight());
                g.drawCenteredString(font, getMessage(), labelX, labelY, 0xffffe2ab);
            }
        }

        @Override public void onClick(double x, double y) { action.run(); }
        @Override public boolean keyPressed(int key, int scan, int modifiers) {
            if (!active || !visible || (key != GLFW.GLFW_KEY_ENTER && key != GLFW.GLFW_KEY_KP_ENTER && key != GLFW.GLFW_KEY_SPACE)) return false;
            playDownSound(minecraft.getSoundManager());
            action.run();
            return true;
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
    }
}
