package com.p1nero.cobblemon.rumblecore.compat.legendarymonuments.dialogue;

import com.cobblemon.mod.common.api.dialogue.*;
import com.cobblemon.mod.common.api.dialogue.input.*;
import com.jorgaomc.legendarymonuments.entities.CalyrexNpcManager;
import com.p1nero.cobblemon.rumblecore.compat.legendarymonuments.localization.MonumentText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Collects only the output of the current upstream call. Cobblemon owns the open screen's lifetime. */
public final class CalyrexConversation {
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();

    private CalyrexConversation() {}

    public static <T> T run(ServerPlayer player, Supplier<T> upstream) {
        Frame previous = CURRENT.get();
        var frame = new Frame(player);
        CURRENT.set(frame);
        T result;
        try {
            result = upstream.get();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
        if (!frame.lines.isEmpty() || !frame.options.isEmpty()) {
            DialogueManager.stopDialogue(player);
            DialogueManager.startDialogue(player, page(frame.lines, frame.options));
        }
        return result;
    }

    public static boolean line(ServerPlayer player, String text, boolean objective) {
        Frame frame = frameFor(player);
        if (frame == null) return false;
        var line = MonumentText.literal(text);
        if (objective) line.withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC);
        frame.lines.add(new WrappedDialogueText(line));
        return true;
    }

    public static boolean options(ServerPlayer player, UUID npc, String firstId, String firstLabel,
            String secondId, String secondLabel) {
        Frame frame = frameFor(player);
        if (frame == null) return false;
        frame.options = List.of(option(npc, firstId, firstLabel), option(npc, secondId, secondLabel));
        return true;
    }

    private static Frame frameFor(ServerPlayer player) {
        Frame frame = CURRENT.get();
        return frame != null && frame.player == player ? frame : null;
    }

    private static DialogueOption option(UUID npc, String id, String label) {
        return new DialogueOption(new WrappedDialogueText(MonumentText.literal(label)), id, (dialogue, ignored) -> {
            ServerPlayer player = dialogue.getPlayerEntity();
            dialogue.close();
            if (player.getServer() != null) CalyrexNpcManager.handleOption(player.getServer(), player, npc, id);
        }, dialogue -> true, dialogue -> true);
    }

    static Dialogue page(List<DialogueText> lines, List<DialogueOption> options) {
        var page = new DialoguePage();
        page.setId("calyrex");
        page.setSpeaker("calyrex");
        page.setLines(lines.isEmpty() ? List.of(new WrappedDialogueText(Component.translatable("lmcn.ui.continue"))) : List.copyOf(lines));
        if (options.isEmpty()) {
            page.setInput(new DialogueNoInput((dialogue, ignored) -> dialogue.close()));
        } else {
            var input = new DialogueOptionSetInput();
            input.setOptions(options);
            input.setVertical(false);
            page.setInput(input);
        }
        var speaker = new DialogueSpeaker(new WrappedDialogueText(Component.translatableWithFallback("lmcn.species.calyrex", "Calyrex")),
                new ArtificialDialogueFaceProvider("pokemon", ResourceLocation.fromNamespaceAndPath("cobblemon", "calyrex"), Set.of(), true),
                new DialogueGibber());
        DialogueAction noop = (dialogue, value) -> {};
        return new Dialogue(List.of(page), Dialogue.Companion.getDEFAULT_BACKGROUND(), noop, Map.of("calyrex", speaker), noop);
    }

    private static final class Frame {
        final ServerPlayer player;
        final List<DialogueText> lines = new ArrayList<>();
        List<DialogueOption> options = List.of();

        Frame(ServerPlayer player) { this.player = player; }
    }
}
