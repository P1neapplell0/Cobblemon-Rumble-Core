package com.p1nero.cobblemon.rumblecore.client.combat;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.entity.PokemonClientDelegate;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.logging.LogUtils;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaIntro;
import com.p1nero.cobblemon.rumblecore.client.ui.ore.TrialPortraits;
import com.p1nero.cobblemon.rumblecore.gym.GymStory;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.joml.Quaternionf;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class ArenaIntroOverlay {
    public static final String TRAINER_PREVIEW_TAG = "rumble_arena_intro_preview";
    private static final long FACE_OFF_END = 4_150;
    private static final long LENGTH = ArenaIntro.MINIMUM_TICKS * 50L;
    private static final long TIMEOUT = 90_000;
    private static final int GOLD = 0xfff1bf;
    private static Scene active;

    private ArenaIntroOverlay() {}

    static boolean playingFor(ResourceLocation dimension) {
        return active != null && active.packet.destination().equals(dimension)
                && Util.getMillis() - active.startedAt <= TIMEOUT;
    }

    public static void start(ArenaIntro.Start packet) {
        active = new Scene(packet, Util.getMillis());
        ArenaCinematicAudio.play("arena.intro", .72f);
    }

    public static void transition(ArenaIntro.Transition packet) {
        if (active == null || !active.packet.match().equals(packet.match())) return;
        if (packet.state() == ArenaIntro.CANCEL) active = null;
        else if (packet.state() == ArenaIntro.READY && active.readyAt == 0) active.readyAt = Util.getMillis();
    }

    static void finishForKnockout(ResourceLocation dimension) {
        if (active != null && active.packet.destination().equals(dimension)) active = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hud(RenderGuiEvent.Pre event) {
        if (Minecraft.getInstance().screen == null && renderSafely(event.getGuiGraphics())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void screen(ScreenEvent.Render.Post event) { renderSafely(event.getGuiGraphics()); }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { active = null; }

    private static boolean renderSafely(GuiGraphics graphics) {
        if (active == null) return false;
        try {
            render(graphics);
            return active != null;
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("Could not draw arena introduction for {}", active.packet.match(), e);
            active = null;
            return false;
        }
    }

    private static void render(GuiGraphics graphics) {
        var scene = active;
        if (scene == null) return;
        long now = Util.getMillis();
        Minecraft client = Minecraft.getInstance();
        boolean ready = scene.readyAt != 0 && client.level != null
                && client.level.dimension().location().equals(scene.packet.destination())
                && !(client.screen instanceof ReceivingLevelScreen);
        if (ready && scene.finishAt == 0) scene.finishAt = Math.max(scene.startedAt + FACE_OFF_END, now);
        long elapsed = scene.finishAt == 0 ? Math.min(FACE_OFF_END, now - scene.startedAt)
                : Math.min(FACE_OFF_END, now - scene.startedAt) + Math.max(0, now - scene.finishAt);
        if (elapsed >= LENGTH || now - scene.startedAt > TIMEOUT) {
            active = null;
            return;
        }
        if (client.level != null && scene.level != client.level) scene.createModels(client.level);

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int middle = width / 2;
        float entranceLeft = ease((elapsed - 100) / 730f);
        float entranceRight = ease((elapsed - 290) / 730f);
        boolean fight = elapsed >= 4_320;
        boolean rosterIntro = scene.packet.left().isPlayer() && scene.packet.right().isPlayer()
                && !scene.packet.leftSupport().isEmpty() && !scene.packet.rightSupport().isEmpty();
        float exit = ease((elapsed - 4_870) / 530f);
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 350);
        try {
            float impact = Math.max(beat(elapsed, 820), beat(elapsed, 4_320));
            ArenaIntroStage.draw(graphics, width, height, (now - scene.startedAt) / 1000f, impact);
            graphics.pose().translate(0, 0, 2);
            backdrop(graphics, scene.packet.left(), width, height, false, entranceLeft);
            backdrop(graphics, scene.packet.right(), width, height, true, entranceRight);
            if (rosterIntro) {
                drawFrontier(graphics, scene.faces, scene.packet.leftSupport(), width, height, entranceLeft, exit, elapsed, 1);
                drawFrontier(graphics, scene.faces, scene.packet.rightSupport(), width, height, entranceRight, exit, elapsed, -1);
            } else {
                drawLineup(graphics, scene.left, scene.leftSupport, 0, middle, height, entranceLeft, exit, elapsed, 1);
                drawLineup(graphics, scene.right, scene.rightSupport, middle, width, height, entranceRight, exit, elapsed, -1);
            }
            if (elapsed >= 820 && !scene.faceOffSound) {
                scene.faceOffSound = true;
                ArenaCinematicAudio.play("arena.face_off", .85f);
            }
            if (fight && !scene.fightSound) {
                scene.fightSound = true;
                ArenaCinematicAudio.play("arena.fight", .75f);
            }
            scene.cry(client, elapsed);
            graphics.pose().translate(0, 0, 230);
            graphics.fillGradient(0, height * (rosterIntro ? 83 : 62) / 100, width, height, 0x00101523, 0xf6080b14);
            graphics.fillGradient(0, 0, width, height / 7, 0xaf0a0b17, 0x000a0b17);
            int inset = Math.max(8, width / 32);
            graphics.fill(inset, 8, inset + 4, 12, 0xffffdb84);
            graphics.drawString(client.font, "RUMBLE ARENA  /  01", inset + 9, 6, 0xfff5e7c2);
            drawName(graphics, scene.packet.left(), inset, height, middle, false, entranceLeft);
            drawName(graphics, scene.packet.right(), width - inset, height, middle, true, entranceRight);
            if (elapsed >= 820 && (elapsed < FACE_OFF_END + 100 || fight)) {
                float hit = ease((elapsed - (fight ? 4_320 : 820)) / 230f);
                float size = Math.min(width, height) / (fight ? 48f : 42f);
                Component title = fight ? Component.translatable("rumble.arena.intro.fight") : Component.literal("VS");
                float centerY = height * (rosterIntro ? .48f : .47f);
                float scale = Math.max(1, size) * (1 + (1 - hit) * .25f);
                label(graphics, title, middle + 1, centerY + scale * .84f, scale, 0xff472342, true);
                label(graphics, title, middle, centerY + scale * .42f, scale, 0xffc36d51, true);
                label(graphics, title, middle, centerY, scale, GOLD, true);
                tag(graphics, Component.literal(fight ? "ROUND 01" : "FACE OFF"), middle, centerY - scale * 5.5f);
                if (!fight) tag(graphics, Component.literal("ROUND 01"), middle, centerY + scale * 6.5f);
            }
            if (!ready && elapsed >= FACE_OFF_END) {
                tag(graphics, Component.translatable("rumble.arena.intro.loading"), middle, height - 18);
            }
            if (impact > 0) graphics.fill(0, 0, width, height, ((int) (impact * 38) << 24) | 0xffe3a8);
            if (exit > 0) graphics.fill(0, 0, width, height, ((int) (exit * 255) << 24) | 0x080b14);
        } finally {
            graphics.flush();
            graphics.pose().popPose();
        }
    }

    private static float beat(long elapsed, int at) {
        return elapsed < at ? 0 : 1 - ease((elapsed - at) / 260f);
    }

    private static void backdrop(GuiGraphics graphics, ArenaIntro.Portrait portrait, int width, int height,
                                 boolean right, float entrance) {
        var font = Minecraft.getInstance().font;
        var text = portrait.name().copy().withStyle(net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.ITALIC);
        float scale = Math.min(height / 30f, width * .51f / Math.max(1, font.width(text)));
        int x = (int) (width * (right ? .75f : .25f) + (1 - entrance) * width * (right ? .23f : -.23f));
        graphics.enableScissor(right ? width / 2 : 0, 0, right ? width : width / 2, height);
        try {
            label(graphics, text, x, height * .29f, scale, 0x33e4cbee, false);
        } finally {
            graphics.disableScissor();
        }
    }

    private static void tag(GuiGraphics graphics, Component text, int x, float y) {
        int half = Minecraft.getInstance().font.width(text) / 2 + 5;
        graphics.fill(x - half, (int) y - 5, x + half, (int) y + 6, 0xe6191029);
        label(graphics, text, x, y, 1, 0xffffe5a9, false);
    }

    private static void drawLineup(GuiGraphics graphics, LivingEntity lead, java.util.List<LivingEntity> support,
                                   int minX, int maxX, int height, float entrance, float exit, long elapsed, int direction) {
        if (support.isEmpty()) {
            drawModel(graphics, lead, minX, maxX, height, 1, entrance, exit, elapsed, direction);
            return;
        }
        int panel = maxX - minX;
        // Reserve room for the VS title, and keep every trainer on the same floor.
        int innerGap = panel / 7;
        int leadWidth = (panel - innerGap) / 2;
        int supportWidth = panel - innerGap - leadWidth;
        int supportX = direction == 1 ? minX : maxX - supportWidth;
        for (int i = 0; i < support.size(); i++) {
            int from = supportX + supportWidth * i / support.size();
            int to = supportX + supportWidth * (i + 1) / support.size();
            drawModel(graphics, support.get(i), from, to, height, .92f, entrance, exit, elapsed, direction);
        }
        int leadX = direction == 1 ? maxX - innerGap - leadWidth : minX + innerGap;
        drawModel(graphics, lead, leadX, leadX + leadWidth, height, .92f, entrance, exit, elapsed, direction);
    }

    private static void drawModel(GuiGraphics graphics, LivingEntity model, int minX, int maxX, int height,
                                  float size, float entrance, float exit, long elapsed, int direction) {
        if (model == null || entrance <= 0) return;
        float panel = maxX - minX;
        float centerX = (minX + maxX) * .5f - direction * panel * (1.04f * (1 - entrance) + .14f * exit);
        boolean human = !(model instanceof PokemonEntity);
        float modelHeight = Math.max(.5f, model.getBbHeight());
        float scale = Math.max(1, size * Math.min(height * (human ? .6f : .68f) / modelHeight,
                panel / Math.max(.65f, model.getBbWidth()) * (human ? .54f : .86f)));
        float feet = height * .76f;
        if (human) drawContactShadow(graphics, minX, maxX, height, centerX, feet, scale);
        renderModel(graphics, model, minX, 3, maxX, height - 3, centerX, feet, scale, elapsed,
                180 - direction * (25 + 38 * (1 - entrance)), direction);
    }

    private static void drawContactShadow(GuiGraphics graphics, int minX, int maxX, int height,
                                          float x, float feet, float scale) {
        int centerY = Math.round(feet + scale * .0125f);
        int radiusY = Math.max(2, Math.round(scale * .06f));
        float radiusX = scale * .38f;
        graphics.enableScissor(minX, 3, maxX, height - 3);
        try {
            for (int y = -radiusY; y <= radiusY; y++) {
                for (int layer = 3; layer >= 1; layer--) {
                    float edge = y / (radiusY * (layer + 1) / 4f);
                    if (Math.abs(edge) >= 1) continue;
                    int span = Math.round(radiusX * (layer + 1) / 4 * Mth.sqrt(1 - edge * edge));
                    int centerX = Math.round(x);
                    graphics.fill(centerX - span, centerY + y, centerX + span + 1, centerY + y + 1, 0x35050710);
                }
            }
        } finally {
            graphics.disableScissor();
        }
    }

    /** Same slope and origin as the split in arena_intro.fsh (screen Y increases downwards). */
    private static int frontierSeam(int width, int height, float row) {
        return Math.round(width * (.5f - (row / height - .5f) * .17f));
    }

    private static void drawFrontier(GuiGraphics g, TrialPortraits faces, java.util.List<ArenaIntro.Portrait> picks,
                                     int width, int height, float entrance, float exit, long elapsed, int direction) {
        if (entrance <= 0) return;
        boolean left = direction > 0;
        int accent = left ? 0xff8ae9f4 : 0xffff9cce;
        int rowH = Math.max(22, height * 18 / 100), stride = rowH + Math.max(4, height / 50);
        int top = height / 2 - (rowH + 2 * stride) / 2;
        int columnWidth = Math.round(width * .29f), gutter = Math.round(width * .105f);
        int shift = Math.round(-direction * width * ((1 - entrance) * .49f + exit * .20f));
        var font = Minecraft.getInstance().font;
        for (int i = 0; i < 3; i++) {
            int y = top + i * stride;
            int inner = frontierSeam(width, height, y + rowH / 2f) - direction * gutter + shift;
            int x = left ? inner - columnWidth : inner;
            for (int line = 0; line < rowH; line++) {
                int skew = frontierSeam(width, height, y + line) - frontierSeam(width, height, y + rowH / 2f);
                int from = x + skew, to = from + columnWidth;
                int fill = line == 0 || line == rowH - 1 ? accent : left ? 0xe01a4359 : 0xe04e2845;
                g.fill(from, y + line, to, y + line + 1, fill);
                g.fill(left ? to - 2 : from, y + line, left ? to : from + 2, y + line + 1, accent);
            }
            int size = Math.min(rowH - 8, columnWidth / 3), faceX = left ? x + 13 : x + columnWidth - 13 - size;
            int faceY = y + (rowH - size) / 2;
            var pick = i < picks.size() ? picks.get(i) : null;
            if (size >= 5 && (pick == null || !FrontierConsole.partyFace(g, faces, pick.species(), Set.copyOf(pick.aspects()),
                    faceX, faceY, size, elapsed / 50f % 1, !left, accent))) {
                FrontierConsole.placeholder(g, font, faceX, faceY, size);
            }
            int textArea = columnWidth - size - 24;
            int textX = left ? faceX + size + textArea / 2 : faceX - textArea / 2;
            float scale = Math.clamp(rowH / 19f, 1.2f, 3.75f);
            label(g, Component.literal((i + 1) + "P").withStyle(net.minecraft.ChatFormatting.BOLD), textX, y + rowH * .38f, scale, accent, true);
            if (pick != null) FrontierConsole.center(g, font, pick.name(), textX, y + rowH * 3 / 4 - 3, textArea - 5, 0xfff2f8fd);
        }
    }

    /** A screen-local model cache; only draft Pokémon get their own display entity. */
    static final class TeamModels {
        private final java.util.Map<ResourceLocation, LivingEntity> models = new java.util.HashMap<>();
        private ClientLevel level;

        void draw(GuiGraphics g, ResourceLocation species, int x, int y, int w, int h, boolean left) {
            if (w < 5 || h < 5) return;
            var current = Minecraft.getInstance().level;
            if (level != current) {
                level = current;
                models.clear();
            }
            if (level == null) return;
            var model = models.computeIfAbsent(species, id -> create(ArenaIntro.Portrait.pokemon(id, Component.empty(), java.util.List.of()), level));
            if (model == null) return;
            float scale = Math.min((h - 3) * .77f / Math.max(.5f, model.getBbHeight()), (w - 2) * .72f / Math.max(.55f, model.getBbWidth()));
            int facing = left ? 1 : -1;
            renderModel(g, model, x, y, x + w, y + h, x + w * .5f, y + h - 2, scale,
                    Util.getMillis() % 60_000L, 180 - facing * 34, facing);
        }
    }

    private static void renderModel(GuiGraphics graphics, LivingEntity model, int minX, int top, int maxX, int bottom,
                                    float centerX, float feet, float scale, long elapsed, float yaw, int direction) {
        model.yBodyRot = model.yBodyRotO = yaw;
        model.yHeadRot = model.yHeadRotO = yaw - direction * 9;
        model.setYRot(yaw);
        model.setXRot(-3);
        model.tickCount = (int) (elapsed / 50);
        if (model instanceof PokemonEntity pokemon && pokemon.getDelegate() instanceof PokemonClientDelegate delegate) {
            delegate.updateAge(model.tickCount);
        }
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var orientation = new Quaternionf(dispatcher.cameraOrientation());
        graphics.enableScissor(minX, top, maxX, bottom);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(centerX, feet, 60);
            graphics.pose().scale(scale, scale, -scale);
            graphics.pose().mulPose(new Quaternionf().rotateZ(Mth.PI));
            Lighting.setupForEntityInInventory();
            dispatcher.overrideCameraOrientation(new Quaternionf().rotateY(Mth.PI));
            dispatcher.setRenderShadow(false);
            dispatcher.render(model, 0, 0, 0, yaw, 1, graphics.pose(), graphics.bufferSource(), LightTexture.FULL_BRIGHT);
            graphics.flush();
        } finally {
            dispatcher.setRenderShadow(true);
            dispatcher.overrideCameraOrientation(orientation);
            Lighting.setupFor3DItems();
            graphics.pose().popPose();
            graphics.disableScissor();
        }
    }

    private static void drawName(GuiGraphics graphics, ArenaIntro.Portrait portrait, int x, int height,
                                 int panelWidth, boolean right, float entrance) {
        if (entrance < .3f) return;
        var font = Minecraft.getInstance().font;
        var kind = Component.translatable(portrait.isPlayer() ? "rumble.arena.intro.player"
                : portrait.isTrainer() ? "rumble.arena.intro.trainer" : "rumble.arena.intro.pokemon");
        int color = right ? 0xffff99d2 : 0xff82d8ff;
        int y = height * 87 / 100;
        int limit = Math.max(1, panelWidth - 20);
        var name = font.substrByWidth(portrait.name(), limit).getString();
        graphics.drawString(font, kind, right ? x - font.width(kind) : x, y - 12, color);
        graphics.drawString(font, name, right ? x - font.width(name) : x, y, 0xfff8f3ff);
        graphics.fill(right ? x - Math.min(limit, 67) : x, y + 11, right ? x : x + Math.min(limit, 67), y + 12, color);
    }

    private static void label(GuiGraphics graphics, Component text, float x, float y, float scale, int color, boolean italic) {
        var font = Minecraft.getInstance().font;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y - 4 * scale, 0);
        graphics.pose().scale(scale, scale, 1);
        var display = italic ? text.copy().withStyle(net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.ITALIC) : text;
        graphics.drawString(font, display, -font.width(display) / 2, 0, color, false);
        graphics.pose().popPose();
    }

    private static float ease(float progress) {
        float remaining = 1 - Mth.clamp(progress, 0, 1);
        return 1 - remaining * remaining * remaining;
    }

    private static LivingEntity create(ArenaIntro.Portrait portrait, ClientLevel level) {
        if (portrait.isTrainer()) {
            var trainer = GymStory.trainerModel(level, portrait.aspects().getFirst());
            trainer.addTag(TRAINER_PREVIEW_TAG);
            trainer.setHideNameTag(true);
            trainer.setCustomNameVisible(false);
            return trainer;
        }
        if (portrait.isPlayer()) {
            var connection = Minecraft.getInstance().getConnection();
            var info = connection == null ? null : connection.getPlayerInfo(portrait.player());
            var profile = info != null ? info.getProfile() : new GameProfile(portrait.player(), portrait.name().getString());
            return new PortraitPlayer(level, profile);
        }
        var species = PokemonSpecies.getByIdentifier(portrait.species());
        if (species == null) return null;
        var pokemon = new Pokemon();
        pokemon.setSpecies(species);
        pokemon.setForcedAspects(Set.copyOf(portrait.aspects()));
        pokemon.updateAspects();
        var entity = new PokemonEntity(level, pokemon, CobblemonEntities.POKEMON);
        entity.getEntityData().set(PokemonEntity.getASPECTS(), pokemon.getAspects());
        entity.refreshDimensions();
        return entity;
    }

    private static final class PortraitPlayer extends RemotePlayer {
        PortraitPlayer(ClientLevel level, GameProfile profile) {
            super(level, profile);
            getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f);
        }
    }

    private static final class Scene {
        final ArenaIntro.Start packet;
        final long startedAt;
        long readyAt, finishAt;
        ClientLevel level;
        LivingEntity left, right;
        java.util.List<LivingEntity> leftSupport = java.util.List.of();
        java.util.List<LivingEntity> rightSupport = java.util.List.of();
        final TrialPortraits faces = new TrialPortraits();
        boolean cried, faceOffSound, fightSound;

        Scene(ArenaIntro.Start packet, long startedAt) {
            this.packet = packet;
            this.startedAt = startedAt;
        }

        void createModels(ClientLevel level) {
            this.level = level;
            boolean frontier = packet.left().isPlayer() && packet.right().isPlayer()
                    && !packet.leftSupport().isEmpty() && !packet.rightSupport().isEmpty();
            left = frontier ? null : create(packet.left(), level);
            right = frontier ? null : create(packet.right(), level);
            if (!frontier) {
                leftSupport = packet.leftSupport().stream().map(portrait -> create(portrait, level)).toList();
                rightSupport = packet.rightSupport().stream().map(portrait -> create(portrait, level)).toList();
            }
        }

        void cry(Minecraft client, long elapsed) {
            if (cried || elapsed < 1_430 || !(right instanceof PokemonEntity pokemon)
                    || !(pokemon.getDelegate() instanceof PokemonClientDelegate delegate)) return;
            cried = true;
            delegate.cry();
            var species = packet.right().species();
            var sound = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(
                    species.getNamespace(), "pokemon." + species.getPath() + ".cry"));
            client.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1, .72f));
        }
    }
}
