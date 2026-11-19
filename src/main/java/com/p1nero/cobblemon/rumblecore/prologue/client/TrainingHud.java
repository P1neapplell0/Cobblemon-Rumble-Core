package com.p1nero.cobblemon.rumblecore.prologue.client;

import com.cobblemon.mod.common.client.keybind.keybinds.PartySendBinding;
import com.johnn.realtimecobblemon.client.BattleCamera;
import com.johnn.realtimecobblemon.client.ClientSmash;
import com.mojang.blaze3d.systems.RenderSystem;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import com.p1nero.cobblemon.rumblecore.combat.arena.TeraCombat;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;

import static com.p1nero.cobblemon.rumblecore.prologue.ProloguePackets.Cue;

final class TrainingHud {
    private static final net.minecraft.resources.ResourceLocation PORTRAIT = PrologueWorld.id("textures/gui/prof_p1nero_prologue.png");
    private static final net.minecraft.resources.ResourceLocation GIOVANNI = PrologueWorld.id("textures/entity/npc/ground_leader.png");
    private static final net.minecraft.resources.ResourceLocation MAY = PrologueWorld.id("textures/entity/npc/xiao_yao.png");
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> MEGA_STONES = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.parse("mega_showdown:mega_stone"));

    private TrainingHud() {}

    static void render(GuiGraphics g, Cue cue, boolean ready, boolean accepted, int moves, long changedAt, long acceptedAt) {
        var mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui) return;
        var font = mc.font;
        float age = seconds(changedAt), confirmed = seconds(acceptedAt);
        boolean may = cue == Cue.MEGA_ITEM || cue == Cue.MEGA_BATTLE;
        boolean giovanni = cue == Cue.TERA_ITEM || cue == Cue.TERA_BATTLE;
        boolean head = may || giovanni;
        float disappear = cue == Cue.SPAR || cue == Cue.MEGA_BATTLE || cue == Cue.TERA_BATTLE ? fade(5.5f, 6.2f, age) : accepted ? fade(.9f, 1.48f, confirmed) : 1;
        float opacity = smooth(age / .24f) * disappear;
        if (opacity < .03f) return;
        boolean hasPortrait = head || cue == Cue.CHALLENGE || cue == Cue.MODE_EXIT || cue.defensive() || cue == Cue.SPAR || cue == Cue.CLEAR;
        int portraitH = Math.min(head ? 32 : 68, g.guiHeight() / 4);
        int portraitW = head ? portraitH : Math.round(portraitH * 816f / 1290f);
        int inset = hasPortrait ? portraitW + 8 : 0;
        int y = Math.min(48, Math.round(g.guiHeight() * .21f));
        int width = Math.max(40, Math.min(265, g.guiWidth() - 24 - inset));
        String step = cue.name().toLowerCase(Locale.ROOT);
        var keys = keys(mc, cue, moves);
        var title = tr(step + ".title");
        var body = tr(step, keysText(keys, cue));
        // Size the group from the finished text so neither the portrait nor the lines slide while typing.
        var lines = font.getSplitter().splitLines(body, width, net.minecraft.network.chat.Style.EMPTY);
        int textWidth = font.width(title);
        for (FormattedText line : lines) textWidth = Math.max(textWidth, font.width(line));
        int groupX = (g.guiWidth() - textWidth - inset) / 2;
        int center = groupX + inset + textWidth / 2;
        g.drawCenteredString(font, title, center, y, alpha(0x9bf4eb, opacity));
        int letters = Math.max(0, (int)((age - .10f) * 56));
        int lineY = y + 16;
        for (FormattedText line : lines) {
            String text = line.getString();
            int length = text.codePointCount(0, text.length());
            int shown = Math.min(letters, length);
            if (shown > 0) {
                String visible = text.substring(0, text.offsetByCodePoints(0, shown));
                g.drawString(font, visible, center - font.width(text) / 2, lineY, alpha(0xffffff, opacity));
            }
            letters = Math.max(0, letters - length);
            lineY += font.lineHeight + 2;
        }

        if (accepted) {
            g.drawCenteredString(font, tr("accepted"), center, lineY + 7, alpha(0xb8ffbc, opacity));
        } else if (ready && (cue.focus() || cue == Cue.MEGA_ITEM || cue == Cue.TERA_ITEM)) {
            drawKeys(g, keys, g.guiWidth() / 2, lineY + 8, age, opacity);
        }

        if (hasPortrait) {
            float speak = smooth((age - .08f) / .35f) * fade(2.6f, 3.45f, age) * disappear;
            if (speak < .03f) return;
            g.flush();
            RenderSystem.enableBlend();
            g.setColor(1, 1, 1, speak);
            int portraitY = Math.max(6, (y + lineY - portraitH) / 2);
            int at = portraitY + Math.round((1 - speak) * 5);
            if (head) {
                var skin = giovanni ? GIOVANNI : MAY;
                g.blit(skin, groupX, at, portraitW, portraitH, 8, 8, 8, 8, 64, 64);
                g.blit(skin, groupX, at, portraitW, portraitH, 40, 8, 8, 8, 64, 64);
            } else {
                g.blit(PORTRAIT, groupX, at, portraitW, portraitH, 0, 0, 816, 1290, 816, 1290);
            }
            g.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }
    }

    static void renderHeld(GuiGraphics g) {
        var mc = Minecraft.getInstance();
        var id = ClientSmash.controlledEntity();
        if (mc.screen != null || mc.level == null || !ClientSmash.active() || id == null
                || !(mc.level.getEntity(id) instanceof com.cobblemon.mod.common.entity.pokemon.PokemonEntity body)) return;
        var stack = body.getShownItem();
        if (stack.isEmpty()) stack = body.getPokemon().heldItem();
        var items = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        var item = items.getKey(stack.getItem()).toString();
        boolean held = stack.is(MEGA_STONES) || item.equals("cobblemon:sitrus_berry") || item.equals("cobblemon:oran_berry");
        int x = g.guiWidth() / 2 + 72, y = g.guiHeight() - 48;
        if (held) drawBattleItem(g, x, y, stack, PrologueClient.HELD, false);
        if (mc.player != null && TeraCombat.equipped(mc.player)) {
            var orb = items.get(TeraCombat.ORB).getDefaultInstance();
            int off = held ? 27 : 0;
            // At narrow scales put the second slot on the other side of the moves rather than clipping it.
            int teraX = x + off + 22 <= g.guiWidth() ? x + off : Math.max(2, g.guiWidth() / 2 - 94);
            drawBattleItem(g, teraX, y, orb, PrologueClient.TERA, PrologueClient.teraSpent(body));
        }
    }

    private static void drawBattleItem(GuiGraphics g, int x, int y, net.minecraft.world.item.ItemStack stack, KeyMapping key, boolean spent) {
        g.fill(x, y, x + 22, y + 22, 0xd51a232b);
        g.renderOutline(x, y, 22, 22, spent ? 0xdd718283 : 0xdd9beadf);
        g.renderItem(stack, x + 3, y + 3);
        if (spent) g.fill(x + 1, y + 1, x + 21, y + 21, 0xaa131b24);
        g.drawCenteredString(Minecraft.getInstance().font, key.getTranslatedKeyMessage(), x + 11, y + 24, spent ? 0xff91a9ad : 0xfffafafa);
    }

    private static void drawKeys(GuiGraphics g, KeyMapping[] keys, int center, int y, float age, float opacity) {
        var font = Minecraft.getInstance().font;
        if (keys.length == 0) return;
        int gap = 7;
        int max = Math.max(18, (g.guiWidth() - 20 - gap * (keys.length - 1)) / keys.length);
        String[] labels = new String[keys.length];
        int[] widths = new int[keys.length];
        int span = gap * (keys.length - 1);
        for (int i = 0; i < keys.length; i++) {
            labels[i] = font.plainSubstrByWidth(keys[i].getTranslatedKeyMessage().getString(), max - 10);
            widths[i] = Math.max(17, font.width(labels[i]) + 10);
            span += widths[i];
        }
        int x = center - span / 2;
        float wave = (age % 1.2f) / 1.2f;
        for (int i = 0; i < keys.length; i++) {
            int w = widths[i];
            int halo = 1 + (int)(4 * wave);
            g.renderOutline(x - halo, y - halo, w + halo * 2, 17 + halo * 2,
                    alpha(0x83ffe6, opacity * (1 - wave) * .8f));
            g.fill(x + 1, y + 1, x + w - 1, y + 16, alpha(0x132127, opacity * .85f));
            g.renderOutline(x, y, w, 17, alpha(0x9beadf, opacity));
            g.drawCenteredString(font, labels[i], x + w / 2, y + 5, alpha(0xffffff, opacity));
            x += w + gap;
        }
    }

    private static KeyMapping[] keys(Minecraft mc, Cue cue, int moves) {
        var o = mc.options;
        return switch (cue) {
            case CHALLENGE -> new KeyMapping[]{PartySendBinding.INSTANCE};
            case MODE_EXIT, MODE_REENTER -> new KeyMapping[]{ClientSmash.SMASH};
            case MOVEMENT -> new KeyMapping[]{o.keyUp, o.keyLeft, o.keyDown, o.keyRight, o.keyJump};
            case LOCK -> new KeyMapping[]{BattleCamera.LOCK};
            case ATTACK, HEAVY -> new KeyMapping[]{o.keyAttack};
            case GUARD, PARRY -> new KeyMapping[]{o.keyUse};
            case MOVE -> Arrays.copyOf(o.keyHotbarSlots, moves);
            case MEGA_ITEM -> new KeyMapping[]{PrologueClient.HELD};
            case TERA_ITEM -> new KeyMapping[]{PrologueClient.TERA};
            default -> new KeyMapping[0];
        };
    }

    private static Object[] keysText(KeyMapping[] keys, Cue cue) {
        if (keys.length == 0) return new Object[0];
        if (cue == Cue.MOVEMENT) return new Object[]{join(keys, 4), keys[4].getTranslatedKeyMessage()};
        return new Object[]{join(keys, keys.length)};
    }

    private static Component join(KeyMapping[] keys, int count) {
        var result = Component.empty();
        for (int i = 0; i < count; i++) {
            if (i > 0) result.append(" / ");
            result.append(keys[i].getTranslatedKeyMessage());
        }
        return result;
    }

    private static int alpha(int rgb, float opacity) {
        return Math.round(Mth.clamp(opacity, 0, 1) * 255) << 24 | rgb;
    }

    private static float seconds(long since) { return (System.nanoTime() - since) / 1_000_000_000f; }
    private static float smooth(float value) { float t = Mth.clamp(value, 0, 1); return t * t * (3 - 2 * t); }
    private static float fade(float start, float end, float seconds) { return 1 - smooth((seconds - start) / (end - start)); }
    private static Component tr(String key, Object... args) { return Component.translatable("rumble.prologue.lesson." + key, args); }
}
