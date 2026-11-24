package com.p1nero.cobblemon.rumblecore.client.combat;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TrialPortraits;
import com.p1nero.cobblemon.rumblecore.combat.arena.HallOfFame;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.OreTheme;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HallOfFameScreen extends Screen {
    private static final int GOLD = 0xFFF0BD68, PAPER = 0xFFFFF4DA, MUTED = 0xFFC3B8AB;
    private final Screen parent;
    private final UUID token;
    private final CompoundTag data;
    private final ListTag roster;
    private final TrialPortraits portraits = new TrialPortraits();
    private final long opened = Util.getMillis();
    private Button done;
    private int selected, chime, profileScroll, scrollLimit;

    private HallOfFameScreen(Screen parent, UUID token, CompoundTag data) {
        super(text(token == null ? "roster" : "hall_title"));
        this.parent = parent;
        this.token = token;
        this.data = data;
        roster = data.getList("roster", Tag.TAG_COMPOUND);
    }

    @SubscribeEvent
    public static void register(FMLClientSetupEvent event) { event.enqueueWork(HallOfFame::registerClient); }

    public static void open(HallOfFame.Show packet) {
        var minecraft = Minecraft.getInstance();
        minecraft.setScreen(new HallOfFameScreen(null, packet.token(), packet.data()));
    }

    public static Screen details(Screen parent, CompoundTag data) {
        if (data.getBoolean("rumbleTeamRun")) return TeamDpsScreen.ranking(parent, data, data.getCompound("rumbleTeamReport"));
        return roster(parent, data);
    }

    static Screen roster(Screen parent, CompoundTag data) { return new HallOfFameScreen(parent, null, data); }

    @Override protected void init() {
        done = token == null ? addRenderableWidget(OreTheme.action(width / 2 - 48, height - 27, 96, 20,
                Component.translatable("gui.back"), false, b -> onClose())) : addRenderableWidget(Button.builder(Component.translatable(token == null ? "gui.back" : "gui.continue"), b -> onClose())
                .bounds(width / 2 - 48, height - 27, 96, 20).build());
        done.visible = token == null || elapsed() > 1700;
    }

    private long elapsed() { return Util.getMillis() - opened; }

    @Override public void tick() {
        if (token == null) return;
        long time = elapsed();
        done.visible = time > 1700;
        if (chime == 0 && time > 300) {
            chime++;
            if (minecraft.player != null) minecraft.player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, .7F, .8F);
        } else if (chime == 1 && time > 1600) {
            chime++;
            if (minecraft.player != null) minecraft.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, .55F, 1F);
        }
        if (time >= 8000) onClose();
    }

    @Override public void onClose() {
        if (token != null && !HallOfFame.isPreview(token)) PacketDistributor.sendToServer(new HallOfFame.Finish(token));
        minecraft.setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }

    public boolean isCeremony() { return token != null; }

    private int columns() { return Math.max(1, Math.min(roster.size(), width >= 330 ? 6 : 3)); }
    private int cardWidth() { return Math.max(24, Math.min(78, (width - 28) / columns())); }
    private int cardTop() { return token == null ? 50 : Math.max(54, height / 2 - 28); }
    private int cardHeight() {
        boolean wrapped = roster.size() > columns();
        if (token == null) return wrapped ? 38 : 50;
        return wrapped ? Math.min(46, cardWidth() + 13) : Math.min(75, cardWidth() + 13);
    }
    private int cardX(int index) { return (width - columns() * cardWidth()) / 2 + index % columns() * cardWidth(); }
    private int cardY(int index) { return cardTop() + index / columns() * cardHeight(); }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int y = token == null ? 13 : Math.max(13, cardTop() - 49);
        int edge = Math.max(12, width / 10);
        if (token == null) {
            graphics.fill(0, 0, width, height, 0xA0121214);
            int panelWidth = Math.min(width - 12, Math.max(340, columns() * cardWidth() + 12));
            OreTheme.panel(graphics, (width - panelWidth) / 2, 5, panelWidth, height - 10, 24);
        } else {
            graphics.fillGradient(0, 0, width, height, 0xFF29221D, 0xFF090B14);
            graphics.fill(edge, y, width - edge, y + 1, GOLD);
            graphics.fill(edge, y + 35, width - edge, y + 36, GOLD);
        }
        graphics.drawCenteredString(font, getTitle(), width / 2, y + 7, token == null ? OreTheme.TEXT : GOLD);
        graphics.drawCenteredString(font, data.getString("player"), width / 2, y + 21, token == null ? OreTheme.MUTED : PAPER);
        for (int i = 0; i < roster.size(); i++) {
            long revealTime = token == null ? 280L : elapsed() - 480L - i * 175L;
            if (revealTime < 0) break;
            float reveal = Math.min(1F, revealTime / 280F);
            int alpha = Math.round(reveal * 255);
            var pokemon = roster.getCompound(i);
            int x = cardX(i), top = cardY(i) + Math.round((1F - reveal) * 7), w = cardWidth(), h = cardHeight();
            boolean active = token == null && selected == i;
            if (token == null) {
                OreTheme.row(graphics, x + 1, top, w - 2, h - 1, active,
                        mouseX >= x && mouseX < x + w && mouseY >= top && mouseY < top + h);
                if (pokemon.getBoolean("used")) graphics.fill(x + 3, top + h - 3, x + w - 3, top + h - 2, OreTheme.GREEN);
            } else {
                graphics.fill(x + 1, top + 1, x + w - 1, top + h - 1, tint(0xFF151927, alpha));
                graphics.fill(x + 1, top + h - 2, x + w - 1, top + h - 1, tint(pokemon.getBoolean("used") ? GOLD : 0xFF575363, alpha));
            }
            int portraitSize = Math.min(w - 4, h - 13);
            if (alpha >= 96) {
                var battle = pokemon.getCompound("battle");
                portraits.draw(graphics, token == null && !battle.isEmpty() ? battle : pokemon,
                        x + (w - portraitSize) / 2, top + 2, portraitSize, partialTick);
                graphics.drawCenteredString(font, font.plainSubstrByWidth(species(pokemon).getString(), w - 4), x + w / 2,
                        top + h - 11, token == null ? OreTheme.TEXT : tint(PAPER, alpha));
            }
        }
        if (token == null && selected < roster.size()) renderDetails(graphics, roster.getCompound(selected));
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static int tint(int rgb, int alpha) { return (rgb & 0xFFFFFF) | (alpha << 24); }

    private void renderDetails(GuiGraphics graphics, CompoundTag pokemon) {
        int y = cardTop() + Math.max(1, (roster.size() + columns() - 1) / columns()) * cardHeight() + 4;
        var form = pokemon.getCompound("battle");
        int maxWidth = width - 20;
        var label = text(pokemon.getBoolean("used") ? "deployed" : "reserve", species(pokemon), pokemon.getInt("level"));
        graphics.drawCenteredString(font, font.plainSubstrByWidth(label.getString(), maxWidth), width / 2, y, GOLD);
        var id = ResourceLocation.tryParse(pokemon.getString("item"));
        var item = id == null ? Component.translatable("rumble.champion.none") : BuiltInRegistries.ITEM.get(id).getDescription();
        int x = Math.max(10, width / 2 - 157);
        int bottom = height - 32;
        int detailsTop = y + 11;
        graphics.enableScissor(9, detailsTop, width - 9, Math.max(detailsTop, bottom));
        y = draw(graphics, text("profile", Component.translatable(pokemon.getString("nature")),
                Component.translatable(pokemon.getString("ability")), item), x, detailsTop - profileScroll + 1);
        y = draw(graphics, text("ivs", stats(pokemon.getIntArray("ivs"))), x, y);
        y = draw(graphics, text("evs", stats(pokemon.getIntArray("evs"))), x, y);
        var moves = (form.isEmpty() ? pokemon : form).getList("moves", Tag.TAG_STRING);
        var names = Component.empty();
        for (int i = 0; i < moves.size(); i++) {
            if (i != 0) names.append("  ·  ");
            names.append(Component.translatable("cobblemon.move." + moves.getString(i)));
        }
        y = draw(graphics, text("moves", names), x, y);
        scrollLimit = Math.max(0, y + profileScroll - bottom);
        profileScroll = Math.min(profileScroll, scrollLimit);
        graphics.disableScissor();
    }

    private int draw(GuiGraphics graphics, Component line, int x, int y) {
        for (var part : font.split(line, width - x - 10)) {
            graphics.drawString(font, part, x, y, MUTED, false);
            y += 10;
        }
        return y;
    }

    private static String stats(int[] values) { return String.join(" / ", Arrays.stream(values).mapToObj(Integer::toString).toList()); }

    private static Component species(CompoundTag entry) {
        var id = ResourceLocation.tryParse(entry.getString("species"));
        return id == null ? text("none") : Component.translatable(id.getNamespace() + ".species." + id.getPath() + ".name");
    }

    private static Component text(String field, Object... args) { return Component.translatable("rumble.champion." + field, args); }

    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (isCeremony() || scrollLimit == 0) return false;
        profileScroll = Math.clamp(profileScroll - (int) Math.round(dy * 12), 0, scrollLimit);
        return true;
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (token == null && button == 0) for (int i = 0; i < roster.size(); i++) {
            if (x >= cardX(i) && x < cardX(i) + cardWidth() && y >= cardY(i) && y < cardY(i) + cardHeight()) {
                selected = i;
                profileScroll = 0;
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }
}
