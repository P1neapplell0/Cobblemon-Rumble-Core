package com.p1nero.cobblemon.rumblecore.prologue;

import com.p1nero.cobblemon.rumblecore.CobblemonRumbleCore;
import com.p1nero.cobblemon.rumblecore.shop.WanderingCatalogue;
import fr.harmex.cobbledollars.common.api.bank.Bank;
import fr.harmex.cobbledollars.common.world.inventory.ShopMenu;
import fr.harmex.cobbledollars.common.world.item.trading.CobbleDollarsShopHolder;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class TrialMerchantEntity extends WanderingTrader implements CobbleDollarsShopHolder {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, CobblemonRumbleCore.MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<TrialMerchantEntity>> TYPE =
            TYPES.register("trial_merchant", () -> EntityType.Builder.of(TrialMerchantEntity::new, MobCategory.MISC)
                    .sized(.6F, 1.95F).clientTrackingRange(8).updateInterval(2).build("cobblemon_rumble_core:trial_merchant"));
    private long shopDay = Long.MIN_VALUE;
    private ShopRef shop;
    private BankRef bank = new BankRef.Inline(new Bank(Map.of()));
    private Set<Player> trading = new HashSet<>();

    public TrialMerchantEntity(EntityType<? extends WanderingTrader> type, Level level) {
        super(type, level);
        setNoAi(true);
        setSilent(true);
        setInvulnerable(true);
        setPersistenceRequired();
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(TYPE.get(), WanderingTrader.createMobAttributes().build()));
    }

    // Old entity saves can restore Silent=false after construction. Never play ambient grunts.
    @Override protected SoundEvent getAmbientSound() { return null; }

    @Override protected Component getTypeName() { return Component.translatable(WanderingMerchantQuest.NAME); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeLeashed() { return false; }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // The shop is opened by the ArcQuest dialogue action.
        return InteractionResult.FAIL;
    }

    @Override public ShopRef getShopRef() {
        var server = level().getServer();
        if (server == null) return shop;
        long day = server.overworld().getGameTime() / 24_000L;
        if (shop == null || shopDay != day) {
            // Do not let an open shop apply yesterday's row index to today's stock.
            for (var player : Set.copyOf(trading)) if (player instanceof ServerPlayer p
                    && p.containerMenu instanceof ShopMenu menu
                    && menu.getCobbleMerchant() == this) p.closeContainer();
            shopDay = day;
            shop = new ShopRef.Inline(WanderingCatalogue.create(RandomSource.create(server.overworld().getSeed() ^ (day * 31 + 0xC1A0BEEF))));
        }
        return shop;
    }

    public boolean isCurrentShop(ServerPlayer player) {
        if (shopDay == player.server.overworld().getGameTime() / 24_000L) return true;
        player.closeContainer();
        return false;
    }

    @Override public void setShopRef(ShopRef value) { shop = value; }
    @Override public BankRef getBankRef() { return bank; }
    @Override public void setBankRef(BankRef value) { bank = value; }
    @Override public UUID getMerchantUUID() { return getUUID(); }
    @Override public void setMerchantUUID(UUID value) { }
    @Override public Set<Player> getTradingPlayers() { return trading; }
    @Override public void setTradingPlayers(Set<Player> players) { trading = players; }
}
