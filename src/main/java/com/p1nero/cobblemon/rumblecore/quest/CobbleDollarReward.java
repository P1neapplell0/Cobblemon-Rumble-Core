package com.p1nero.cobblemon.rumblecore.quest;

import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import java.math.BigInteger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.arcadia.arc_quest.quest.api.IReward;

public record CobbleDollarReward(int amount) implements IReward {
    @Override public void grant(ServerPlayer player) {
        PlayerExtensionKt.earnCobbleDollars(player, BigInteger.valueOf(amount), false);
    }

    @Override public String describe() {
        return Component.translatable("rumble.reward.coins", amount).getString();
    }
}
