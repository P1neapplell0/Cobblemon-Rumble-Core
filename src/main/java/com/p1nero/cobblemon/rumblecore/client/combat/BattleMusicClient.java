package com.p1nero.cobblemon.rumblecore.client.combat;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.BattleMusic;
import com.p1nero.cobblemon.rumblecore.client.menu.RumbleTitleScreen;
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
    private static final byte TITLE = 3;
    private static BattleMusic.State state;
    private static Track current;
    private static int retry;

    private BattleMusicClient() {}

    public static void receive(BattleMusic.State next) { state = next; }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        byte wanted = 0;
        if (mc.level == null && mc.screen instanceof RumbleTitleScreen) wanted = TITLE;
        else if (state != null && (ArenaIntroOverlay.playingFor(state.dimension())
                || mc.level != null && state.dimension().equals(mc.level.dimension().location()))) wanted = state.track();
        var sounds = mc.getSoundManager();
        if (retry > 0) retry--;
        // SoundEngine can refuse a stream (or clear it on a level/device reload) without stopping this object.
        if (current != null && (current.isStopped() || !sounds.isActive(current))) current = null;
        if (current != null && wanted != current.id) {
            current.fading = true;
            return;
        }
        if (current != null) current.fading = false;
        else if (wanted != 0 && retry == 0
                && mc.options.getSoundSourceVolume(SoundSource.MASTER) > 0
                && mc.options.getSoundSourceVolume(SoundSource.MUSIC) > 0) {
            mc.getMusicManager().stopPlaying();
            current = new Track(wanted);
            sounds.play(current);
            if (!sounds.isActive(current)) {
                current = null;
                retry = 10;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void background(SelectMusicEvent event) {
        if (current != null && !current.isStopped()) event.overrideMusic(null);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        state = null;
        retry = 0;
        if (current != null) Minecraft.getInstance().getSoundManager().stop(current);
        current = null;
    }

    private static final class Track extends AbstractTickableSoundInstance {
        private final byte id;
        private final float targetVolume;
        private boolean fading;

        Track(byte id) {
            super(SoundEvent.createVariableRangeEvent(PrologueWorld.id(switch (id) {
                        case TITLE -> "title.theme";
                        case BattleMusic.GYM -> "battle.gym";
                        default -> "battle.standard";
                    })),
                    SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.id = id;
            this.targetVolume = id == TITLE ? .22F : .8F;
            this.volume = .001F;
            this.looping = true;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
        }

        @Override public void tick() {
            volume = fading ? Math.max(0, volume - .035F) : Math.min(targetVolume, volume + .035F);
            if (fading && volume == 0) stop();
        }
    }
}
