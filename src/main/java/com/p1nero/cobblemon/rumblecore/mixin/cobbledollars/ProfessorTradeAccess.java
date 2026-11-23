package com.p1nero.cobblemon.rumblecore.mixin.cobbledollars;

import com.p1nero.cobblemon.rumblecore.prologue.ProfessorEntity;
import com.p1nero.cobblemon.rumblecore.prologue.ProfessorShop;
import com.p1nero.cobblemon.rumblecore.prologue.TrialMerchantEntity;
import com.p1nero.cobblemon.rumblecore.prologue.WanderingMerchantQuest;
import fr.harmex.cobbledollars.common.network.handlers.server.BuyHandler;
import fr.harmex.cobbledollars.common.network.handlers.server.OpenBankHandler;
import fr.harmex.cobbledollars.common.network.handlers.server.OpenShopHandler;
import fr.harmex.cobbledollars.common.network.handlers.server.SellHandler;
import fr.harmex.cobbledollars.common.network.packets.c2s.BuyPacket;
import fr.harmex.cobbledollars.common.network.packets.c2s.OpenBankPacket;
import fr.harmex.cobbledollars.common.network.packets.c2s.OpenShopPacket;
import fr.harmex.cobbledollars.common.network.packets.c2s.SellPacket;
import fr.harmex.cobbledollars.common.world.inventory.BankMenu;
import fr.harmex.cobbledollars.common.world.inventory.ShopMenu;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the same access rules on the native Buy/Sell shortcuts as on the dialogue choices. */
final class ProfessorTradeAccess {
    @Mixin(value = OpenShopHandler.class, remap = false)
    static abstract class OpenShop {
        @Inject(method = "handle(Lfr/harmex/cobbledollars/common/network/packets/c2s/OpenShopPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"), cancellable = true)
        private void rumble$access(OpenShopPacket packet, MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
            var target = player.serverLevel().getEntity(packet.getMerchantUUID());
            if (target instanceof ProfessorEntity doctor && !ProfessorShop.canTrade(player, doctor)
                    || target instanceof TrialMerchantEntity) ci.cancel();
        }
    }

    @Mixin(value = OpenBankHandler.class, remap = false)
    static abstract class OpenBank {
        @Inject(method = "handle(Lfr/harmex/cobbledollars/common/network/packets/c2s/OpenBankPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"), cancellable = true)
        private void rumble$access(OpenBankPacket packet, MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
            var target = player.serverLevel().getEntity(packet.getMerchantUUID());
            if (target instanceof TrialMerchantEntity || target instanceof ProfessorEntity doctor
                    && !ProfessorShop.canTrade(player, doctor)) ci.cancel();
        }
    }

    @Mixin(value = BuyHandler.class, remap = false)
    static abstract class Buy {
        @Inject(method = "handle(Lfr/harmex/cobbledollars/common/network/packets/c2s/BuyPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"), cancellable = true)
        private void rumble$access(BuyPacket packet, MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
            var target = player.serverLevel().getEntity(packet.getMerchantUUID());
            if (target instanceof ProfessorEntity doctor) {
                if (!(player.containerMenu instanceof ShopMenu menu) || menu.getCobbleMerchant() != doctor
                        || !packet.getHasMerchant() || !ProfessorShop.canTrade(player, doctor)) ci.cancel();
            } else if (target instanceof TrialMerchantEntity trader) {
                if (!(player.containerMenu instanceof ShopMenu menu) || menu.getCobbleMerchant() != trader
                        || !packet.getHasMerchant() || !WanderingMerchantQuest.canTrade(player, trader)
                        || !trader.isCurrentShop(player)) ci.cancel();
            } else if (player.containerMenu instanceof ShopMenu menu
                    && (menu.getCobbleMerchant() instanceof ProfessorEntity || menu.getCobbleMerchant() instanceof TrialMerchantEntity)) ci.cancel();
        }
    }

    @Mixin(value = SellHandler.class, remap = false)
    static abstract class Sell {
        @Inject(method = "handle(Lfr/harmex/cobbledollars/common/network/packets/c2s/SellPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"), cancellable = true)
        private void rumble$access(SellPacket packet, MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
            var target = player.serverLevel().getEntity(packet.getMerchantUUID());
            if (target instanceof ProfessorEntity doctor) {
                if (!(player.containerMenu instanceof BankMenu menu) || menu.getCobbleMerchant() != doctor
                        || !packet.getHasMerchant() || !ProfessorShop.canTrade(player, doctor)) ci.cancel();
            } else if (player.containerMenu instanceof BankMenu menu && menu.getCobbleMerchant() instanceof ProfessorEntity) ci.cancel();
        }
    }
}
