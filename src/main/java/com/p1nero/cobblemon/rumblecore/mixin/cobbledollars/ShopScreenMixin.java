package com.p1nero.cobblemon.rumblecore.mixin.cobbledollars;

import com.cobblemon.mod.common.api.net.NetworkPacket;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fr.harmex.cobbledollars.common.client.gui.screen.ShopScreen;
import fr.harmex.cobbledollars.common.network.packets.c2s.BuyPacket;
import fr.harmex.cobbledollars.common.api.shop.Offer;
import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;

/** Keeps the amount field usable after a full-inventory purchase is rejected. */
@Mixin(value = ShopScreen.class, remap = false)
public abstract class ShopScreenMixin {
    @ModifyArgs(method = "buy", at = @At(value = "INVOKE",
            target = "Lfr/harmex/cobbledollars/common/network/packets/c2s/BuyPacket;<init>(Lfr/harmex/cobbledollars/common/api/shop/Offer;IIIIZLjava/util/UUID;)V"))
    private void rumble$clampPacketAmount(Args args) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;

        Offer offer = args.get(0);
        int ingredientIndex = args.get(3);
        if (ingredientIndex < 0 || ingredientIndex >= offer.getItems().size()) return;
        ItemStack item = offer.getItems().get(ingredientIndex);
        int requested = Math.max(0, args.get(4));
        int obtainable = PlayerExtensionKt.getMaxAmountObtainable(player, item);
        int affordable = offer.getPrice().signum() > 0 ? PlayerExtensionKt.getMaxAmountPurchasable(player, offer.getPrice()) : requested;
        int amount = Math.min(requested, Math.min(obtainable, affordable));
        if (offer.getStock() >= 0) amount = Math.min(amount, offer.getStock());
        args.set(4, amount);
    }

    @WrapOperation(method = "buy", at = @At(value = "INVOKE",
            target = "Lfr/harmex/cobbledollars/common/network/CobbleDollarsNetwork;sendToServer(Lcom/cobblemon/mod/common/api/net/NetworkPacket;)V"))
    private void rumble$skipEmptyPurchase(NetworkPacket<?> packet, Operation<Void> original) {
        if (!(packet instanceof BuyPacket buy) || buy.getAmount() > 0) original.call(packet);
    }
}
