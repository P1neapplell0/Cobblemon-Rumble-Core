package com.p1nero.cobblemon.rumblecore.client.combat;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.BattleMusic;
import com.p1nero.cobblemon.rumblecore.prologue.PrologueWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class BattleMusicClient {
    private static BattleMusic.State state;
    private static Track current;

    private BattleMusicClient() {}

    public static void receive(BattleMusic.State next) { state = next; }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        byte wanted = state != null && mc.level != null && state.dimension().equals(mc.level.dimension().location()) ? state.track() : 0;
        if (current != null && current.isStopped()) current = null;
        if (current != null && wanted != current.id) {
            current.fading = true;
            return;
        }
        if (current != null) current.fading = false;
        else if (wanted != 0) {
            current = new Track(wanted);
            mc.getSoundManager().play(current);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void background(SelectMusicEvent event) {
        if (current != null && !current.isStopped()) event.overrideMusic(null);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        state = null;
        if (current != null) Minecraft.getInstance().getSoundManager().stop(current);
        current = null;
    }

    private static final class Track extends AbstractTickableSoundInstance {
        private final byte id;
        private boolean fading;

        Track(byte id) {
            super(SoundEvent.createVariableRangeEvent(PrologueWorld.id(id == 2 ? "battle.trainer" : "battle.wild")),
                    SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.id = id;
            this.volume = .001F;
            this.looping = true;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override public void tick() {
            volume = fading ? Math.max(0, volume - .035F) : Math.min(.8F, volume + .035F);
            if (fading && volume == 0) stop();
        }
    }
}
