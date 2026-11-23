package com.p1nero.cobblemon.rumblecore.client.quest;

import com.cobblemon.mod.common.CobblemonSounds;
import com.johnn.realtimecobblemon.client.ClientSmash;
import com.nbp.cobblemon_smartphone.client.gui.PokeInfoDetailScreen;
import com.nbp.cobblemon_smartphone.client.gui.PokeInfoScreen;
import com.nbp.cobblemon_smartphone.client.gui.SmartphoneScreen;
import com.nbp.cobblemon_smartphone.item.SmartphoneItem;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TutorialPulse;
import com.p1nero.cobblemon.rumblecore.phone.PhoneApp;
import com.p1nero.cobblemon.rumblecore.prologue.OpeningTutorial.Step;
import com.p1nero.cobblemon.rumblecore.prologue.OpeningTutorial;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.arcadia.arc_quest.client.events.ClientEventHandler;
import org.arcadia.arc_quest.client.hud.quest.journal.QuestJournalScreen;
import org.arcadia.arc_quest.client.quest.tracking.ClientQuestTrackingController;
import org.arcadia.arc_quest.dialogue.network.ClientDialogueCache;
import org.arcadia.arc_quest.quest.api.QuestCategory;
import org.arcadia.arc_quest.quest.registry.QuestRegistry;

