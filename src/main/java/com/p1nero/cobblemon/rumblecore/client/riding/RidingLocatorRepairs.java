package com.p1nero.cobblemon.rumblecore.client.riding;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/** Restores only missing seat locators from old-core metadata, after checking the active rig. */
public final class RidingLocatorRepairs {
    private static final JsonObject REPAIRS = load();
    private static final JsonArray ZERO = zero();

    private RidingLocatorRepairs() {}

    private static JsonObject load() {
        var repairs = load("seat_locators.json");
        load("seat_locators_extra.json").entrySet().forEach(entry -> {
            if (!repairs.has(entry.getKey())) repairs.add(entry.getKey(), new JsonArray());
            repairs.getAsJsonArray(entry.getKey()).addAll(entry.getValue().getAsJsonArray());
        });
        return repairs;
    }

    private static JsonObject load(String name) {
        try (var stream = RidingLocatorRepairs.class.getResourceAsStream(
                "/assets/cobblemon_rumble_core/riding/" + name)) {
            if (stream == null) throw new IOException("Missing bundled seat locator metadata");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load riding locator repairs", e);
        }
    }

    public static Resource repair(ResourceLocation identifier, Resource resource) {
        var patches = REPAIRS.getAsJsonArray(identifier.toString());
        if (patches == null) return resource;
        try (var reader = resource.openAsReader()) {
            var model = JsonParser.parseReader(reader).getAsJsonObject();
            int added = repair(model, patches);
            if (added == 0) return resource;
            byte[] bytes = model.toString().getBytes(StandardCharsets.UTF_8);
            LogUtils.getLogger().debug("Restored {} riding locators in {} from {}", added, identifier, resource.sourcePackId());
            // The resource keeps its real source and metadata; only this model factory sees the patch.
            return new Resource(resource.source(), () -> new ByteArrayInputStream(bytes), resource::metadata);
        } catch (IOException | RuntimeException e) {
            LogUtils.getLogger().warn("Cannot repair riding locators in {}", identifier, e);
            return resource;
        }
    }

    public static int repair(JsonObject model, JsonArray patches) {
        var geometries = model.getAsJsonArray("minecraft:geometry");
        if (geometries == null || geometries.size() != 1) return 0;
        var bones = geometries.get(0).getAsJsonObject().getAsJsonArray("bones");
        Map<String, JsonObject> byName = new HashMap<>();
        var locators = new HashSet<String>();
        for (var element : bones) {
            var bone = element.getAsJsonObject();
            byName.put(bone.get("name").getAsString(), bone);
            if (bone.has("locators")) locators.addAll(bone.getAsJsonObject("locators").keySet());
        }
        int added = 0;
        for (var element : patches) {
            var patch = element.getAsJsonObject();
            var candidate = patch.getAsJsonObject("bone");
            var wanted = candidate.getAsJsonObject("locators");
            if (locators.containsAll(wanted.keySet())) continue;

            // Existing locators win. A changed ancestor pivot, rotation or parent indicates a new rig;
            // importing coordinates from the old model would put the rider in the wrong place again.
            boolean compatible = true;
            for (var ancestor : patch.getAsJsonObject("ancestors").entrySet()) {
                if (!sameTransform(byName.get(ancestor.getKey()), ancestor.getValue().getAsJsonObject())) {
                    compatible = false;
                    break;
                }
            }
            if (!compatible) continue;
            String name = candidate.get("name").getAsString();
            var destination = byName.get(name);
            if (destination != null && !sameTransform(destination, candidate)) continue;
            if (destination == null) {
                destination = candidate.deepCopy();
                destination.remove("locators");
                bones.add(destination);
                byName.put(name, destination);
            }
            if (!destination.has("locators")) destination.add("locators", new JsonObject());
            for (var locator : wanted.entrySet()) {
                if (locators.add(locator.getKey())) {
                    destination.getAsJsonObject("locators").add(locator.getKey(), locator.getValue().deepCopy());
                    added++;
                }
            }
        }
        return added;
    }

    private static boolean sameTransform(JsonObject actual, JsonObject expected) {
        if (actual == null || !Objects.equals(actual.get("parent"), expected.get("parent"))) return false;
        for (String key : new String[] {"pivot", "rotation"}) {
            if (!Objects.equals(actual.has(key) ? actual.get(key) : ZERO,
                    expected.has(key) ? expected.get(key) : ZERO)) return false;
        }
        return true;
    }

    private static JsonArray zero() {
        var result = new JsonArray();
        for (int i = 0; i < 3; i++) result.add(0);
        return result;
    }
}
