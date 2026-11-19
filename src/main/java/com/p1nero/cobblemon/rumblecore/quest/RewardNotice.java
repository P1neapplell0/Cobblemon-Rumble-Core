package com.p1nero.cobblemon.rumblecore.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.arcadia.arc_quest.dialogue.api.DialogueText;

/** Reward lines use the same localized stacks that the server grants. */
public final class RewardNotice {
    private RewardNotice() {}

    public static ItemStack stack(String id, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElseThrow(), count);
    }

    public static Component component(ItemStack... stacks) {
        return Component.translatable("rumble.reward.items", names(stacks));
    }

    private static Component names(ItemStack... stacks) {
        Component names = Component.empty();
        for (int i = 0; i < stacks.length; i++) {
            var stack = stacks[i];
            var label = Component.translatable("rumble.reward.stack", stack.getHoverName(), stack.getCount());
            names = i == 0 ? label : Component.translatable("rumble.reward.join", names, label);
        }
        return names;
    }

    public static DialogueText items(ItemStack... stacks) {
        return DialogueText.translatable("rumble.reward.items", DialogueText.DialogueArg.of(ctx -> names(stacks)));
    }

    public static void grant(ServerPlayer player, ItemStack... stacks) {
        for (var reward : stacks) {
            var stack = reward.copy();
            if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false);
        }
    }
}
