package com.p1nero.cobblemon.rumblecore.compat.legendarymonuments.localization;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/** Adapts Legendary Monuments' hard-coded English at its call sites, before serialization. */
public final class MonumentText {
    private record Template(String key, String fallback, String prefix, String suffix, Pattern pattern) {}
    private record Catalog(Map<String, String> keys, List<Template> templates) {
        private static Catalog load() {
            var input = Objects.requireNonNull(MonumentText.class.getResourceAsStream("/assets/lmcn/lang/en_us.json"));
            Map<String, String> keys = new HashMap<>();
            List<Template> templates = new ArrayList<>();
            try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Map<String, String> entries = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
                entries.forEach((key, english) -> {
                    if (!key.startsWith("lmcn.") || key.startsWith("lmcn.dialogue.") || key.startsWith("lmcn.ui.")) return;
                    var parts = english.split("%s", -1);
                    if (parts.length == 1) {
                        keys.putIfAbsent(english, key);
                    } else {
                        String regex = String.join("(.+?)", java.util.Arrays.stream(parts).map(Pattern::quote).toList());
                        templates.add(new Template(key, english, parts[0], parts[parts.length - 1],
                                Pattern.compile(regex, Pattern.DOTALL)));
                    }
                });
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            templates.sort(Comparator.comparingInt((Template t) -> t.fallback.length()).reversed());
            return new Catalog(Map.copyOf(keys), List.copyOf(templates));
        }
    }

    private static final class Lazy {
        static final Catalog CATALOG = Catalog.load();
    }

    private MonumentText() {}

    public static MutableComponent literal(String text) {
        var translated = translationOrNull(text);
        return translated != null ? translated : Component.literal(text);
    }

    static MutableComponent translationOrNull(String text) {
        if (text == null || text.isEmpty()) return null;
        var catalog = Lazy.CATALOG;
        String key = catalog.keys.get(text);
        if (key != null) return Component.translatableWithFallback(key, text);
        // One scan of the small template list, no matching/copying for unrelated Minecraft text.
        for (var template : catalog.templates) {
            if (!text.startsWith(template.prefix) || !text.endsWith(template.suffix)) continue;
            var match = template.pattern.matcher(text);
            if (!match.matches()) continue;
            var args = new Object[match.groupCount()];
            for (int i = 0; i < args.length; i++) args[i] = argument(match.group(i + 1));
            return Component.translatableWithFallback(template.key, template.fallback, args);
        }
        return null;
    }

    private static Object argument(Object value) {
        if (value instanceof String text) {
            String key = Lazy.CATALOG.keys.get(text);
            if (key != null && (key.startsWith("lmcn.species.") || key.startsWith("lmcn.term."))) {
                return Component.translatableWithFallback(key, text);
            }
        }
        return value;
    }

    public static MutableComponent translatable(String key, Object... args) {
        return Component.translatable(key, localizeArguments(args));
    }

    public static MutableComponent translatableWithFallback(String key, String fallback, Object... args) {
        return Component.translatableWithFallback(key, fallback, localizeArguments(args));
    }

    private static Object[] localizeArguments(Object[] args) {
        Object[] result = args;
        for (int i = 0; i < args.length; i++) {
            Object value = argument(args[i]);
            if (value == args[i]) continue;
            if (result == args) result = args.clone();
            result[i] = value;
        }
        return result;
    }
}
