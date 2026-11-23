package com.p1nero.cobblemon.rumblecore.prologue.client;

import com.p1nero.cobblemon.rumblecore.gym.MegaTrials;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TrialPortraits;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TutorialPulse;
import com.p1nero.cobblemon.rumblecore.prologue.MegaSimulator;
import com.p1nero.misc.custom_instances.client.InstanceCatalogIcons;
import com.p1nero.misc.custom_instances.client.screen.FantasyScreen;
import com.p1nero.misc.custom_instances.client.style.UiTheme;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MegaSimulatorScreen extends FantasyScreen {
    private static final String KEY = "rumble.mega.simulator.";
    private static final int CHOICE_HEIGHT = 58;
    private static final int CHOICE_GAP = 8;
    private final MegaSimulator.Open state;

    private MegaSimulatorScreen(MegaSimulator.Open state) {
        super(Component.translatable(KEY + "title"));
        this.state = state;
        panelHeight = 112;
    }

    public static void open(MegaSimulator.Open state) {
        InstanceCatalogIcons.register(MegaTrials.PORTRAIT, new TrialPortraits()::draw);
        Minecraft.getInstance().setScreen(new MegaSimulatorScreen(state));
    }

    @Override protected void init() {
        super.init();
        panelWidth = Math.min(panelWidth, 406);
        left = (width - panelWidth) / 2;
        int choiceWidth = (panelWidth - 32 - 4 * CHOICE_GAP) / 5;
        int rowLeft = (width - (5 * choiceWidth + 4 * CHOICE_GAP)) / 2;
        int step = choiceWidth + CHOICE_GAP;
        int y = top + 41;
        addChoice(MegaSimulator.MEGA, "mega", state.mega(), "mega_showdown:keystone", Items.AMETHYST_SHARD,
                rowLeft, y, choiceWidth);
        addChoice(MegaSimulator.UNIVERSE, "universe", state.universe(), "cobblebash:training_simulator", Items.RESPAWN_ANCHOR,
                rowLeft + step, y, choiceWidth);
        addChoice(MegaSimulator.SAFARI, "safari", state.safari(), "cobblemon:safari_ball", Items.COMPASS,
                rowLeft + 2 * step, y, choiceWidth);
        addChoice(MegaSimulator.CINEMA, "cinema", state.cinema(), "minecraft:music_disc_otherside", Items.JUKEBOX,
                rowLeft + 3 * step, y, choiceWidth);
        addChoice(MegaSimulator.FRONTIER, "frontier", true, "minecraft:diamond_sword", Items.DIAMOND_SWORD,
                rowLeft + 4 * step, y, choiceWidth);
    }

    private void addChoice(int selection, String key, boolean unlocked, String itemId, Item fallback,
                           int x, int y, int buttonWidth) {
        var label = Component.translatable(KEY + key);
        var hint = Component.translatable(KEY + "locked");
        var icon = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId)).orElse(fallback).getDefaultInstance();
        addRenderableWidget(new SimulatorChoice(x, y, buttonWidth, label, icon, unlocked,
                state.teaching() && selection == MegaSimulator.UNIVERSE, pressed -> {
            if (unlocked) PacketDistributor.sendToServer(new MegaSimulator.Select(selection));
            else setStatus(hint.getString());
        }));
    }

    private static final class SimulatorChoice extends Button {
        private final ItemStack icon;
        private final boolean unlocked, teaching;
        private long pressedUntil;

        private SimulatorChoice(int x, int y, int width, Component label, ItemStack icon, boolean unlocked, boolean teaching, OnPress press) {
            // Locked choices remain focusable so their unlock hints are reachable by keyboard as well.
            super(x, y, width, CHOICE_HEIGHT, label, press, DEFAULT_NARRATION);
            this.icon = icon;
            this.unlocked = unlocked;
            this.teaching = teaching;
        }

        @Override public void onPress() {
            if (unlocked) pressedUntil = Util.getMillis() + 120;
            super.onPress();
        }

        @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            if (teaching && unlocked) TutorialPulse.render(graphics, x, getY(), width, height);
            int y = getY();
            boolean highlighted = isHoveredOrFocused();
            boolean pressed = UiTheme.ore() && Util.getMillis() < pressedUntil;
            int offset = pressed ? 2 : 0;
            if (UiTheme.bedrock()) {
                UiTheme.button(graphics, x, y, width, height, unlocked, highlighted, false, pressed);
                if (!unlocked && highlighted) graphics.renderOutline(x - 1, y - 1, width + 2, height + 2, 0xFFFFFFFF);
            } else {
                graphics.fill(x, y, x + width, y + height, highlighted ? 0xFFFFDD89 : unlocked ? 0xFFAA8347 : 0xFF5A5244);
                graphics.fillGradient(x + 1, y + 1, x + width - 1, y + height - 1,
                        unlocked ? 0xFF63442C : 0xFF302B25, unlocked ? 0xFF2A1D17 : 0xFF211F1B);
            }

            graphics.pose().pushPose();
            graphics.pose().translate(x + width / 2F, y + 20 + offset, 0);
            float iconScale = Math.min(1.625F, (width - 12) / 16F);
            graphics.pose().scale(iconScale, iconScale, 1);
            graphics.renderItem(icon, -8, -8);
            graphics.pose().popPose();

            int color = unlocked ? UiTheme.buttonText(highlighted, 0xFFFFE1A6) : UiTheme.disabledButtonText();
            if (!unlocked) {
                int lockX = x + width - 14;
                int lockY = y + 7;
                graphics.renderOutline(lockX + 1, lockY, 6, 6, color);
                graphics.fill(lockX, lockY + 4, lockX + 8, lockY + 9, color);
            }

            var font = Minecraft.getInstance().font;
            int labelWidth = font.width(getMessage());
            float textScale = Math.min(1F, (width - 10F) / Math.max(1, labelWidth));
            graphics.pose().pushPose();
            graphics.pose().translate(x + width / 2F, y + 43 + offset - font.lineHeight * textScale / 2, 0);
            graphics.pose().scale(textScale, textScale, 1);
            graphics.drawString(font, getMessage(), -labelWidth / 2, 0, color, !UiTheme.bedrock());
            graphics.pose().popPose();
        }
    }
}
