package com.p1nero.cobblemon.rumblecore.combat;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

/**
 * 训练师只能指挥宝可梦战斗，不能自己对宝可梦造成伤害。
 * 使用服务端 actionbar 提示，不在聊天栏留下重复消息。
 */
@EventBusSubscriber(modid = CobblemonRumbleCore.MOD_ID)
public final class PlayerPokemonAttackGate {
    private PlayerPokemonAttackGate() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onAttack(AttackEntityEvent event) {
        // 客户端仍发送攻击请求，由服务端统一拒绝并提示，避免两端重复提示。
        if (event.getTarget() instanceof PokemonEntity
                && event.getEntity() instanceof ServerPlayer player) {
            // 在近战流程开始前取消，同时阻止击退、附魔着火等攻击副作用。
            event.setCanceled(true);
            notifyTrainer(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof PokemonEntity)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        // RTC 招式以宝可梦实体作为直接伤害来源；玩家代理归属不能误判为训练师攻击。
        // 不以 Smash.active 作为豁免，否则操控期间玩家的箭和其他武器也会被放行。
        if (event.getSource().getDirectEntity() instanceof PokemonEntity) return;
        // 兜底拦截玩家发射的箭、三叉戟及其他归属于玩家的伤害（包括横扫）。
        event.setCanceled(true);
        notifyTrainer(player);
    }

    private static void notifyTrainer(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("rumble.combat.trainer_attack_blocked"), true);
    }
}
