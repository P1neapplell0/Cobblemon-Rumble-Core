package com.p1nero.cobblemon.rumblecore.compat.realtime.localization;

import com.johnn.realtimecobblemon.client.Fonts;
import net.minecraft.network.chat.MutableComponent;

/** Keep RTC's font styling for unmodified labels and user text. */
public final class RealtimeHudText {
    private RealtimeHudText() {}

    public static MutableComponent title(String text) {
        var translated = RealtimeText.translate(text);
        return translated == null ? Fonts.title(text) : Fonts.title(translated);
    }

    public static MutableComponent body(String text) {
        var translated = RealtimeText.translate(text);
        return translated == null ? Fonts.body(text) : Fonts.body(translated);
    }

    public static MutableComponent pixel(String text) {
        var translated = RealtimeText.translate(text);
        return translated == null ? Fonts.pixel(text) : translated.withStyle(style -> style.withFont(Fonts.PIXEL));
    }
}
