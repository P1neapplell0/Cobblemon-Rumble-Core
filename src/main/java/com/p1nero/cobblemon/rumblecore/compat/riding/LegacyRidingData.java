package com.p1nero.cobblemon.rumblecore.compat.riding;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.StringReader;

/** Adapts pre-locator seats before Cobblemon deserializes species and species additions. */
public final class LegacyRidingData {
    private LegacyRidingData() {}

    public static BufferedReader migrate(BufferedReader reader) {
        var json = JsonParser.parseReader(reader);
        if (json.isJsonObject()) migrate(json.getAsJsonObject());
        return new BufferedReader(new StringReader(json.toString()));
    }

    public static int migrate(JsonObject definition) {
        int changed = 0;
        if (definition.get("riding") instanceof JsonObject riding && riding.has("seats")) {
            var seats = riding.getAsJsonArray("seats");
            for (int i = 0; i < seats.size(); i++) {
                var seat = seats.get(i).getAsJsonObject();
                // 1.8 ignores both old offset fields; without a locator the resulting seat has an
                // empty name and cannot follow the model's seat bone. Leave modern seats untouched.
                boolean missing = !seat.has("locator") || seat.get("locator").isJsonNull()
                        || seat.get("locator").getAsString().isBlank();
                if (missing && (seat.has("offset") || seat.has("poseOffsets"))) {
                    seat.addProperty("locator", "seat_" + (i + 1));
                    seat.remove("offset");
                    seat.remove("poseOffsets");
                    changed++;
                }
            }
        }
        // Additions can contain nested form definitions, including Mega and regional forms.
        if (definition.has("forms")) {
            for (var form : definition.getAsJsonArray("forms")) changed += migrate(form.getAsJsonObject());
        }
        return changed;
    }
}
