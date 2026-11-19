package com.p1nero.cobblemon.rumblecore.integration.rct;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.gitlab.srcmc.rctapi.api.trainer.TrainerNPC;
import com.gitlab.srcmc.rctmod.ModCommon;
import com.gitlab.srcmc.rctmod.api.RCTMod;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaTheme;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog;
import com.p1nero.cobblemon.rumblecore.mixin.rct.TrainerOpponent;
import com.p1nero.cobblemon.rumblecore.quest.CobbleDollarReward;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;

public final class RoadTrainerArena {
    private static final TicketType<UUID> TRAINER_TICKET = TicketType.create("rumble_road_trainer", UUID::compareTo);

    private RoadTrainerArena() {}

    public static boolean challenge(ServerPlayer player, TrainerMob mob) {
        if (mob.level() != player.level() || !mob.isAlive() || !mob.canBattleAgainst(player)) return false;
        var registered = ModCommon.RCT.getTrainerRegistry().getById(mob.getTrainerId(), TrainerNPC.class);
        if (registered == null) return false;
        List<Pokemon> roster = Arrays.stream(registered.getTeam()).filter(Objects::nonNull).toList();
        if (roster.isEmpty()) return false;
        var source = player.serverLevel();
        var chunk = mob.chunkPosition();
        var ticket = UUID.randomUUID();
        var theme = ArenaTheme.biome(source, mob.blockPosition());
        var actor = new GymCatalog.Actor("road_trainer", mob.blockPosition(),
                () -> roster.stream().map(pokemon -> pokemon.clone(true, player.registryAccess())).toList(), mob.getDisplayName());
        var trainerManager = RCTMod.getInstance().getTrainerManager();
        var challenge = new ArenaMatch.StoryChallenge() {
            private boolean released;

            @Override public List<GymCatalog.Actor> actors() { return List.of(actor); }
            @Override public ArenaTheme arena(ServerPlayer player) { return theme; }
            @Override public boolean canSkip() { return false; }
            @Override public boolean eligible(ServerPlayer player) {
                return mob.isAlive() && (mob.getOpponent() == null || mob.getOpponent() == player);
            }

            @Override public void result(ServerPlayer player, boolean won) {
                var battle = trainerManager.getBattle(player.getUUID()).orElse(null);
                if (battle == null || mob.getOpponent() != player || !trainerManager.removeBattle(player.getUUID())) return;
                try {
                    battle.distributeRewards(won);
                    if (won) {
                        int amount = Math.min(1200, 100 + 5 * roster.stream().mapToInt(Pokemon::getLevel).sum());
                        new CobbleDollarReward(amount).grant(player);
                        player.sendSystemMessage(Component.translatable("rumble.gym.reward.coins", amount));
                    }
                } finally {
                    closed();
                }
            }

            @Override public void closed() {
                if (released) return;
                released = true;
                try {
                    if (mob.getOpponent() == player) {
                        trainerManager.removeBattle(player.getUUID());
                        ((TrainerOpponent) mob).rumble$setOpponent(null);
                    }
                } finally {
                    source.getChunkSource().removeRegionTicket(TRAINER_TICKET, chunk, 2, ticket);
                }
            }
        };
        source.getChunkSource().addRegionTicket(TRAINER_TICKET, chunk, 2, ticket);
        boolean started = false;
        try {
            started = ArenaMatch.challengeStory(player, challenge);
            return started;
        } finally {
            if (!started) challenge.closed();
        }
    }
}
