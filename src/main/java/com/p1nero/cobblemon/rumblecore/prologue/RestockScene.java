package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.arcadia.arc_quest.api.event.dialogue.DialogueNodeStartedEvent;
import org.arcadia.arc_quest.dialogue.runtime.DialogueSessionManager;

/** Only the audience and effects are per-player; the three actors are saved in the hall. */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class RestockScene {
    private record Audience(ProfessorEntity doctor, UUID session) {}

    private static final Map<ServerPlayer, Audience> AUDIENCE = new WeakHashMap<>();

    private RestockScene() {}

    @SubscribeEvent
    public static void onBeat(DialogueNodeStartedEvent event) {
        if (!event.getDialogueId().startsWith(ProfessorRestock.TREE + "/")) return;
        var player = event.getPlayer();
        switch (event.getNodeId()) {
            case "jessie_stop", "duo_retreat" -> {
                var session = DialogueSessionManager.INSTANCE.getSession(player);
                if (session != null && event.getNpc() instanceof ProfessorEntity doctor && PrologueStory.at(player, doctor)
                        && AUDIENCE.putIfAbsent(player, new Audience(doctor, session.getSessionId())) == null
                        && "jessie_stop".equals(event.getNodeId())) smoke(player, false);
            }
            case "thanks" -> clear(player, true);
        }
    }

    static boolean visible(ServerPlayer player) {
        var audience = AUDIENCE.get(player);
        if (audience == null || !PrologueStory.at(player, audience.doctor())) return false;
        var session = DialogueSessionManager.INSTANCE.getSession(player);
        return session != null && audience.session().equals(session.getSessionId());
    }

    static void clear(ServerPlayer player, boolean retreat) {
        var audience = AUDIENCE.remove(player);
        if (retreat && audience != null && player.level() == audience.doctor().level()) smoke(player, true);
    }

    private static void smoke(ServerPlayer player, boolean retreat) {
        var level = player.serverLevel();
        for (var actor : FixedStoryActors.rocketActors(level)) level.sendParticles(player, ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                actor.getX(), actor.getY() + 0.6, actor.getZ(), 13, 0.3, 0.3, 0.3, 0.023);
        player.playNotifySound(SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 0.55F, retreat ? 0.83F : 1.15F);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !visible(player)) clear(player, false);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player, false);
    }
}
