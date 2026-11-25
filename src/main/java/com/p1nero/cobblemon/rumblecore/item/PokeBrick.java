package com.p1nero.cobblemon.rumblecore.item;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.combat.RealtimeMoveWindup;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PokeBrick extends Item {
    private static final int MIN_CHARGE_TICKS = 4;
    private static final int FULL_CHARGE_TICKS = 20;
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CobblemonRumbleCore.MOD_ID);
    public static final DeferredItem<PokeBrick> ITEM = ITEMS.registerItem("pokebrick", PokeBrick::new);

    private PokeBrick(Properties properties) {
        super(properties);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        ThrownPokeBrick.register(bus);
        bus.addListener(PokeBrick::addToCreativeTab);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) event.accept(ITEM);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isSpectator() || (player instanceof ServerPlayer server && RealtimeMoveWindup.locked(server))) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingTicks) {
        if (!(level instanceof ServerLevel serverLevel) || !(user instanceof ServerPlayer player)
                || !player.isAlive() || player.isSpectator() || RealtimeMoveWindup.locked(player)) return;
        int charge = getUseDuration(stack, user) - remainingTicks;
        if (charge < MIN_CHARGE_TICKS) return;

        float power = Mth.clamp((float) charge / FULL_CHARGE_TICKS, 0.0F, 1.0F);
        var projectile = new ThrownPokeBrick(serverLevel, player);
        projectile.setItem(stack);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.55F + 1.35F * power, 0.5F);
        if (!serverLevel.addFreshEntity(projectile)) return;
        stack.consume(1, player);
        player.awardStat(Stats.ITEM_USED.get(this));
        serverLevel.playSound(null, player, SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.8F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".description").withStyle(ChatFormatting.GRAY));
    }
}
