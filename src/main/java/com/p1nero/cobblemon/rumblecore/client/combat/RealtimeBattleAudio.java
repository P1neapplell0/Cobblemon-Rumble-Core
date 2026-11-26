package com.p1nero.cobblemon.rumblecore.client.combat;

import com.johnn.realtimecobblemon.client.ClientSmash;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeSoundDistance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID, value = Dist.CLIENT)
public final class RealtimeBattleAudio {
    private RealtimeBattleAudio() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void play(PlaySoundEvent event) {
        if (!(event.getSound() instanceof SimpleSoundInstance sound) || sound.isRelative()
                || sound.isLooping() || sound.getAttenuation() != SoundInstance.Attenuation.LINEAR
                || !"realtimecobblemon".equals(sound.getLocation().getNamespace())) return;
        var mc = Minecraft.getInstance();
        var id = ClientSmash.controlledEntity();
        var pokemon = mc.level == null || id == null ? null : mc.level.getEntity(id);
        if (pokemon != null && !pokemon.isRemoved()) event.setSound(new GroundSound(sound, pokemon));
    }

    /** RTC positional effects are one-shots. Keep the original seeded sample, volume slider and pitch. */
    private static final class GroundSound extends SimpleSoundInstance implements TickableSoundInstance {
        private final SimpleSoundInstance original;
        private final Entity listener;
        private float gain;

        GroundSound(SimpleSoundInstance original, Entity listener) {
            super(original.getLocation(), original.getSource(), 1, 1, SoundInstance.createUnseededRandom(),
                    false, 0, Attenuation.NONE, 0, 0, 0, true);
            this.original = original;
            this.listener = listener;
        }

        @Override public WeighedSoundEvents resolve(SoundManager manager) {
            var resolved = original.resolve(manager);
            sound = original.getSound();
            if (sound != null) tick();
            return resolved;
        }

        @Override public float getPitch() { return original.getPitch(); }
        @Override public float getVolume() { return original.getVolume() * gain; }
        @Override public boolean isStopped() {
            return listener.isRemoved() || listener.level() != Minecraft.getInstance().level;
        }

        @Override public void tick() {
            var box = listener.getBoundingBox();
            double sx = original.getX(), sz = original.getZ();
            double distance = Math.sqrt(RealtimeSoundDistance.squared(box, sx, sz));
            double range = Math.max(original.getVolume(), 1F) * sound.getAttenuationDistance();
            gain = range > 0 ? (float) Math.max(0, 1 - distance / range) : 0;
            // OpenAL relative coordinates preserve panning without letting camera position affect gain.
            var center = box.getCenter();
            var direction = new Vector3f((float) (sx - center.x), 0, (float) (sz - center.z));
            if (distance > 0 && direction.lengthSquared() > 0) {
                var rotation = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
                direction.normalize().rotate(new Quaternionf(rotation).conjugate());
                direction.mul((float) Math.min(1, distance / 2));
            } else {
                direction.zero();
            }
            x = direction.x;
            y = direction.y;
            z = direction.z;
        }
    }
}
