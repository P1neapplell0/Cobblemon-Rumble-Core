package com.p1nero.cobblemon.rumblecore.prologue.client;

import com.p1nero.cobblemon.rumblecore.client.quest.GymDialogueCamera;
import com.p1nero.cobblemon.rumblecore.prologue.ProfessorRestock;
import com.p1nero.cobblemon.rumblecore.prologue.DialogueTarget;
import com.cobblemon.mod.common.client.keybind.keybinds.PartySendBinding;
import com.johnn.realtimecobblemon.client.BattleCamera;
import com.johnn.realtimecobblemon.client.ClientSmash;
import com.mojang.blaze3d.platform.InputConstants;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaPackets;
import com.p1nero.cobblemon.rumblecore.combat.arena.TeraCombat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.p1nero.cobblemon.rumblecore.prologue.ProfessorEntity;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import net.minecraft.world.entity.LivingEntity;
import com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.cobblemon.rumblecore.prologue.TeraChapter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import static com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets.Cue;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class PrologueClient {
    private static final String PREFIX = "rumble.prologue.";
    private static final ResourceLocation ARES_HUD = ResourceLocation.fromNamespaceAndPath("ares_hud", "main");
    private static final ResourceLocation INSTANCE_STATUS = ResourceLocation.fromNamespaceAndPath("custom_instances", "instance_status");
    private static final ResourceLocation QUEST_STATUS = ResourceLocation.fromNamespaceAndPath("arc_quest", "quest_hud");
    private static final ResourceLocation QUEST_TRACKER = ResourceLocation.fromNamespaceAndPath("arc_quest", "quest_tracker");
    private static final ResourceLocation QUEST_MARKERS = ResourceLocation.fromNamespaceAndPath("arc_quest", "quest_markers");
    private static final KeyMapping TALK = new KeyMapping("key.cobblemon_rumble_core.talk", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E, "key.categories.cobblemon_rumble_core");
    static final KeyMapping HELD = new KeyMapping("key.cobblemon_rumble_core.use_held", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Q, "key.categories.cobblemon_rumble_core");
    static final KeyMapping TERA = new KeyMapping(TeraCombat.BINDING, KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_T, "key.categories.cobblemon_rumble_core");
    private static boolean teraPressed, teraUsed;
    private static Cue cue = Cue.NONE;
    private static int target = -1, moves;
    private static boolean ready, sentReady, sentLock, accepted;
    private static final Set<Integer> SCANCODES = new HashSet<>();
    private static LivingEntity focusNpc, renderedNpc, pendingTalk;
    private static long focusNpcAt;
    private static long changedAt;
    private static long acceptedAt;
    private static final WeakHashMap<com.cobblemon.mod.common.entity.pokemon.PokemonEntity, Long> TENACITY = new WeakHashMap<>();

    private PrologueClient() {}

    @EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void key(RegisterKeyMappingsEvent event) { event.register(TALK); event.register(HELD); event.register(TERA); }
        @SubscribeEvent public static void renderer(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ProfessorEntity.TYPE.get(), ProfessorRenderer::new);
            event.registerEntityRenderer(com.p1nero.cobblemon.rumblecore.prologue.TrialMerchantEntity.TYPE.get(),
                    net.minecraft.client.renderer.entity.WanderingTraderRenderer::new);
        }
    }

    public static void receive(ProloguePackets.State state) {
        if (state.cue() < 0 || state.cue() >= Cue.values().length) return;
        Cue latest = Cue.values()[state.cue()];
        boolean changed = latest != cue || state.target() != target;
        if (changed) {
            changedAt = System.nanoTime();
            sentReady = false;
            sentLock = false;
        }
        var player = Minecraft.getInstance().player;
        if (state.accepted() && !accepted) {
            acceptedAt = System.nanoTime();
            if (player != null) player.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, .68f, 1.6f);
        } else if (changed && latest == Cue.CLEAR && player != null) {
            player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, .65f, 1.2f);
        } else if (latest.focus() && state.ready() && (changed || !ready) && player != null) {
            player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(), .25f, 1.5f);
        }
        cue = latest;
        ready = state.ready();
        accepted = state.accepted();
        target = state.target();
        moves = Math.clamp(state.moves(), 0, 4);
    }

    @SubscribeEvent public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        receive(new ProloguePackets.State(Cue.NONE.ordinal(), -1, false, 0, false));
        teraPressed = teraUsed = false;
        focusNpc = renderedNpc = pendingTalk = null;
        InteractionHud.clear();
    }

    public static void teraUsed(boolean used) { teraUsed = used; }
    public static void tenacity(int entityId, ResourceLocation dimension, int ticks) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(dimension)
                || !(level.getEntity(entityId) instanceof com.cobblemon.mod.common.entity.pokemon.PokemonEntity pokemon)) return;
        TENACITY.put(pokemon, level.getGameTime() + Math.max(1, ticks));
    }
    public static boolean tenacityFlash(com.cobblemon.mod.common.entity.pokemon.PokemonEntity pokemon) {
        Long until = TENACITY.get(pokemon);
        return until != null && pokemon.level().getGameTime() < until && pokemon.level().getGameTime() % 8 < 4;
    }
    static boolean teraSpent(PokemonEntity body) {
        return teraUsed || body.getAspects().stream().anyMatch(aspect -> aspect.startsWith(TeraCombat.NETWORK_ASPECT));
    }

    private static boolean canPressTera(Minecraft mc) {
        var id = ClientSmash.controlledEntity();
        return mc.player != null && mc.screen == null && mc.level != null && ClientSmash.active() && !restrict()
                && id != null && mc.level.getEntity(id) instanceof PokemonEntity body && !teraSpent(body) && TeraCombat.equipped(mc.player);
    }

    public static boolean restrict() { return cue == Cue.TRANSFER || cue == Cue.LOAD || cue.focus(); }
    public static boolean allow(Cue action) { return !restrict() || cue.accepts(action) && ready; }

    private static boolean canShowInteraction(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.screen == null && !mc.options.hideGui
                && !TALK.isUnbound() && mc.isWindowActive() && !restrict() && !ClientSmash.active();
    }

    private static LivingEntity aimedNpc(Minecraft mc) {
        if (!canShowInteraction(mc) || !mc.level.dimension().equals(PrologueWorld.GYMS)) return null;
        return mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(ProfessorEntity.TALK_DISTANCE),
                        actor -> (actor instanceof ProfessorEntity || actor instanceof com.p1nero.cobblemon.rumblecore.prologue.TrialMerchantEntity
                                || GymStory.talkable(actor))
                                && !GymDialogueCamera.hasActorName(actor, ProfessorRestock.JESSIE)
                                && !GymDialogueCamera.hasActorName(actor, ProfessorRestock.JAMES)
                                && !GymDialogueCamera.hasActorName(actor, TeraChapter.CASSIDY_NAME)
                                && !GymDialogueCamera.hasActorName(actor, TeraChapter.BUTCH_NAME)
                                && DialogueTarget.aimedAt(mc.player, actor))
                .stream().min(Comparator.comparingDouble(mc.player::distanceToSqr)).orElse(null);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isWindowActive()) SCANCODES.clear();
        boolean useTera = drain(TERA) || teraPressed;
        teraPressed = false;
        if (useTera && canPressTera(mc)) {
            // T is also vanilla chat by default. Only consume it when a Tera charge is usable.
            if (TERA.getKey().equals(mc.options.keyChat.getKey())) drain(mc.options.keyChat);
            PacketDistributor.sendToServer(new ArenaPackets.UseTera());
        }
        if (drain(HELD) && mc.player != null && mc.screen == null && ClientSmash.active() && !restrict()) {
            if (HELD.getKey().equals(mc.options.keyDrop.getKey())) drain(mc.options.keyDrop);
            PacketDistributor.sendToServer(new com.p1nero.cobblemon.rumblecore.combat.arena.ArenaPackets.UseHeld());
        }
        if (drain(TALK) && renderedNpc != null && canShowInteraction(mc) && aimedNpc(mc) == renderedNpc) {
            if (TALK.getKey().equals(mc.options.keyInventory.getKey())) drain(mc.options.keyInventory);
            pendingTalk = renderedNpc;
        }
        if (cue == Cue.LOAD && !sentReady && mc.screen == null && mc.level != null
                && mc.level.getEntity(target) instanceof com.cobblemon.mod.common.entity.pokemon.PokemonEntity) {
            sentReady = true;
            PacketDistributor.sendToServer(new ProloguePackets.ArenaReady(target));
        }
        if (cue == Cue.LOCK && ready && !sentLock && BattleCamera.target() instanceof com.cobblemon.mod.common.entity.pokemon.PokemonEntity mon
                && mon.getId() == target) {
            sentLock = true;
            PacketDistributor.sendToServer(new ProloguePackets.TargetLocked(target));
        }
        if (!restrict()) return;
        // Discard competing bindings before other mods can consume them, even if they share the lesson key.
        for (KeyMapping key : mc.options.keyMappings) {
            if (allowsKey(mc.options, key)) continue;
            drain(key);
            key.setDown(false);
        }
    }

    @SubscribeEvent public static void sendTalkAfterMovement(ClientTickEvent.Post event) {
        var actor = pendingTalk;
        pendingTalk = null;
        if (actor != null && canShowInteraction(Minecraft.getInstance())) {
            PacketDistributor.sendToServer(new ProloguePackets.Talk(actor.getId()));
        }
    }

    private static boolean allowsKey(Options options, KeyMapping key) {
        // Keep an exit path (chat/commands) available while game actions are on rails.
        if (key == options.keyChat || key == options.keyCommand) return true;
        if (cue.focus() && (key == options.keyUp || key == options.keyLeft || key == options.keyDown || key == options.keyRight
                || key == options.keyJump || key == options.keySprint || key == options.keyShift)) return true;
        if (!ready) return false;
        return switch (cue) {
            case CHALLENGE -> key == PartySendBinding.INSTANCE;
            case MODE_EXIT, MODE_REENTER -> key == ClientSmash.SMASH;
            case LOCK -> key == BattleCamera.LOCK;
            case ATTACK, HEAVY -> key == options.keyAttack;
            case DODGE -> key == ClientSmash.DODGE;
            case GUARD, PARRY -> key == options.keyUse;
            case MOVE -> {
                boolean match = false;
                for (int i = 0; i < moves; i++) match |= key == options.keyHotbarSlots[i];
                yield match;
            }
            default -> false;
        };
    }

    private static boolean drain(KeyMapping binding) {
        boolean pressed = false;
        while (binding.consumeClick()) pressed = true;
        return pressed;
    }

    /** RTC uses both edge-triggered clicks and held input for attack/use. Do not poll a fixed mouse button. */
    public static boolean held(long window, KeyMapping binding) {
        var key = binding.getKey();
        if (binding.isUnbound() || !binding.isConflictContextAndModifierActive()) return false;
        return switch (key.getType()) {
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
            case KEYSYM -> InputConstants.isKeyDown(window, key.getValue());
            case SCANCODE -> SCANCODES.contains(key.getValue());
        };
    }

    @SubscribeEvent public static void keyboard(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_RELEASE) SCANCODES.remove(event.getScanCode());
        else SCANCODES.add(event.getScanCode());
        if (event.getAction() == GLFW.GLFW_PRESS && TERA.isActiveAndMatches(InputConstants.getKey(event.getKey(), event.getScanCode()))
                && canPressTera(Minecraft.getInstance())) teraPressed = true;
    }

    public static void allowTutorialChat() {
        var mc = Minecraft.getInstance();
        var o = mc.options;
        boolean chat = drain(o.keyChat), command = drain(o.keyCommand);
        if ((chat || command) && mc.screen == null) mc.setScreen(new ChatScreen(command ? "/" : ""));
    }

    @SubscribeEvent public static void projectInteraction(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        CaptureHud.project(event);
        var mc = Minecraft.getInstance();
        var doctor = aimedNpc(mc);
        renderedNpc = null;
        InteractionHud.clear();
        if (doctor != focusNpc) {
            focusNpc = doctor;
            focusNpcAt = System.nanoTime();
        }
        if (doctor == null) return;
        InteractionHud.project(event, doctor);
    }

    @SubscribeEvent public static void hideOverlappingHud(RenderGuiLayerEvent.Pre event) {
        if (net.neoforged.neoforge.client.gui.VanillaGuiLayers.FOOD_LEVEL.equals(event.getName())) {
            event.setCanceled(true);
            return;
        }
        // RTC already suppresses the vanilla hotbar, but Ares draws its own hotbar and player HUD.
        var layer = event.getName();
        boolean arcQuestHud = QUEST_STATUS.equals(layer) || QUEST_TRACKER.equals(layer);
        // ArcQuest draws the right-hand tracker separately from the quest HUD and world marker layer.
        if ((ClientSmash.active() && (ARES_HUD.equals(layer) || arcQuestHud || QUEST_MARKERS.equals(layer)))
                || (cue != Cue.NONE && (INSTANCE_STATUS.equals(layer) || arcQuestHud))) event.setCanceled(true);
    }

    public static boolean hideInspectionHud() {
        var mc = Minecraft.getInstance();
        return mc.screen == null && (cue.focus() || cue == Cue.SPAR || cue == Cue.CLEAR
                || focusNpc != null && mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
                        && hit.getEntity() == focusNpc);
    }

    public static void renderOverlay(GuiGraphics graphics) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (canShowInteraction(mc) && focusNpc != null) {
            float opacity = Math.min(1, (System.nanoTime() - focusNpcAt) / 170_000_000f);
            if (InteractionHud.render(graphics, TALK.getTranslatedKeyMessage(), opacity)) renderedNpc = focusNpc;
        }
        InteractionHud.clear();
        CaptureHud.render(graphics);
        if (cue == Cue.TRANSFER || cue == Cue.LOAD) drawTransfer(graphics);
        else if (cue.focus() || cue == Cue.SPAR || cue == Cue.CLEAR || cue == Cue.MEGA_ITEM || cue == Cue.MEGA_BATTLE
                || cue == Cue.TERA_ITEM || cue == Cue.TERA_BATTLE) {
            TrainingHud.render(graphics, cue, ready, accepted, moves, changedAt, acceptedAt);
        }
        TrainingHud.renderHeld(graphics);
    }

    private static void drawTransfer(GuiGraphics g) {
        int w = g.guiWidth(), h = g.guiHeight();
        long elapsed = (System.nanoTime() - changedAt) / 1_000_000L;
        int inset = cue == Cue.LOAD ? 0 : Math.max(0, h / 2 - (int) (elapsed * h / 540));
        g.fill(0, 0, w, h / 2 - inset, 0xff091524);
        g.fill(0, h / 2 + inset, w, h, 0xff091524);
        for (int y = 4; y < h; y += 6) {
            if (y < h / 2 - inset || y > h / 2 + inset) g.fill(0, y, w, y + 1, 0x2334cadd);
        }
        g.fill(0, h / 2 - inset - 1, w, h / 2 - inset + 1, 0xff70e8ee);
        g.fill(0, h / 2 + inset - 1, w, h / 2 + inset + 1, 0xff70e8ee);
        if (inset > 0) return;
        var font = Minecraft.getInstance().font;
        g.drawCenteredString(font, tr("arena"), w / 2, h / 2 - 28, 0xffe4faff);
        g.drawCenteredString(font, tr("link"), w / 2, h / 2 + 12, 0xff78dedc);
        int span = Math.min(144, w - 16), x = (w - span) / 2;
        g.fill(x, h / 2 + 27, x + span, h / 2 + 30, 0xff21475d);
        int head = x + (int)((elapsed / 5) % Math.max(1, span - 20));
        g.fill(head, h / 2 + 27, Math.min(x + span, head + 20), h / 2 + 30, 0xff70e8ee);
    }

    private static Component tr(String key, Object... args) { return Component.translatable(PREFIX + key, args); }
}
