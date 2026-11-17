package com.p1nero.cobblemon.rumblecore.wallet;

import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import java.math.BigInteger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class KnockoutWallet {
    private KnockoutWallet() {}

    public static void credit(ServerPlayer player, int maxHp) {
        BigInteger amount = BigInteger.valueOf(Math.max(1, maxHp));
        // Use the wallet's API so its balance is persisted and synchronized to the existing UI.
        BigInteger before = PlayerExtensionKt.getCobbleDollars(player);
        if (!PlayerExtensionKt.earnCobbleDollars(player, amount, false)) return;
        BigInteger credited = PlayerExtensionKt.getCobbleDollars(player).subtract(before);
        if (credited.signum() > 0) player.sendSystemMessage(Component.translatable("rumble.combat.wallet", credited.toString()));
    }
}
