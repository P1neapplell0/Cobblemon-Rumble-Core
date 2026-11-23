package com.p1nero.cobblemon.rumblecore.client.combat;

import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

/** Local one-shots stay synchronized with the overlay, even while a dimension is loading. */
final class ArenaCinematicAudio {
    private ArenaCinematicAudio() {}

    static void play(String key, float volume) {
        var sound = SoundEvent.createVariableRangeEvent(PrologueWorld.id(key));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1f, volume));
    }
}
