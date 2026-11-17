package com.p1nero.cobblemon.rumblecore.compat.realtime.localization;

import com.cobblemon.mod.common.api.moves.Moves;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Adapts only text produced by RTC. Server messages carry translation keys, not resolved Chinese. */
public final class RealtimeText {
    private static final String PREFIX = "realtimecobblemon.rumble.";
    private static final String STAGE = PREFIX + "stage.";
    private static final String STAGE_CHANGE = STAGE + "change";
    private static final Pattern MOVE_SEPARATORS = Pattern.compile("[^a-z0-9]+");

    private record Entry(String key, String fallback) {
        MutableComponent component(Object... args) {
            return Component.translatableWithFallback(key, fallback, args);
        }
    }

    private record Template(Entry entry, String first, String last, Pattern pattern) {}

    private record Catalog(Map<String, Entry> exact, List<Template> templates) {
        private static Catalog load() {
            var exact = new HashMap<String, Entry>();
            var templates = new ArrayList<Template>();
            try (var reader = new InputStreamReader(Objects.requireNonNull(
                    RealtimeText.class.getResourceAsStream("/realtime-localization/en_us.json")), StandardCharsets.UTF_8)) {
                Map<String, String> english = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
                // A stat or result may contain spaces (e.g. "Sp. Atk rose sharply!").
                var stageResults = english.entrySet().stream()
                        .filter(pair -> pair.getKey().startsWith(STAGE) && !pair.getKey().equals(STAGE_CHANGE))
                        .map(pair -> Pattern.quote(pair.getValue()))
                        .collect(Collectors.joining("|", "(", ")"));
                english.forEach((key, text) -> {
                    var entry = new Entry(key, text);
                    var parts = text.split("%s", -1);
                    if (parts.length == 1) {
                        exact.put(text, entry);
                    } else {
                        String pattern = key.equals(STAGE_CHANGE)
                                ? Pattern.quote(parts[0]) + "(.*?)" + Pattern.quote(parts[1]) + stageResults + Pattern.quote(parts[2])
                                : String.join("(.*?)", java.util.Arrays.stream(parts).map(Pattern::quote).toList());
                        templates.add(new Template(entry, parts[0], parts[parts.length - 1], Pattern.compile(pattern)));
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            templates.sort(Comparator.comparingInt((Template template) -> template.entry.fallback.length()).reversed());
            return new Catalog(Map.copyOf(exact), List.copyOf(templates));
        }
    }

    private static final class Lazy {
        static final Catalog CATALOG = Catalog.load();
    }

    private RealtimeText() {}

    public static MutableComponent literal(String text) {
        var translated = translate(text);
        return translated != null ? translated : Component.literal(text);
    }

    static MutableComponent translate(String text) {
        if (text == null || text.isBlank() || Character.isDigit(text.charAt(0))) return null;
        var catalog = Lazy.CATALOG;
        var entry = catalog.exact.get(text);
        if (entry != null) return entry.component();
        for (var template : catalog.templates) {
            if (!text.startsWith(template.first) || !text.endsWith(template.last)) continue;
            var matcher = template.pattern.matcher(text);
            if (!matcher.matches()) continue;
            var args = new Object[matcher.groupCount()];
            for (int i = 0; i < args.length; i++) {
                String arg = matcher.group(i + 1);
                var word = catalog.exact.get(arg);
                args[i] = word != null ? word.component() : isMoveArgument(template.entry.key, i) ? moveName(arg) : arg;
            }
            return template.entry.component(args);
        }
        return null;
    }

    private static boolean isMoveArgument(String key, int index) {
        return index == 0 && (key.equals(PREFIX + "battle.used") || key.equals(PREFIX + "battle.used_here")
                || key.equals(PREFIX + "field.here"));
    }

    /** RTC sometimes supplies only the English move name (including move tags from the network). */
    public static MutableComponent moveName(String name) {
        String id = MOVE_SEPARATORS.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("");
        var move = Moves.getByName(id);
        String key = "cobblemon.move." + (move != null ? move.getName() : id);
        // Language files are available before a client receives Cobblemon's move registry.
        return move != null || Language.getInstance().has(key)
                ? Component.translatableWithFallback(key, name) : Component.literal(name);
    }

    public static MutableComponent popup(String text) {
        var translated = translate(text);
        if (translated != null) return translated;
        return text.isEmpty() || !Character.isLetter(text.charAt(0)) ? Component.literal(text) : moveName(text);
    }

    public static MutableComponent append(MutableComponent parent, String text) {
        var translated = translate(text);
        return translated != null ? parent.append(translated) : parent.append(text);
    }
}