/** Renders beside native controls; the walkthrough never intercepts input. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class OpeningTutorialOverlay {
    private static final long READ_MS = 1500, RETRY_MS = 700, INTRO_MS = 2200;
    private static final int GOLD = 0xfff9df94, PAPER = 0xfff9f5e8;
    private static Step step = Step.INACTIVE;
    private static long shownAt, sentAt, actedAt, mainGroupSeen;
    private static boolean acted;
    private static Screen observed;
    private static Target target;

    private record Target(Screen screen, int x, int y, int w, int h, long seen) {
    }

    private OpeningTutorialOverlay() { }

    public static boolean at(Step expected) { return step == expected; }

    public static boolean mainQuest(String id) {
        var definition = id == null ? null : QuestRegistry.get(id);
        return definition != null && QuestCategory.ARCHON.equals(definition.getCategory());
    }

    public static Step phoneStep(String id) {
        if (PhoneApp.CLOUD.actionId().equals(id) || "cobblemon_smartphone:ender".equals(id)) return Step.CLOUD;
        if (PhoneApp.WAYSTONE.actionId().equals(id)) return Step.WAYSTONES;
        if ("cobblemon_smartphone:pokeinfo".equals(id)) return Step.POKEMON_INFO;
        return null;
    }

    public static void pointJournal(Step expected, QuestJournalScreen screen, int x, int y, int w, int h) {
        float scale = screen.getUiScale();
        point(expected, screen, Math.round(x * scale), Math.round(y * scale),
                Math.round(w * scale), Math.round(h * scale));
    }

    public static void observeMainGroup(QuestJournalScreen screen, boolean expanded, int x, int y, int w, int h) {
        if (expanded) {
            mainGroupSeen = Util.getMillis();
            target = null;
        } else {
            mainGroupSeen = 0;
            pointJournal(Step.MAIN_GROUP, screen, x, y, w, h);
        }
    }

    public static boolean trackingMainQuest() {
        return mainQuest(ClientQuestTrackingController.INSTANCE.trackedQuestId());
    }

    private static boolean mainGroupVisible() {
        return mainGroupSeen != 0 && Util.getMillis() - mainGroupSeen < 500;
    }

    public static void sync(int value) {
        Step next = Step.fromId(value);
        if (next == step) return;
        var client = Minecraft.getInstance();
        if (step.active() && next.ordinal() == step.ordinal() + 1) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(CobblemonSounds.GUI_CLICK, 1.3f, .55f));
        }
        step = next;
        shownAt = 0;
        sentAt = 0;
        mainGroupSeen = 0;
        acted = false;
        target = null;
        observed = null;
    }

    public static void point(Step expected, Screen screen, int x, int y, int w, int h) {
        if (step != expected || screen != Minecraft.getInstance().screen || w <= 0 || h <= 0
                || x + w < 0 || y + h < 0 || x >= screen.width || y >= screen.height) return;
        target = new Target(screen, x, y, w, h, Util.getMillis());
    }

    public static void action(Step expected) {
        if (step == expected && !acted) {
            acted = true;
            actedAt = Util.getMillis();
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { sync(Step.INACTIVE.ordinal()); }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!available(mc)) {
            shownAt = 0;
            return;
        }
        var screen = mc.screen;
        long now = Util.getMillis();
        if (shownAt == 0) shownAt = now;
        if (observed != screen) {
            if (step == Step.CLOSE_JOURNAL && observed instanceof QuestJournalScreen && screen == null) acted = true;
            observed = screen;
            target = null;
        }
        boolean mainHand = mc.player.getMainHandItem().getItem() instanceof SmartphoneItem;
        boolean completed = switch (step) {
            case JOURNAL -> screen instanceof QuestJournalScreen;
            case MAIN_GROUP -> screen instanceof QuestJournalScreen && mainGroupVisible();
            case MAIN_QUEST -> screen instanceof QuestJournalScreen journal && mainQuest(journal.getSelectedQuestId());
            case TRACK -> screen instanceof QuestJournalScreen && trackingMainQuest();
            case CLOSE_JOURNAL -> acted || screen == null;
            case HOLD_PHONE -> mainHand;
            case OPEN_PHONE -> mainHand && screen instanceof SmartphoneScreen;
            case CLOUD, WAYSTONES -> acted && now - actedAt >= INTRO_MS;
            case POKEMON_INFO -> screen instanceof PokeInfoScreen || screen instanceof PokeInfoDetailScreen;
            case DISTRIBUTION -> acted && now - actedAt >= INTRO_MS;
            default -> false;
        };
        long duration = step == Step.JOURNAL || step == Step.HOLD_PHONE ? INTRO_MS : READ_MS;
        if (completed && now - shownAt >= duration && now - sentAt >= RETRY_MS) {
            sentAt = now;
            PacketDistributor.sendToServer(new OpeningTutorial.Acknowledge(step.ordinal()));
        }
    }

    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.screen == null && available(mc)) draw(event.getGuiGraphics(), null);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void screen(ScreenEvent.Render.Post event) {
        if (available(Minecraft.getInstance())) draw(event.getGuiGraphics(), event.getScreen());
    }

    private static boolean available(Minecraft mc) {
        return step.active() && mc.player != null && !mc.options.hideGui && !ClientSmash.active()
                && ClientDialogueCache.INSTANCE.getCurrentSession() == null;
    }

    private static void draw(GuiGraphics graphics, Screen screen) {
        if (shownAt == 0 || !showOn(screen)) return;
        long age = Util.getMillis() - shownAt;
        var mc = Minecraft.getInstance();
        boolean reopenJournal = screen == null && (step == Step.MAIN_GROUP || step == Step.MAIN_QUEST || step == Step.TRACK);
        boolean explaining = acted && (step == Step.CLOUD || step == Step.WAYSTONES || step == Step.DISTRIBUTION);
        boolean reopenPhone = screen == null && step.ordinal() >= Step.CLOUD.ordinal() && !explaining;
        KeyMapping key = step == Step.JOURNAL || reopenJournal ? ClientEventHandler.KEY_OPEN_JOURNAL
                : step == Step.OPEN_PHONE || reopenPhone ? mc.options.keyUse : null;
        String message = reopenJournal ? "journal" : reopenPhone ? "reopen_phone" : step.name().toLowerCase(Locale.ROOT);
        if (explaining) {
            message += "_opened";
            age = Util.getMillis() - actedAt;
        } else if (step == Step.MAIN_GROUP && mainGroupVisible()) {
            message = "main_group_opened";
        } else if (step == Step.TRACK && screen instanceof QuestJournalScreen && trackingMainQuest()) {
            message = "already_tracked";
        } else if (step == Step.DISTRIBUTION && screen instanceof PokeInfoScreen) {
            message = "choose_pokemon";
        }
        Component binding = key == null ? Component.empty() : key.getTranslatedKeyMessage();
        var lines = mc.font.split(text(message, binding), Math.min(210, graphics.guiWidth() - 22));
        var title = text(step.ordinal() <= Step.CLOSE_JOURNAL.ordinal() ? "journal_title" : "phone_title");
        int w = Math.min(graphics.guiWidth() - 12, Math.max(mc.font.width(title) + 18,
                lines.stream().mapToInt(mc.font::width).max().orElse(0) + 20));
        int h = 25 + lines.size() * 11 + (key == null ? 0 : 22);
        Target mark = !explaining && target != null && target.screen() == screen && Util.getMillis() - target.seen() < 250 ? target : null;
        int x = (graphics.guiWidth() - w) / 2;
        int y = screen == null ? Math.max(6, graphics.guiHeight() * 2 / 3 - h) : 6;
        if (mark != null) {
            if (mark.x() + mark.w() + w + 17 <= graphics.guiWidth()) {
                x = mark.x() + mark.w() + 11;
                y = mark.y() + mark.h() / 2 - h / 2;
            } else if (mark.x() >= w + 17) {
                x = mark.x() - w - 11;
                y = mark.y() + mark.h() / 2 - h / 2;
            } else {
                y = mark.y() >= h + 17 ? mark.y() - h - 11 : mark.y() + mark.h() + 11;
            }
        }
        y = Math.max(6, Math.min(y, graphics.guiHeight() - h - 6));
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 480);
        if (mark != null) {
            TutorialPulse.render(graphics, mark.x(), mark.y(), mark.w(), mark.h());
            int mx = mark.x() + mark.w() / 2, my = mark.y() + mark.h() / 2;
            int px = Math.clamp(mx, x, x + w), py = Math.clamp(my, y, y + h);
            graphics.hLine(Math.min(mx, px), Math.max(mx, px), my, 0xccf9df94);
            graphics.vLine(px, Math.min(my, py), Math.max(my, py), 0xccf9df94);
        }
        graphics.fill(x, y, x + w, y + h, 0xe91a202a);
        graphics.hLine(x, x + w - 1, y, GOLD);
        graphics.drawString(mc.font, title, x + 10, y + 7, GOLD);
        int lineY = y + 20;
        int ink = (int) Math.min(Integer.MAX_VALUE, age / 13);
        for (var line : lines) {
            final int[] remaining = {ink};
            graphics.drawString(mc.font, (FormattedCharSequence) sink -> line.accept((i, style, cp) ->
                    remaining[0]-- > 0 && sink.accept(i, style, cp)), x + 10, lineY, PAPER);
            ink = Math.max(0, remaining[0]);
            lineY += 11;
        }
        if (key != null) {
            int bw = mc.font.width(binding) + 18;
            int bx = x + (w - bw) / 2;
            graphics.fill(bx, lineY + 2, bx + bw, lineY + 17, 0xff302e2a);
            TutorialPulse.render(graphics, bx, lineY + 2, bw, 15);
            graphics.drawCenteredString(mc.font, binding, x + w / 2, lineY + 5, PAPER);
        }
        graphics.pose().popPose();
    }

    private static boolean showOn(Screen screen) {
        if (acted && (step == Step.CLOUD || step == Step.WAYSTONES || step == Step.DISTRIBUTION)) return true;
        return switch (step) {
            case JOURNAL -> screen == null;
            case MAIN_GROUP, MAIN_QUEST, TRACK -> screen == null || screen instanceof QuestJournalScreen;
            case CLOSE_JOURNAL -> screen instanceof QuestJournalScreen;
            case HOLD_PHONE, OPEN_PHONE -> screen == null;
            case CLOUD, WAYSTONES -> screen == null || screen instanceof SmartphoneScreen;
            case POKEMON_INFO, DISTRIBUTION -> screen == null || screen instanceof SmartphoneScreen
                    || screen instanceof PokeInfoScreen || screen instanceof PokeInfoDetailScreen;
            default -> false;
        };
    }

    private static Component text(String suffix, Object... args) {
        return Component.translatable("rumble.opening_tutorial." + suffix, args);
    }
}
