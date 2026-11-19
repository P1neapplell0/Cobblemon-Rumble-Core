package com.p1nero.cobblemon.rumblecore.integration.cobblebash;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.nore.cobblebash.Config;
import com.nore.cobblebash.command.GymCommand;
import com.nore.cobblebash.dimension.CobbleBashDimensions;
import com.nore.cobblebash.elitefour.EliteFourMember;
import com.nore.cobblebash.gym.GymTrainerUnit;
import com.nore.cobblebash.instance.GymInstanceManager;
import com.nore.cobblebash.integration.RctApiProbe;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaMatch;
import com.p1nero.cobblemon.rumblecore.combat.arena.ArenaTheme;
import com.p1nero.cobblemon.rumblecore.gym.GymCatalog;
import com.p1nero.cobblemon.rumblecore.quest.CobbleDollarReward;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SimulatorArena {
    private SimulatorArena() {}

    public static boolean challenge(ServerPlayer player, String gym, int slot, String trainerId) {
        if (!CobbleBashDimensions.GYM_VOID.equals(player.level().dimension())) return false;
        var instance = GymInstanceManager.getActive(player.getUUID());
        var unit = GymTrainerUnit.fromTrainerIdPart(trainerId);
        if (instance == null || instance.getSlotId() != slot || unit == null) return false;
        var registered = RctApiProbe.getGymTrainer(gym, slot, trainerId);
        if (registered == null || registered.getEntity() == null || !registered.getEntity().isAlive()) return false;
        List<Pokemon> roster = Arrays.stream(registered.getTeam()).filter(Objects::nonNull).toList();
        if (roster.isEmpty()) return false;
        var npc = registered.getEntity();
        int stage = instance.getTrainerStage();
        boolean champion = gym.equals("elite4_champion");
        var member = EliteFourMember.fromTrainerGymType(gym);
        boolean league = champion || member != null;
        var actor = new GymCatalog.Actor("simulator_trainer", npc.blockPosition(),
                () -> roster.stream().map(pokemon -> pokemon.clone(true, player.registryAccess())).toList(), registered.getName().getComponent());
        return ArenaMatch.challengeStory(player, new ArenaMatch.StoryChallenge() {
            @Override public List<GymCatalog.Actor> actors() { return List.of(actor); }
            @Override public ArenaTheme arena(ServerPlayer player) { return ArenaTheme.gym(gym); }
            @Override public boolean canSkip() { return false; }
            @Override public boolean eligible(ServerPlayer player) {
                if (GymInstanceManager.getActive(player.getUUID()) != instance || instance.getTrainerStage() != stage) return false;
                if (!league) return instance.getGymType().equals(gym) && stage == unit.getRequiredStage();
                if (!instance.getGymType().equals("elite4") || unit != GymTrainerUnit.BOSS) return false;
                return champion ? instance.isEliteFourChampionUnlocked() : member.getId().equals(instance.getActiveEliteFourMember());
            }

            @Override public void result(ServerPlayer player, boolean won) {
                if (!eligible(player) || !CobbleBashDimensions.GYM_VOID.equals(player.level().dimension())) return;
                if (!won) {
                    GymCommand.clearActiveGym(player, true);
                    return;
                }
                GymCommand.handleTrainerVictory(player, gym, slot, unit);
                // CobbleBash already pays for standard repeat clears.
                if (instance.isRepeatClear() && !league) return;
                int amount = (unit == GymTrainerUnit.BOSS ? Config.COBBLE_DOLLARS_REPEAT_BOSS_REWARD
                        : Config.COBBLE_DOLLARS_REPEAT_TRAINER_REWARD).get();
                if (amount <= 0) return;
                new CobbleDollarReward(amount).grant(player);
                player.sendSystemMessage(Component.translatable("rumble.gym.reward.coins", amount));
            }
        });
    }
}
