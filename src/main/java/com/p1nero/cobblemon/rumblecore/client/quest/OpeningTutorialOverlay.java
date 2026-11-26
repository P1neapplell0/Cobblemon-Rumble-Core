package com.p1nero.cobblemon.rumblecore.client.quest;

import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.gui.interact.partyselect.PartySelectGUI;
import com.cobblemon.mod.common.client.gui.summary.Summary;
import com.cobblemon.mod.common.client.gui.summary.widgets.screens.SummaryTab;
import com.cobblemon.mod.common.client.gui.summary.widgets.screens.moves.MoveSwapScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import com.cobblemon.mod.common.client.gui.summary.widgets.screens.moves.MovesWidget;
import com.johnn.realtimecobblemon.client.ClientSmash;
import com.nbp.cobblemon_smartphone.client.gui.PokeInfoDetailScreen;
import com.nbp.cobblemon_smartphone.client.gui.PokeInfoScreen;
import com.nbp.cobblemon_smartphone.client.gui.SmartphoneScreen;
import com.nbp.cobblemon_smartphone.item.SmartphoneItem;
import com.cobblemon.mod.common.client.keybind.CobblemonKeyBinds;
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
    private static final long CANDY_NOTICE_MS = 5000, CANDY_FADE_MS = 450;
    private static final int GOLD = 0xfff9df94, PAPER = 0xfff9f5e8;
    private static Step step = Step.INACTIVE;
    private static long shownAt, sentAt, actedAt, mainGroupSeen;
    private static boolean acted, candyNotice;
    private static long candyNoticeUntil;
    private static Screen observed;
    private static Target target;

    private record Target(Screen screen, int x, int y, int w, int h, long seen) {
    }

    private OpeningTutorialOverlay() { }

    public static boolean isActive() { return step.active(); }

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
        actedAt = 0;
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
            actedAt = 0;
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        candyNotice = false;
        candyNoticeUntil = 0;
        sync(Step.INACTIVE.ordinal());
    }

    public static void candyComplete() {
        candyNotice = true;
        candyNoticeUntil = 0; // Start when the player can see the HUD, not while the selector is closing.
    }

    public static void pointMoveControl(Summary summary, int x, int y, int w, int h) {
        if (target == null) point(Step.CAPTURE_SKILLS, summary, x, y, w, h);
    }

    @SubscribeEvent
    public static void beforeScreen(ScreenEvent.Render.Pre event) {
        // The native move rows report their actual bounds during this render pass.
        if (step == Step.CAPTURE_SKILLS && event.getScreen() instanceof Summary) target = null;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!available(mc)) {
            shownAt = 0;
            actedAt = 0;
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
            case CLOUD, WAYSTONES, DISTRIBUTION -> acted && actedAt != 0 && now - actedAt >= INTRO_MS;
            case POKEMON_INFO -> screen instanceof PokeInfoScreen || screen instanceof PokeInfoDetailScreen;
            // These follow-ups complete only from their authoritative server events:
            // BenchMoveHandler.exchangeMove and EXPERIENCE_CANDY_USE_POST.
            case CAPTURE_SKILLS, CANDY -> false;
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
        if (mc.screen != null) return;
        if (candyNotice && hudAvailable(mc)) {
            long now = Util.getMillis();
            if (candyNoticeUntil == 0) candyNoticeUntil = now + CANDY_NOTICE_MS;
            long remaining = candyNoticeUntil - now;
            if (remaining > 0) {
                drawCandyNotice(event.getGuiGraphics(), Math.min(1F, remaining / (float) CANDY_FADE_MS));
                return;
            }
            candyNotice = false;
        }
        if (available(mc)) draw(event.getGuiGraphics(), null);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void screen(ScreenEvent.Render.Post event) {
        if (available(Minecraft.getInstance())) draw(event.getGuiGraphics(), event.getScreen());
    }

    private static boolean available(Minecraft mc) {
        return isActive() && hudAvailable(mc) && (step == Step.CAPTURE_SKILLS || !ClientSmash.active());
    }

    private static boolean hudAvailable(Minecraft mc) {
        return mc.player != null && mc.getOverlay() == null && !mc.options.hideGui
                && ClientDialogueCache.INSTANCE.getCurrentSession() == null;
    }

    private static void drawCandyNotice(GuiGraphics graphics, float alpha) {
        var font = Minecraft.getInstance().font;
        var title = text("candy_title");
        // Keep the banner in the middle third, clear of the side chat and party HUDs.
        int wrapWidth = Math.max(1, Math.min(210, graphics.guiWidth() / 3 - 12));
        var lines = font.split(text("candy_opened"), wrapWidth);
        int w = Math.max(font.width(title), lines.stream().mapToInt(font::width).max().orElse(0)) + 20;
        int h = 25 + lines.size() * 11;
        int x = (graphics.guiWidth() - w) / 2;
        int y = Math.max(6, graphics.guiHeight() * 2 / 3 - h);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 480);
        graphics.fill(0, 0, w, h, fade(0xe91a202a, alpha));
        graphics.hLine(0, w - 1, 0, fade(GOLD, alpha));
        if (alpha >= 0.02F) {
            graphics.drawString(font, title, 10, 7, fade(GOLD, alpha));
            for (int i = 0; i < lines.size(); i++) graphics.drawString(font, lines.get(i), 10, 20 + i * 11, fade(PAPER, alpha));
        }
        graphics.pose().popPose();
    }

    private static int fade(int argb, float alpha) {
        return (argb & 0xffffff) | (Math.round((argb >>> 24) * alpha) << 24);
    }

    private static void draw(GuiGraphics graphics, Screen screen) {
        if (shownAt == 0 || !showOn(screen)) return;
        var mc = Minecraft.getInstance();
        boolean reopenJournal = screen == null && (step == Step.MAIN_GROUP || step == Step.MAIN_QUEST || step == Step.TRACK);
        boolean explaining = acted && (step == Step.CLOUD || step == Step.WAYSTONES || step == Step.DISTRIBUTION);
        boolean reopenPhone = screen == null && step.ordinal() >= Step.CLOUD.ordinal()
                && step.ordinal() <= Step.DISTRIBUTION.ordinal() && !explaining;
        KeyMapping key = step == Step.JOURNAL || reopenJournal ? ClientEventHandler.KEY_OPEN_JOURNAL
                : step == Step.OPEN_PHONE || reopenPhone ? mc.options.keyUse : null;
        String message = reopenJournal ? "journal" : reopenPhone ? "reopen_phone" : step.name().toLowerCase(Locale.ROOT);
        if (step == Step.CAPTURE_SKILLS) {
            if (ClientSmash.active() || CobblemonClient.INSTANCE.getBattle() != null) message = "capture_leave_battle";
            else if (screen instanceof Summary summary) {
                if (summary.getSideScreen() instanceof MoveSwapScreen swap && swap.getReplacedMove() != null) {
                    message = "capture_pick_move";
                } else if (summary.children().stream().anyMatch(MovesWidget.class::isInstance)) {
                    message = target != null ? "capture_replace_move" : "capture_no_moves";
                } else {
                    message = "capture_open_moves";
                    // Cobblemon 1.8.4: identify the native Moves tab by its icon, not by translated text or index.
                    summary.children().stream().filter(SummaryTab.class::isInstance).map(SummaryTab.class::cast)
                            .filter(tab -> tab.getIcon().getPath().endsWith("/summary_tab_icon_moves.png"))
                            .findFirst().ifPresent(tab -> pointWidget(summary, tab));
                }
            } else {
                key = CobblemonKeyBinds.INSTANCE.getSUMMARY();
            }
        } else if (step == Step.CANDY) {
            if (screen instanceof PartySelectGUI) message = "candy_choose";
            else if (mc.player.getMainHandItem().is(CobblemonItems.EXPERIENCE_CANDY_S)) {
                message = "candy_use";
                key = mc.options.keyUse;
            }
        }
        if (explaining) {
            message += "_opened";
            if (actedAt == 0) actedAt = Util.getMillis();
        } else if (step == Step.MAIN_GROUP && mainGroupVisible()) {
            message = "main_group_opened";
        } else if (step == Step.TRACK && screen instanceof QuestJournalScreen && trackingMainQuest()) {
            message = "already_tracked";
        } else if (step == Step.DISTRIBUTION && screen instanceof PokeInfoScreen) {
            message = "choose_pokemon";
        }
        Component binding = key == null ? Component.empty() : key.getTranslatedKeyMessage();
        var lines = mc.font.split(text(message, binding), Math.max(1, Math.min(210, graphics.guiWidth() - 32)));
        var title = text(switch (step) {
            case CAPTURE_SKILLS -> "capture_title";
            case CANDY -> "candy_title";
            default -> step.ordinal() <= Step.CLOSE_JOURNAL.ordinal() ? "journal_title" : "phone_title";
        });
        int w = Math.max(mc.font.width(title) + 20, lines.stream().mapToInt(mc.font::width).max().orElse(0) + 20);
        if (key != null) w = Math.max(w, mc.font.width(binding) + 32);
        int h = 25 + lines.size() * 11 + (key == null ? 0 : 22);
        float scale = Math.min(1F, Math.min(Math.max(1, graphics.guiWidth() - 12) / (float) w,
                Math.max(1, graphics.guiHeight() - 12) / (float) h));
        int boxW = (int) Math.ceil(w * scale), boxH = (int) Math.ceil(h * scale);
        Target mark = !explaining && target != null && target.screen() == screen && Util.getMillis() - target.seen() < 250 ? target : null;
        int x = (graphics.guiWidth() - boxW) / 2;
        int y = screen == null ? Math.max(6, graphics.guiHeight() * 2 / 3 - boxH) : 6;
        // The native summary uses the top edge for tabs; keep the tutorial beneath its panel.
        boolean nativeMoves = screen instanceof Summary && step == Step.CAPTURE_SKILLS;
        if (nativeMoves) y = graphics.guiHeight() - boxH - 6;
        if (mark != null && !nativeMoves) {
            if (mark.x() + mark.w() + boxW + 17 <= graphics.guiWidth()) {
                x = mark.x() + mark.w() + 11;
                y = mark.y() + mark.h() / 2 - boxH / 2;
            } else if (mark.x() >= boxW + 17) {
                x = mark.x() - boxW - 11;
                y = mark.y() + mark.h() / 2 - boxH / 2;
            } else {
                y = mark.y() >= boxH + 17 ? mark.y() - boxH - 11 : mark.y() + mark.h() + 11;
            }
        }
        y = Math.max(6, Math.min(y, graphics.guiHeight() - boxH - 6));
        graphics.pose().pushPose();
        // Summary draws its panels and the move tooltip above the normal HUD depth.
        graphics.pose().translate(0, 0, nativeMoves ? 1200 : 480);
        if (mark != null) {
            TutorialPulse.render(graphics, mark.x(), mark.y(), mark.w(), mark.h());
            if (!nativeMoves) {
                int mx = mark.x() + mark.w() / 2, my = mark.y() + mark.h() / 2;
                int px = Math.clamp(mx, x, x + boxW), py = Math.clamp(my, y, y + boxH);
                graphics.hLine(Math.min(mx, px), Math.max(mx, px), my, 0xccf9df94);
                graphics.vLine(px, Math.min(my, py), Math.max(my, py), 0xccf9df94);
            }
        }
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.fill(0, 0, w, h, 0xe91a202a);
        graphics.hLine(0, w - 1, 0, GOLD);
        graphics.drawString(mc.font, title, 10, 7, GOLD);
        int lineY = 20;
        // Pass the font's original wrapped sequences straight to the renderer. Partial
        // character visitors can break shaping/batching in production text renderers.
        for (var line : lines) {
            graphics.drawString(mc.font, line, 10, lineY, PAPER);
            lineY += 11;
        }
        if (key != null) {
            int bw = mc.font.width(binding) + 18;
            int bx = (w - bw) / 2;
            graphics.fill(bx, lineY + 2, bx + bw, lineY + 17, 0xff302e2a);
            TutorialPulse.render(graphics, bx, lineY + 2, bw, 15);
            graphics.drawCenteredString(mc.font, binding, w / 2, lineY + 5, PAPER);
        }
        graphics.pose().popPose();
    }

    private static void pointWidget(Summary summary, AbstractWidget widget) {
        pointMoveControl(summary, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
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
            case CAPTURE_SKILLS -> screen == null || screen instanceof Summary;
            case CANDY -> screen == null || screen instanceof PartySelectGUI;
            default -> false;
        };
    }

    private static Component text(String suffix, Object... args) {
        return Component.translatable("rumble.opening_tutorial." + suffix, args);
    }
}
