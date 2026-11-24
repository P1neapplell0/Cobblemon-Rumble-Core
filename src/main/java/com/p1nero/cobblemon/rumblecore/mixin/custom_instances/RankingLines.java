package com.p1nero.cobblemon.rumblecore.mixin.custom_instances;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.p1nero.misc.custom_instances.client.screen.information.InstanceInformationScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = InstanceInformationScreen.class, remap = false)
abstract class RankingLines {
    @Shadow private int tab;

    @WrapOperation(method = "buildLines", at = @At(value = "INVOKE", target =
            "Lcom/p1nero/misc/custom_instances/client/screen/information/InstanceInformationScreen;append(Lnet/minecraft/network/chat/Component;Lnet/minecraft/world/item/ItemStack;)V"))
    private void rumble$rankingRow(InstanceInformationScreen screen, Component line, ItemStack stack, Operation<Void> original) {
        if (line.getContents() instanceof TranslatableContents text) {
            if (text.getKey().endsWith(".battle_unscored")) return;
            if (tab == 2 && text.getKey().endsWith(".ranking_dps_details"))
                line = Component.translatable("rumble.gym.ranking_dps_details", text.getArgs()).withStyle(line.getStyle());
        }
        original.call(screen, line, stack);
    }
}
